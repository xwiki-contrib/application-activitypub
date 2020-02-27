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
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link FollowActivityHandler}.
 *
 * @version $Id$
 */
@ComponentTest
public class FollowActivityHandlerTest extends AbstractHandlerTest
{
    @InjectMockComponents
    private FollowActivityHandler handler;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeEach
    public void setup() throws IOException
    {
        this.initMock();
    }

    @Test
    public void handleInboxNoID() throws Exception
    {
        this.handler.handleInboxRequest(new ActivityRequest<>(null, new Follow()));
        assertEquals("The ID of the activity must not be null.", logCapture.getMessage(0));

        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, new Follow(), this.servletRequest, this.servletResponse));
        this.verifyResponse(400, "The ID of the activity must not be null.");
    }

    @Test
    public void handleInboxFollowNoActor() throws Exception
    {
        Follow follow = new Follow().setObject(new Document()).setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Document>)follow.getObject())).thenReturn(new Document());
        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        this.verifyResponse(501, "Only following actors is implemented.");
    }

    @Test
    public void handleInboxAsk() throws Exception
    {
        Person followingActor = new Person().setPreferredUsername("Following");
        Person followedActor = new Person().setPreferredUsername("Followed");
        DocumentReference followedUserRef = new DocumentReference("xwiki", "XWiki", "Followed");
        when(this.actorHandler.getXWikiUserReference(followedActor)).thenReturn(followedUserRef);
        Follow follow = new Follow()
            .setObject(followedActor)
            .setActor(followingActor)
            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Person>)follow.getObject())).thenReturn(followedActor);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(follow.getActor())).thenReturn(followingActor);
        Inbox followedActorInbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getInbox()))
            .thenReturn(followedActorInbox);
        when(this.activityPubConfiguration.getFollowPolicy()).thenReturn(ActivityPubConfiguration.FollowPolicy.ASK);

        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        verifyResponse(follow);
        verify(this.activityPubStorage).storeEntity(followedActorInbox);
        assertEquals(Collections.singletonList(follow), followedActorInbox.getPendingFollows());
        verify(this.notifier).notify(follow, Collections.singleton(followedUserRef));
        assertFalse(follow.isAccepted());
        assertFalse(follow.isRejected());
    }

    @Test
    public void handleInboxReject() throws Exception
    {
        Person followingActor = new Person().setPreferredUsername("Following");
        Person followedActor = new Person().setPreferredUsername("Followed");
        Follow follow = new Follow()
            .setObject(followedActor)
            .setActor(followingActor)
            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Person>)follow.getObject())).thenReturn(followedActor);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(follow.getActor())).thenReturn(followingActor);
        Inbox followedActorInbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getInbox()))
            .thenReturn(followedActorInbox);
        when(this.activityPubConfiguration.getFollowPolicy()).thenReturn(ActivityPubConfiguration.FollowPolicy.REJECT);

        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        assertTrue(follow.isRejected());
        verify(this.activityPubStorage).storeEntity(follow);
        verify(this.notifier, never()).notify(any(), any());
        verifyResponse(401, "Follow request are not accepted on this server.");
    }

    @Test
    public void handleInboxAccept() throws Exception
    {
        OrderedCollection<AbstractActor> followings = new OrderedCollection<AbstractActor>().addItem(new Person());
        OrderedCollection<AbstractActor> followers = new OrderedCollection<>();

        Person followingActor = new Person()
            .setPreferredUsername("Following")
            .setFollowing(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(followings));
        Person followedActor = new Person()
            .setPreferredUsername("Followed")
            .setFollowers(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(followers));

        when(this.activityPubObjectReferenceResolver.resolveReference(followingActor.getFollowing()))
            .thenReturn(followings);
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getFollowers()))
            .thenReturn(followers);

        DocumentReference followedUserRef = new DocumentReference("xwiki", "XWiki", "Followed");
        when(this.actorHandler.getXWikiUserReference(followedActor)).thenReturn(followedUserRef);
        Follow follow = new Follow()
            .setObject(followedActor)
            .setActor(followingActor)
            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Person>)follow.getObject())).thenReturn(followedActor);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(follow.getActor())).thenReturn(followingActor);
        Inbox followedActorInbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getInbox()))
            .thenReturn(followedActorInbox);
        when(this.activityPubConfiguration.getFollowPolicy()).thenReturn(ActivityPubConfiguration.FollowPolicy.ACCEPT);

        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        assertTrue(follow.isAccepted());
        verify(this.activityPubStorage).storeEntity(follow);
        verify(this.activityPubStorage).storeEntity(followers);
        verify(this.activityPubStorage).storeEntity(followings);

        assertEquals(Arrays.asList(
            new ActivityPubObjectReference<>().setObject(new Person()),
            new ActivityPubObjectReference<>().setObject(followedActor)),
            followings.getOrderedItems());

        assertEquals(
            Collections.singletonList(new ActivityPubObjectReference<>().setObject(followingActor)),
            followers.getOrderedItems());

        verify(this.notifier).notify(follow, Collections.singleton(followedUserRef));
        verifyResponse(follow);
    }

    @Test
    public void handleOutboxFollowNoActorNoId() throws Exception
    {
        Follow follow = new Follow().setObject(new Document());
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Document>)follow.getObject())).thenReturn(new Document());
        this.handler.handleOutboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        this.verifyResponse(501, "Only following actors is implemented.");
        verify(this.activityPubStorage).storeEntity(follow);
    }

    @Test
    public void handleOutboxAsk() throws Exception
    {
        Person followingActor = new Person().setPreferredUsername("Following");
        Person followedActor = new Person().setPreferredUsername("Followed");
        DocumentReference followedUserRef = new DocumentReference("xwiki", "XWiki", "Followed");
        when(this.actorHandler.getXWikiUserReference(followedActor)).thenReturn(followedUserRef);
        Follow follow = new Follow()
            .setObject(followedActor)
            .setActor(followingActor)
            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Person>)follow.getObject())).thenReturn(followedActor);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(follow.getActor())).thenReturn(followingActor);
        Inbox followedActorInbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getInbox()))
            .thenReturn(followedActorInbox);
        when(this.activityPubConfiguration.getFollowPolicy()).thenReturn(ActivityPubConfiguration.FollowPolicy.ASK);

        this.handler.handleOutboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        verifyResponse(follow);
        verify(this.activityPubStorage).storeEntity(followedActorInbox);
        assertEquals(Collections.singletonList(follow), followedActorInbox.getPendingFollows());
        verify(this.notifier).notify(follow, Collections.singleton(followedUserRef));
        assertFalse(follow.isAccepted());
        assertFalse(follow.isRejected());
    }

    @Test
    public void handleOutboxReject() throws Exception
    {
        Person followingActor = new Person().setPreferredUsername("Following");
        Person followedActor = new Person().setPreferredUsername("Followed");
        Follow follow = new Follow()
            .setObject(followedActor)
            .setActor(followingActor)
            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Person>)follow.getObject())).thenReturn(followedActor);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(follow.getActor())).thenReturn(followingActor);
        Inbox followedActorInbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getInbox()))
            .thenReturn(followedActorInbox);
        when(this.activityPubConfiguration.getFollowPolicy()).thenReturn(ActivityPubConfiguration.FollowPolicy.REJECT);

        this.handler.handleOutboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        assertTrue(follow.isRejected());
        verify(this.activityPubStorage).storeEntity(follow);
        verify(this.notifier, never()).notify(any(), any());
        verifyResponse(401, "Follow request are not accepted on this server.");
    }

    @Test
    public void handleOutboxAccept() throws Exception
    {
        OrderedCollection<AbstractActor> followings = new OrderedCollection<AbstractActor>().addItem(new Person());
        OrderedCollection<AbstractActor> followers = new OrderedCollection<>();

        Person followingActor = new Person()
            .setPreferredUsername("Following")
            .setFollowing(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(followings));
        Person followedActor = new Person()
            .setPreferredUsername("Followed")
            .setFollowers(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(followers));

        when(this.activityPubObjectReferenceResolver.resolveReference(followingActor.getFollowing()))
            .thenReturn(followings);
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getFollowers()))
            .thenReturn(followers);

        DocumentReference followedUserRef = new DocumentReference("xwiki", "XWiki", "Followed");
        when(this.actorHandler.getXWikiUserReference(followedActor)).thenReturn(followedUserRef);
        Follow follow = new Follow()
            .setObject(followedActor)
            .setActor(followingActor)
            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference((ActivityPubObjectReference<Person>)follow.getObject())).thenReturn(followedActor);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(follow.getActor())).thenReturn(followingActor);
        Inbox followedActorInbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(followedActor.getInbox()))
            .thenReturn(followedActorInbox);
        when(this.activityPubConfiguration.getFollowPolicy()).thenReturn(ActivityPubConfiguration.FollowPolicy.ACCEPT);

        this.handler.handleOutboxRequest(
            new ActivityRequest<>(null, follow, this.servletRequest, this.servletResponse));
        assertTrue(follow.isAccepted());
        verify(this.activityPubStorage).storeEntity(follow);
        verify(this.activityPubStorage).storeEntity(followers);
        verify(this.activityPubStorage).storeEntity(followings);

        assertEquals(Arrays.asList(
            new ActivityPubObjectReference<>().setObject(new Person()),
            new ActivityPubObjectReference<>().setObject(followedActor)),
            followings.getOrderedItems());

        assertEquals(
            Collections.singletonList(new ActivityPubObjectReference<>().setObject(followingActor)),
            followers.getOrderedItems());

        verify(this.notifier).notify(follow, Collections.singleton(followedUserRef));
        verifyResponse(follow);
    }
}
