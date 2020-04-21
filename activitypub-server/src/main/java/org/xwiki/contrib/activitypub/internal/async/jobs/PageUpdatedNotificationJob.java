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
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;

import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;

/**
 * Handles asynchronous ActivityPub tasks on page update events.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Named(PageUpdatedNotificationJob.ASYNC_REQUEST_TYPE)
public class PageUpdatedNotificationJob extends AbstractPageNotificationJob
{
    /**
     * The name of the job.
     */
    public static final String ASYNC_REQUEST_TYPE = "activitypub-update-page";

    @Inject
    private ActivityHandler<Update> updateActivityHandler;

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
        Update updateActivity = this.getActivity(author);
        ActivityRequest<Update> activityRequest = new ActivityRequest<>(updateActivity.getActor().getObject(),
            updateActivity);
        this.updateActivityHandler.handleOutboxRequest(activityRequest);
    }

    private Update getActivity(AbstractActor author)
        throws MalformedURLException, URISyntaxException, ActivityPubException
    {
        String view = this.request.getViewURL();
        String title = this.request.getDocumentTitle();
        Date creationDate = this.request.getCreationDate();
        XDOM content = this.request.getContent();
        DocumentReference documentReference = this.request.getDocumentReference();

        URI documentUrl = this.urlHandler.getAbsoluteURI(new URI(view));

        List<ActivityPubObjectReference<AbstractActor>> attributedTo = this.emmiters(author);

        String docIdSolr = this.stringEntityReferenceSerializer.serialize(documentReference);

        List<Document> documents = this.storage.query(Document.class, String.format("filter(xwikiReference:%s)",
            escapeQueryChars(docIdSolr)), 1);
        String contentRendered = this.htmlRenderer.render(content, documentReference);
        Document document;
        if (documents.isEmpty()) {
            // the document was created before the installation of this extension.
            document = new Document()
                .setName(title)
                .setAttributedTo(attributedTo)
                .setPublished(creationDate)
                .setContent(contentRendered)
                .setUrl(Collections.singletonList(documentUrl))
                .setXwikiReference(docIdSolr);
        } else {
            // if the document was already existing in storage, we update it
            document = documents.get(0)
                .setContent(contentRendered)
                .setAttributedTo(attributedTo);
        }

        // Make sure it's stored so it can be resolved later.
        this.storage.storeEntity(document);

        return new Update()
            .setActor(author)
            .setObject(document)
            .setName(String.format("Update of document [%s]", title))
            .setTo(Collections.singletonList(new ProxyActor(author.getFollowers().getLink())))
            .setPublished(creationDate);
    }

    @Override public String getType()
    {
        return ASYNC_REQUEST_TYPE;
    }
}
