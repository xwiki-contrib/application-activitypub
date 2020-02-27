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

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Listen for {@link DocumentCreatedEvent} and notify the followers of the creator with ActivityPub protocol.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("ActivityPubDocumentCreatedEventListener")
public class DocumentCreatedEventListener extends AbstractEventListener
{
    private static final List<Event> EVENTS = Arrays.asList(new DocumentCreatedEvent());

    private static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER);

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @Inject
    private ActivityPubStorage storage;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Logger logger;

    @Inject
    private ActivityHandler<Create> createActivityHandler;

    /**
     * Default constructor.
     */
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

        try {
            AbstractActor author = this.actorHandler.getActor(document.getAuthorReference());
            OrderedCollection<AbstractActor> followers =
                this.objectReferenceResolver.resolveReference(author.getFollowers());

            // ensure the page can be viewed with guest user to not disclose private stuff in a notif
            boolean guestAccess =
                this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, document.getDocumentReference());
            if (guestAccess && !followers.isEmpty()) {
                Create createActivity = getActivity(author, document, context);
                ActivityRequest<Create> activityRequest = new ActivityRequest<>(createActivity.getActor().getObject(),
                    createActivity);
                this.createActivityHandler.handleOutboxRequest(activityRequest);
            }
        } catch (URISyntaxException | ActivityPubException | IOException e) {
            this.logger.error("Error while trying to handle DocumentCreatedEvent for document [{}]",
                document.getDocumentReference(), e);
        }
    }

    private Create getActivity(AbstractActor author, XWikiDocument xWikiDocument, XWikiContext context)
        throws URISyntaxException, ActivityPubException
    {
        URI documentUrl = new URI(xWikiDocument.getURL("view", context));

        Document document = new Document()
            .setName(xWikiDocument.getTitle())
            .setAttributedTo(
                Collections.singletonList(new ActivityPubObjectReference<AbstractActor>().setObject(author)))
            .setPublished(xWikiDocument.getCreationDate())
            // We cannot put it as a document id, since we need to be able to resolve it with an activitypub answer.
            .setUrl(Collections.singletonList(documentUrl));

        // Make sure it's stored so it can be resolved later.
        this.storage.storeEntity(document);

        return new Create()
            .setActor(author)
            .setObject(document)
            .setName(String.format("Creation of document [%s]", xWikiDocument.getTitle()))
            .setPublished(xWikiDocument.getCreationDate());
    }
}
