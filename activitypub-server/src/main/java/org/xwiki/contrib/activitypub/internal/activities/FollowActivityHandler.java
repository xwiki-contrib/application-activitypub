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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.activities.Follow;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.internal.ActorHandler;

@Component
@Singleton
public class FollowActivityHandler extends AbstractActivityHandler implements ActivityHandler<Follow>
{
    @Inject
    private ActorHandler actorHandler;

    @Override
    public void handleInboxRequest(ActivityRequest<Follow> activityRequest) throws IOException
    {
        this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
            "Only client to server is currently implemented.");
    }

    /**
     * Answer a 202 answer "Request accepted"
     * @param activityRequest
     */
    @Override
    public void handleOutboxRequest(ActivityRequest<Follow> activityRequest) throws IOException
    {
        Follow follow = activityRequest.getActivity();
        if (follow.getId() == null) {
            this.activityPubStorage.storeEntity(follow);
        }
        Object followedObject = follow.getObject().getObject(this.activityPubJsonParser);
        if (followedObject instanceof Actor) {
            Actor followedActor = (Actor) followedObject;
            Inbox actorInbox = this.actorHandler.getActorInbox(followedActor);
            actorInbox.addPendingFollow(follow);
            actorInbox.addActivity(follow);

            this.answer(activityRequest.getResponse(), HttpServletResponse.SC_ACCEPTED, follow);
        } else {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Only following actors is implemented.");
        }

    }
}
