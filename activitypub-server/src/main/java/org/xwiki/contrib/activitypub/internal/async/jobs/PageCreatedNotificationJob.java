/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.activitypub.internal.async.jobs;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.rendering.block.XDOM;

/**
 * Handles asynchronous ActivityPub tasks on page creation events.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Named(PageCreatedNotificationJob.ASYNC_REQUEST_TYPE)
public class PageCreatedNotificationJob extends AbstractPageNotificationJob
{
    /**
     * The name of the job.
     */
    public static final String ASYNC_REQUEST_TYPE = "activitypub-create-page";

    @Inject
    private ActivityHandler<Create> createActivityHandler;

    @Inject
    private ActivityPubStorage storage;

    @Inject
    private DefaultURLHandler urlHandler;

    @Inject
    private HTMLRenderer htmlRenderer;

    @Override
    protected void proceed(AbstractActor author)
        throws IOException, ActivityPubException, URISyntaxException
    {
        Create createActivity = this.getActivity(author);
        ActivityRequest<Create> activityRequest = new ActivityRequest<>(createActivity.getActor().getObject(),
            createActivity);
        this.createActivityHandler.handleOutboxRequest(activityRequest);
    }

    private Create getActivity(AbstractActor author)
        throws MalformedURLException, URISyntaxException, ActivityPubException
    {
        String view = this.request.getViewURL();
        String title = this.request.getDocumentTitle();
        Date creationDate = this.request.getCreationDate();
        XDOM content = this.request.getContent();

        URI documentUrl = this.urlHandler.getAbsoluteURI(new URI(view));

        List<ActivityPubObjectReference<AbstractActor>> attributedTo = this.emitters(author);
        Document document = new Document()
            .setName(title)
            .setAttributedTo(attributedTo)
            .setPublished(creationDate)
            .setContent(this.htmlRenderer.render(content, this.request.getDocumentReference()))
            // We cannot put it as a document id, since we need to be able to resolve it 
            // with an activitypub answer.
            .setUrl(Collections.singletonList(documentUrl))
            .setXwikiReference(this.stringEntityReferenceSerializer.serialize(this.request.getDocumentReference()));

        // Make sure it's stored so it can be resolved later.
        this.storage.storeEntity(document);

        return new Create()
            .setActor(author)
            .setObject(document)
            .setName(String.format("Creation of document [%s]", title))
            .setTo(Collections.singletonList(new ProxyActor(author.getFollowers().getLink())))
            .setPublished(creationDate);
    }

    @Override
    public String getType()
    {
        return ASYNC_REQUEST_TYPE;
    }
}
