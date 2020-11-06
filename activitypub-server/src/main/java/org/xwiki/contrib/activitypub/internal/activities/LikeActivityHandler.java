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

import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Like;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;

/**
 * Activity handler for {@link Like}.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Singleton
public class LikeActivityHandler extends AbstractActivityHandler<Like>
{
    @Override
    public void handleInboxRequest(ActivityRequest<Like> activityRequest)
        throws IOException, ActivityPubException
    {
        Like activity = activityRequest.getActivity();
        ActivityPubObject likedObject =
            this.activityPubObjectReferenceResolver.resolveReference(activity.getObject());
        ActivityPubObjectReference<OrderedCollection<Like>> likesReference = likedObject.getLikes();
        OrderedCollection<Like> likes;
        if (likesReference != null) {
            likes = this.activityPubObjectReferenceResolver.resolveReference(likesReference);
        } else {
            likes = new OrderedCollection<>();
        }
        likes.addItem(activity);
        this.activityPubStorage.storeEntity(likes);

        // If it's the first like then we just created the ordered collection and we need to store its reference.
        if (likesReference == null) {
            likedObject.setLikes(likes.getReference());
            this.activityPubStorage.storeEntity(likedObject);
        }

        this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, activity);
    }

    @Override
    public void handleOutboxRequest(ActivityRequest<Like> activityRequest)
        throws IOException, ActivityPubException
    {
        Like activity = activityRequest.getActivity();
        AbstractActor actor = activityRequest.getActor();
        ActivityPubObjectReference<OrderedCollection<ActivityPubObject>> likedReference = actor.getLiked();
        OrderedCollection<ActivityPubObject> liked;
        if (likedReference != null) {
            liked = this.activityPubObjectReferenceResolver.resolveReference(likedReference);
        } else {
            liked = new OrderedCollection<>();
        }
        liked.addItem(this.activityPubObjectReferenceResolver.resolveReference(activity.getObject()));
        this.activityPubStorage.storeEntity(liked);

        // if it's the first like then we just created the ordered collection and we need to store its reference.
        if (likedReference == null) {
            actor.setLiked(liked.getReference());
            this.activityPubStorage.storeEntity(actor);
        }

        this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, activity);
    }
}
