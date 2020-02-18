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
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;

@Component
@Singleton
public class AcceptActivityHandler extends AbstractActivityHandler implements ActivityHandler<Accept>
{
    @Override
    public void handleInboxRequest(ActivityRequest<Accept> activityRequest) throws IOException
    {
        this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
            "Only client to server is currently implemented.");
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
            OrderedCollection<AbstractActor> followingActorFollowing =
                this.activityPubObjectReferenceResolver.resolveReference(followingActor.getFollowing());
            acceptingActorFollowers.addItem(followingActor);
            followingActorFollowing.addItem(acceptingActor);

            this.activityPubStorage.storeEntity(acceptingActorFollowers);
            this.activityPubStorage.storeEntity(followingActorFollowing);
            if (this.actorHandler.isLocalActor(followingActor)) {
                this.notifier
                    .notify(accept, Collections.singleton(this.actorHandler.getXWikiUserReference(followingActor)));
            }
            this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, accept);
        } else {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                "Only follow activities can be accepted in the current implementation.");
        }
    }
}
