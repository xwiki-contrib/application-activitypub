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
package org.xwiki.contrib.activitypub.internal.async;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.job.AbstractJob;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Handles asynchronous ActivityPub tasks on page creation events.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Named(PageCreatedNotificationJob.ASYNC_REQUEST_TYPE)
public class PageCreatedNotificationJob extends AbstractJob<PageCreatedRequest, PageCreatedStatus>
{
    /**
     * The name of the job.
     */
    public static final String ASYNC_REQUEST_TYPE = "activitypub-create-page";

    private static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER);

    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @Inject
    private ActivityHandler<Create> createActivityHandler;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private ActivityPubStorage storage;

    @Inject
    private DefaultURLHandler urlHandler;

    @Inject
    private HTMLRenderer htmlRenderer;

    @Override
    protected void runInternal()
    {
        PageCreatedRequest request = this.getRequest();
        try {
            UserReference userReference =
                this.xWikiUserBridge.resolveDocumentReference(request.getAuthorReference());
            AbstractActor author = this.actorHandler.getActor(userReference);
            OrderedCollection<AbstractActor> followers =
                this.objectReferenceResolver.resolveReference(author.getFollowers());

            // ensure the page can be viewed with guest user to not disclose private stuff in a notif
            boolean guestAccess =
                this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, request.getDocumentReference());
            if (guestAccess && !followers.isEmpty()) {
                Create createActivity = this.getActivity(author);
                ActivityRequest<Create> activityRequest = new ActivityRequest<>(createActivity.getActor().getObject(),
                    createActivity);
                this.createActivityHandler.handleOutboxRequest(activityRequest);
            }
        } catch (URISyntaxException | ActivityPubException | IOException e) {
            // FIXME: we have a special handling of errors coming from user reference resolution,
            // since we have regular stacktraces related to Scheduler listener and AP resolution issue with
            // CurrentUserReference. This should be removed after fixing XAP-28.
            String errorMessage = "Error while trying to handle DocumentCreatedEvent for document [{}]";
            if (e instanceof ActivityPubException && e.getMessage().contains("Cannot find any user with reference")) {
                this.logger.debug(errorMessage, request.getDocumentReference(), e);
            } else {
                this.logger.error(errorMessage, request.getDocumentReference(), e);
            }
        }
    }

    private Create getActivity(AbstractActor author)
        throws MalformedURLException, URISyntaxException, ActivityPubException
    {

        String view = this.request.getViewURL();
        String title = this.request.getDocumentTitle();
        Date creationDate = this.request.getCreationDate();
        XDOM content = this.request.getContent();

        URI documentUrl = this.urlHandler.getAbsoluteURI(new URI(view));

        Document document = new Document()
                                .setName(title)
                                .setAttributedTo(
                                    Collections.singletonList(
                                        new ActivityPubObjectReference<AbstractActor>().setObject(author)))
                                .setPublished(creationDate)
                                .setContent(this.htmlRenderer.render(content))
                                // We cannot put it as a document id, since we need to be able to resolve it 
                                // with an activitypub answer.
                                .setUrl(Collections.singletonList(documentUrl));

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
