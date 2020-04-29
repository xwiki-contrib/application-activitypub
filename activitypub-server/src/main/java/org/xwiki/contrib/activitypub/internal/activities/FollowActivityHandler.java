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
package org.xwiki.contrib.activitypub.internal.activities;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Reject;

/**
 * Specific handler for {@link Follow} activities.
 *
 * @version $Id$
 */
@Component
@Singleton
public class FollowActivityHandler extends AbstractActivityHandler<Follow>
{
    @Inject
    private ActivityHandler<Accept> acceptActivityHandler;

    @Inject
    private ActivityHandler<Reject> rejectActivityHandler;

    private void handleFollow(Follow follow, HttpServletResponse servletResponse)
        throws ActivityPubException, IOException
    {
        this.activityPubStorage.storeEntity(follow);
        ActivityPubObject followedObject = this.activityPubObjectReferenceResolver.resolveReference(follow.getObject());
        
        ActivityPubObjectReference<AbstractActor> actor = follow.getActor();
        if (actor.getLink() == null) {
            throw new ActivityPubException(
                String.format("Malformed Follow object [%s]. The actor is not a valid url", follow));
        }
        AbstractActor followingActor = this.activityPubObjectReferenceResolver.resolveReference(actor);

        if (!(followedObject instanceof AbstractActor)) {
            this.answerError(servletResponse, HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Only following actors is implemented.");
        } else {
            AbstractActor followedActor = (AbstractActor) followedObject;
            switch (this.activityPubConfiguration.getFollowPolicy()) {
                // In case of ASK, we notify the user who receive the follow to perform an action on it
                // and we answer with the same follow action.
                case ASK:
                    Inbox actorInbox = this.getInbox(followedActor);
                    actorInbox.addPendingFollow(follow);
                    this.activityPubStorage.storeEntity(actorInbox);
                    this.notifier.notify(follow, Collections.singleton(followedActor));
                    this.answer(servletResponse, HttpServletResponse.SC_OK, follow);
                    break;

                // In case of Accept, we create an Accept activity on behalf of the user who receives the follow
                // and we directly handle this accept to answer.
                case ACCEPT:
                    Accept accept = new Accept()
                        .setActor(followedActor)
                        .setObject(follow)
                        .setTo(Collections.singletonList(followingActor.getProxyActor()));
                    this.activityPubStorage.storeEntity(accept);
                    this.notifier.notify(accept, Collections.singleton(followedActor));
                    ActivityRequest<Accept> acceptActivityRequest = new ActivityRequest<>(followedActor, accept);
                    this.acceptActivityHandler.handleOutboxRequest(acceptActivityRequest);
                    this.answer(servletResponse, HttpServletResponse.SC_OK, accept);
                    break;

                // In case of Reject, we create a Reject activity on behalf of the user who receives the follow
                // and we directly handle the reject to answer.
                default:
                case REJECT:
                    Reject reject = new Reject()
                        .setActor(followedActor)
                        .setObject(follow)
                        .setTo(Collections.singletonList(followingActor.getProxyActor()));

                    this.activityPubStorage.storeEntity(reject);
                    ActivityRequest<Reject> rejectActivityRequest = new ActivityRequest<>(followedActor, reject);
                    this.notifier.notify(reject, Collections.singleton(followedActor));
                    this.rejectActivityHandler.handleOutboxRequest(rejectActivityRequest);
                    this.answer(servletResponse, HttpServletResponse.SC_OK, reject);
            }
        }
    }

    @Override
    public void handleInboxRequest(ActivityRequest<Follow> activityRequest) throws IOException, ActivityPubException
    {
        Follow follow = activityRequest.getActivity();
        if (follow.getId() == null) {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_BAD_REQUEST,
                "The ID of the activity must not be null.");
        } else {
            this.handleFollow(follow, activityRequest.getResponse());
        }
    }

    /**
     * @param activityRequest
     */
    @Override
    public void handleOutboxRequest(ActivityRequest<Follow> activityRequest) throws IOException, ActivityPubException
    {
        Follow follow = activityRequest.getActivity();
        this.handleFollow(follow, activityRequest.getResponse());
    }
}
