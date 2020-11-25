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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Like;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LikeActivityHandler}.
 *
 * @version $Id$
 */
@ComponentTest
public class LikeActivityHandlerTest extends AbstractHandlerTest
{
    @InjectMockComponents
    private LikeActivityHandler likeActivityHandler;

    @BeforeEach
    public void setup() throws Exception
    {
        this.initMock();
    }

    @Test
    void handleInboxRequestLikesCollectionExisting() throws Exception
    {
        Person person = mock(Person.class);
        ActivityPubObject likedObject = mock(ActivityPubObject.class);
        when(likedObject.getReference()).thenReturn(new ActivityPubObjectReference<>().setObject(likedObject));

        Like like = new Like().setActor(person).setObject(likedObject);

        when(this.activityPubObjectReferenceResolver
            .resolveReference(new ActivityPubObjectReference<>().setObject(likedObject)))
            .thenReturn(likedObject);
        ActivityPubObjectReference<OrderedCollection<Like>> likesRef = mock(ActivityPubObjectReference.class);
        when(likedObject.getLikes()).thenReturn(likesRef);
        OrderedCollection<Like> likes = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(likesRef)).thenReturn(likes);

        this.likeActivityHandler.handleInboxRequest(
            new ActivityRequest<>(person, like, this.servletRequest, this.servletResponse));
        verifyResponse(like);
        verify(likes).addItem(like);
        verify(this.activityPubStorage).storeEntity(likes);
    }

    @Test
    void handleInboxRequestLikesCollectionNotExisting() throws Exception
    {
        Person person = mock(Person.class);
        ActivityPubObject likedObject = mock(ActivityPubObject.class);
        when(likedObject.getReference()).thenReturn(new ActivityPubObjectReference<>().setObject(likedObject));

        Like like = new Like().setActor(person).setObject(likedObject);

        when(this.activityPubObjectReferenceResolver
            .resolveReference(new ActivityPubObjectReference<>().setObject(likedObject)))
            .thenReturn(likedObject);

        when(this.activityPubStorage.storeEntity(any(OrderedCollection.class))).then(invocation -> {
            assertTrue(((OrderedCollection) invocation.getArgument(0))
                .contains(new ActivityPubObjectReference().setObject(like)));
            return null;
        });

        this.likeActivityHandler.handleInboxRequest(
            new ActivityRequest<>(person, like, this.servletRequest, this.servletResponse));
        verifyResponse(like);
        verify(this.activityPubStorage).storeEntity(any(OrderedCollection.class));
        verify(likedObject).setLikes(any());
        verify(this.activityPubStorage).storeEntity(likedObject);
    }

    @Test
    void handleOutboxRequestLikedCollectionExisting() throws Exception
    {
        Person person = mock(Person.class);
        Note likedObject = mock(Note.class);
        Like like = new Like().setActor(person).setObject(likedObject);

        ActivityPubObjectReference<OrderedCollection<ActivityPubObject>> likedRef =
            mock(ActivityPubObjectReference.class);
        when(person.getLiked()).thenReturn(likedRef);
        OrderedCollection<ActivityPubObject> liked = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(likedRef)).thenReturn(liked);
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Note>)like.getObject())).thenReturn(likedObject);

        this.likeActivityHandler.handleOutboxRequest(
            new ActivityRequest<>(person, like, this.servletRequest, this.servletResponse));
        verifyResponse(like);
        verify(liked).addItem(likedObject);
        verify(this.activityPubStorage).storeEntity(liked);
    }

    @Test
    void handleOutboxRequestLikedCollectionNotExisting() throws Exception
    {
        Person person = mock(Person.class);
        Note likedObject = mock(Note.class);
        when(likedObject.getReference()).thenReturn(new ActivityPubObjectReference<>().setObject(likedObject));
        Like like = new Like().setActor(person).setObject(likedObject);
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Note>)like.getObject())).thenReturn(likedObject);

        when(this.activityPubStorage.storeEntity(any(OrderedCollection.class))).then(invocation -> {
            assertTrue(((OrderedCollection) invocation.getArgument(0))
                .contains(like.getObject()));
            return null;
        });

        this.likeActivityHandler.handleOutboxRequest(
            new ActivityRequest<>(person, like, this.servletRequest, this.servletResponse));
        verifyResponse(like);
        verify(this.activityPubStorage).storeEntity(any(OrderedCollection.class));
        verify(person).setLiked(any());
        verify(this.activityPubStorage).storeEntity(person);
    }
}
