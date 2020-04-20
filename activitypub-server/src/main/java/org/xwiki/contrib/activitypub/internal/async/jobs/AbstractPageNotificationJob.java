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
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.internal.async.PageChangedRequest;
import org.xwiki.contrib.activitypub.internal.async.PageChangedStatus;
import org.xwiki.job.AbstractJob;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Please document me.
 *
 * @version $Id$
 * @since 1.2
 */
public abstract class AbstractPageNotificationJob extends AbstractJob<PageChangedRequest, PageChangedStatus>
{
    private static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER);

    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private ActorHandler actorHandler;

    @Override
    protected void runInternal()
    {
        PageChangedRequest request = this.getRequest();
        try {
            UserReference userReference =
                this.xWikiUserBridge.resolveDocumentReference(request.getAuthorReference());
            AbstractActor author = this.actorHandler.getActor(userReference);
            OrderedCollection<AbstractActor> followers =
                this.objectReferenceResolver.resolveReference(author.getFollowers());

            // ensure the page can be viewed with guest user to not disclose private stuff in a notif
            boolean guestAccess = this.authorizationManager
                .hasAccess(Right.VIEW, GUEST_USER, request.getDocumentReference());
            if (guestAccess && !followers.isEmpty()) {
                this.proceed(author);
            }
        } catch (URISyntaxException | ActivityPubException | IOException e) {
            // FIXME: we have a special handling of errors coming from user reference resolution,
            // since we have regular stacktraces related to Scheduler listener and AP resolution issue with
            // CurrentUserReference. This should be removed after fixing XAP-28.
            String errorMessage = "Error while trying to handle notifications for document [{}]";
            if (e instanceof ActivityPubException && e.getMessage().contains("Cannot find any user with reference")) {
                this.logger.debug(errorMessage, request.getDocumentReference(), e);
            } else {
                this.logger.error(errorMessage, request.getDocumentReference(), e);
            }
        }
    }

    protected abstract void proceed(AbstractActor author) throws URISyntaxException, IOException, ActivityPubException;
}
