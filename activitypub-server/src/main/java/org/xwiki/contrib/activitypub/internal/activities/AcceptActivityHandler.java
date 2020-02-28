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

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;

/**
 * Specific handler for {@link Accept} activities.
 *
 * @version $Id$
 */
@Component
@Singleton
public class AcceptActivityHandler extends AbstractActivityHandler<Accept>
{
    private static final String ONLY_FOLLOW_IMPLEMENTED =
        "Only follow activities can be accepted in the current implementation.";

    @Override
    public void handleInboxRequest(ActivityRequest<Accept> activityRequest) throws IOException, ActivityPubException
    {
        Accept accept = activityRequest.getActivity();
        if (accept.getId() == null) {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_BAD_REQUEST,
                "The ID of the activity must not be null.");
        } else {
            AbstractActor acceptingActor = this.activityPubObjectReferenceResolver.resolveReference(accept.getActor());
            ActivityPubObject object = this.activityPubObjectReferenceResolver.resolveReference(accept.getObject());

            if (object instanceof Follow) {
                Follow follow = (Follow) object;
                AbstractActor followingActor =
                    this.activityPubObjectReferenceResolver.resolveReference(follow.getActor());
                OrderedCollection<AbstractActor> followingActorfollowings =
                    this.activityPubObjectReferenceResolver.resolveReference(followingActor.getFollowing());
                followingActorfollowings.addItem(acceptingActor);
                this.activityPubStorage.storeEntity(followingActorfollowings);

                this.notifier.notify(accept,
                    Collections.singleton(this.actorHandler.getXWikiUserReference(followingActor)));
                this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, accept);
            } else {
                this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                    ONLY_FOLLOW_IMPLEMENTED);
            }
        }
    }

    @Override
    public void handleOutboxRequest(ActivityRequest<Accept> activityRequest) throws IOException, ActivityPubException
    {
        Accept accept = activityRequest.getActivity();
        if (accept.getId() == null) {
            this.activityPubStorage.storeEntity(accept);
        }
        AbstractActor acceptingActor = this.activityPubObjectReferenceResolver.resolveReference(accept.getActor());
        ActivityPubObject object = this.activityPubObjectReferenceResolver.resolveReference(accept.getObject());

        if (object instanceof Follow) {
            Follow follow = (Follow) object;
            AbstractActor followingActor = this.activityPubObjectReferenceResolver.resolveReference(follow.getActor());
            OrderedCollection<AbstractActor> acceptingActorFollowers =
                this.activityPubObjectReferenceResolver.resolveReference(acceptingActor.getFollowers());
            acceptingActorFollowers.addItem(followingActor);
            this.activityPubStorage.storeEntity(acceptingActorFollowers);

            this.notifier.notify(accept,
                Collections.singleton(this.actorHandler.getXWikiUserReference(acceptingActor)));
            HttpMethod postMethod = this.activityPubClient.postInbox(followingActor, accept);
            try {
                this.activityPubClient.checkAnswer(postMethod);
            } catch (ActivityPubException e) {
                // FIXME: in that case is the final answer still a 200 OK?
                this.logger.error("Error while posting the accept to the following user.", e);
            } finally {
                postMethod.releaseConnection();
            }

            this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, accept);
        } else {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                ONLY_FOLLOW_IMPLEMENTED);
        }
    }
}
