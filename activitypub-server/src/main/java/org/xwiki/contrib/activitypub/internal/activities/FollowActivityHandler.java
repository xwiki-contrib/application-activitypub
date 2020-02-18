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

import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;

@Component
@Singleton
public class FollowActivityHandler extends AbstractActivityHandler implements ActivityHandler<Follow>
{
    @Override
    public void handleInboxRequest(ActivityRequest<Follow> activityRequest) throws IOException, ActivityPubException
    {
        Follow follow = activityRequest.getActivity();
        if (follow.getId() == null) {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_BAD_REQUEST,
                "The ID of the activity must not be null.");
        }
        ActivityPubObject followedObject = this.activityPubObjectReferenceResolver.resolveReference(follow.getObject());
        if (followedObject instanceof AbstractActor) {
            AbstractActor followedActor = (AbstractActor) followedObject;
            AbstractActor followingActor = activityRequest.getActor();
            this.handleFollow(follow, followingActor, followedActor, activityRequest.getResponse());
        } else {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Only following actors is implemented.");
        }
    }

    private void handleFollow(Follow follow, AbstractActor followingActor, AbstractActor followedActor,
        HttpServletResponse servletResponse)
        throws ActivityPubException, IOException
    {
        switch (this.activityPubConfiguration.getFollowPolicy()) {
            case ASK:
                Inbox actorInbox = this.actorHandler.getInbox(followedActor);
                actorInbox.addPendingFollow(follow);
                this.answer(servletResponse, HttpServletResponse.SC_OK, follow);
                break;

            case ACCEPT:
                follow.setAccepted(true);
                this.activityPubStorage.storeEntity(follow);
                OrderedCollection<AbstractActor> followers =
                    this.activityPubObjectReferenceResolver.resolveReference(followedActor.getFollowers());
                followers.addItem(followingActor);
                this.activityPubStorage.storeEntity(followers);
                OrderedCollection<AbstractActor> followings =
                    this.activityPubObjectReferenceResolver.resolveReference(followingActor.getFollowing());
                followings.addItem(followedActor);
                this.notifier.notify(follow, Collections.singleton(this.actorHandler
                    .getXWikiUserReference(followedActor)));
                this.answer(servletResponse, HttpServletResponse.SC_OK, follow);
                break;

            case REJECT:
                follow.setRejected(true);
                this.activityPubStorage.storeEntity(follow);
                this.answerError(servletResponse, HttpServletResponse.SC_UNAUTHORIZED,
                    "Follow request are not accepted on this server.");
        }
    }

    /**
     * @param activityRequest
     */
    @Override
    public void handleOutboxRequest(ActivityRequest<Follow> activityRequest) throws IOException, ActivityPubException
    {
        Follow follow = activityRequest.getActivity();
        if (follow.getId() == null) {
            this.activityPubStorage.storeEntity(follow);
        }
        ActivityPubObject followedObject = this.activityPubObjectReferenceResolver.resolveReference(follow.getObject());
        if (followedObject instanceof AbstractActor) {
            AbstractActor followedActor = (AbstractActor) followedObject;
            AbstractActor followingActor = activityRequest.getActor();
            this.handleFollow(follow, followingActor, followedActor, activityRequest.getResponse());
        } else {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Only following actors is implemented.");
        }

    }
}
