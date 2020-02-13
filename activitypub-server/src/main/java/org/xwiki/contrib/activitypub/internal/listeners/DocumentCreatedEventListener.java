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
package org.xwiki.contrib.activitypub.internal.listeners;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.internal.ActorHandler;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
@Singleton
@Named("ActivityPubDocumentCreatedEventListener")
public class DocumentCreatedEventListener extends AbstractEventListener
{
    private static final List<Event> EVENTS = Arrays.asList(new DocumentCreatedEvent());

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private Logger logger;

    @Inject
    private ActivityHandler<Create> createActivityHandler;

    @Inject
    private Container container;

    public DocumentCreatedEventListener()
    {
        super("ActivityPubDocumentCreatedEventListener", EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentCreatedEvent documentCreatedEvent = (DocumentCreatedEvent) event;
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        try {
            Create createActivity = getActivity(document, context);
            ActivityRequest<Create> activityRequest = new ActivityRequest<>(createActivity.getActor().getObject(),
                createActivity, request);
            this.createActivityHandler.handleOutboxRequest(activityRequest);
        } catch (URISyntaxException | ActivityPubException | IOException e) {
            this.logger.error("Error while trying to handle DocumentCreatedEvent for document [{}]",
                document.getDocumentReference(), e);
        }
    }

    private Create getActivity(XWikiDocument xWikiDocument, XWikiContext context)
        throws URISyntaxException, ActivityPubException
    {
        URI documentId = new URI(xWikiDocument.getURL("view", context));
        Actor author = this.actorHandler.getActor(xWikiDocument.getAuthorReference());
        Document document = new Document()
            .setId(documentId)
            .setName(xWikiDocument.getTitle())
            .setAttributedTo(Collections.singletonList(new ActivityPubObjectReference<Actor>().setObject(author)))
            .setPublished(xWikiDocument.getCreationDate());

        return new Create()
            .setActor(new ActivityPubObjectReference<Actor>().setObject(author))
            .setObject(new ActivityPubObjectReference<>().setObject(document))
            .setName(String.format("Creation of document [%s]", xWikiDocument.getTitle()))
            .setPublished(xWikiDocument.getCreationDate());
    }
}
