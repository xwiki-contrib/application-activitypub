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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link AcceptActivityHandler}.
 *
 * @version $Id$
 */
@ComponentTest
public class AcceptActivityHandlerTest extends AbstractHandlerTest
{
    @InjectMockComponents
    private AcceptActivityHandler handler;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeEach
    public void setup() throws Exception
    {
        this.initMock();
    }

    @Test
    void handleInboxIDNull() throws Exception
    {
        this.handler.handleInboxRequest(new ActivityRequest<>(null, new Accept()));
        assertEquals("The ID of the activity must not be null.", logCapture.getMessage(0));

        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, new Accept(), this.servletRequest, this.servletResponse));
        this.verifyResponse(400, "The ID of the activity must not be null.");
    }

    @Test
    void handleInboxNotFollowAccept() throws Exception
    {
        Accept accept = new Accept().setObject(new Document()).setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Document>) accept.getObject())).thenReturn(new Document());
        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, accept, this.servletRequest, this.servletResponse));
        this.verifyResponse(501, "Only follow activities can be accepted in the current implementation.");
    }

    @Test
    void handleInbox() throws Exception
    {
        Person followedPerson = new Person()
            .setPreferredUsername("Followed");

        OrderedCollection<AbstractActor> following = new OrderedCollection<>().setName("following");
        UserReference followingRef = mock(UserReference.class);
        Person followingPerson = new Person()
            .setPreferredUsername("Following")
            .setFollowing(following.getReference());

        Follow follow = new Follow()
            .setActor(followingPerson.getReference())
            .setObject(followedPerson.getReference());

        Accept accept = new Accept()
            .setActor(followedPerson.getReference())
            .setObject(follow.getReference())
            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver.resolveReference(accept.getActor())).thenReturn(followedPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Follow>) accept.getObject())).thenReturn(follow);
        when(this.activityPubObjectReferenceResolver.resolveReference(follow.getActor())).thenReturn(followingPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(followingPerson.getFollowing()))
            .thenReturn(following);
        when(this.actorHandler.getXWikiUserReference(followingPerson)).thenReturn(followingRef);

        this.handler.handleInboxRequest(
            new ActivityRequest<>(followingPerson, accept, this.servletRequest, this.servletResponse));
        verifyResponse(accept);
        assertEquals(Collections.singletonList(followedPerson.getReference()), following.getOrderedItems());
        verify(this.activityPubStorage).storeEntity(following);
        verify(this.notifier).notify(accept, Collections.singleton(followingRef));
    }

    @Test
    void handleInboxDuplicate() throws Exception
    {
        OrderedCollection<AbstractActor> followers = new OrderedCollection<>().setName("followers");
        UserReference followedRef = mock(UserReference.class);
        Person followedPerson = new Person()
                                    .setPreferredUsername("Followed")
                                    .setFollowers(followers.getReference());

        Person followingPerson = new Person()
                                     .setPreferredUsername("Following");

        Follow follow = new Follow()
                            .setActor(followingPerson.getReference())
                            .setObject(followedPerson.getReference());

        Accept accept = new Accept()
                            .setActor(followedPerson.getReference())
                            .setObject(follow.getReference());
        when(this.activityPubObjectReferenceResolver.resolveReference(accept.getActor())).thenReturn(followedPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Follow>) accept.getObject())).thenReturn(follow);
        when(this.activityPubObjectReferenceResolver.resolveReference(follow.getActor())).thenReturn(followingPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(followedPerson.getFollowers()))
            .thenReturn(followers);

        OrderedCollection orderedCollection = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(followingPerson.getFollowing())).thenReturn(orderedCollection);
        ActivityPubObjectReference<AbstractActor> abstractActorActivityPubObjectReference =
            new ActivityPubObjectReference<>();
        when(orderedCollection.getOrderedItems()).thenReturn(Arrays.asList(
            abstractActorActivityPubObjectReference));
        when(this.activityPubObjectReferenceResolver.resolveReference(abstractActorActivityPubObjectReference))
            .thenReturn(followingPerson);
        this.handler
            .handleInboxRequest(new ActivityRequest<Accept>(followedPerson, accept.setId(URI.create("http://id"))));
        verify(orderedCollection, times(0)).addItem(followedPerson);
    }

    @Test
    void handleOutbox() throws Exception
    {
        OrderedCollection<AbstractActor> followers = new OrderedCollection<>().setName("followers");
        UserReference followedRef = mock(UserReference.class);
        Person followedPerson = new Person()
            .setPreferredUsername("Followed")
            .setFollowers(followers.getReference());

        Person followingPerson = new Person()
            .setPreferredUsername("Following");

        Follow follow = new Follow()
            .setActor(followingPerson.getReference())
            .setObject(followedPerson.getReference());

        Accept accept = new Accept()
            .setActor(followedPerson.getReference())
            .setObject(follow.getReference());
        when(this.activityPubObjectReferenceResolver.resolveReference(accept.getActor())).thenReturn(followedPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Follow>) accept.getObject())).thenReturn(follow);
        when(this.activityPubObjectReferenceResolver.resolveReference(follow.getActor())).thenReturn(followingPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(followedPerson.getFollowers()))
            .thenReturn(followers);
        when(this.actorHandler.getXWikiUserReference(followedPerson)).thenReturn(followedRef);

        this.handler.handleOutboxRequest(
            new ActivityRequest<>(followedPerson, accept, this.servletRequest, this.servletResponse));
        verifyResponse(accept);
        assertEquals(Collections.singletonList(followingPerson.getReference()), followers.getOrderedItems());
        verify(this.activityPubStorage).storeEntity(followers);
        verify(this.notifier, never()).notify(eq(accept), any(Set.class));
        verify(this.activityPubClient).checkAnswer(any());
        verify(this.activityPubClient).postInbox(followingPerson, accept);
        verify(this.postMethod).releaseConnection();
    }
}
