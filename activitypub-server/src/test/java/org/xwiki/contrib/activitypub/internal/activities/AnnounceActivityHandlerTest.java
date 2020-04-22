package org.xwiki.contrib.activitypub.internal.activities;/*
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

import java.net.URI;
import java.util.ArrayList;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Announce;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.user.UserReference;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link AnnounceActivityHandler}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
class AnnounceActivityHandlerTest extends AbstractHandlerTest
{
    @InjectMockComponents
    private AnnounceActivityHandler handler;

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
        this.handler.handleInboxRequest(new ActivityRequest<>(null, new Announce()));
        assertEquals("The ID of the activity must not be null.", this.logCapture.getMessage(0));

        this.handler.handleInboxRequest(
                new ActivityRequest<>(null, new Announce(), this.servletRequest, this.servletResponse));
        this.verifyResponse(400, "The ID of the activity must not be null.");
    }

    @Test
    void handleInbox() throws Exception
    {
        Person sender = new Person()
                .setPreferredUsername("Followed");

        ActivityPubObjectReference<Inbox> inboxRef = new ActivityPubObjectReference<>();
        inboxRef.setLink(URI.create("http://inbox/1"));
        OrderedCollection<AbstractActor> following = new OrderedCollection<>().setName("following");
        Person receiver = new Person()
                .setPreferredUsername("Receiver")
                .setFollowing(following.getReference())
                .setInbox(inboxRef);

        ActivityPubObjectReference<OrderedCollection<Announce>> shares = new ActivityPubObjectReference<>();
        OrderedCollection<Announce> sharesCollection = new OrderedCollection<>();
        shares.setObject(sharesCollection);
        Document document = new Document();
        document.setShares(shares);
        Announce announce = new Announce()
                .setActor(sender.getReference())
                .setObject(document)
                .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver.resolveReference(announce.getActor())).thenReturn(sender);
        when(this.activityPubObjectReferenceResolver.resolveReference(
                (ActivityPubObjectReference<Document>) announce.getObject())).thenReturn(document);
        when(this.activityPubObjectReferenceResolver.resolveReference(receiver.getFollowing())).thenReturn(following);

        Inbox inbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(inboxRef)).thenReturn(inbox);

        this.handler.handleInboxRequest(
                new ActivityRequest<>(receiver, announce, this.servletRequest, this.servletResponse));

        this.verifyResponse(announce);
        assertEquals(announce, new ArrayList<>(inbox.getAllActivities()).get(0));
        verify(this.activityPubStorage).storeEntity(inbox);
        assertEquals(new URI("http://www.xwiki.org"), sharesCollection
                .getOrderedItems()
                .get(0)
                .getLink());
        verify(this.activityPubStorage).storeEntity(document);
        verify(this.notifier).notify(eq(announce), eq(singleton(receiver)));
    }

    @Disabled
    @Test
    void handleOutbox() throws Exception
    {
        OrderedCollection<AbstractActor> followers = new OrderedCollection<>().setName("followers");
        UserReference followedRef = mock(UserReference.class);
        ActivityPubObjectReference<Outbox> outboxRef = new ActivityPubObjectReference<>();
        outboxRef.setLink(URI.create("http://outbox/1"));
        Person receiver = new Person()
                .setPreferredUsername("Receiver")
                .setFollowers(followers.getReference())
                .setOutbox(outboxRef);

        Person sender = new Person()
                .setPreferredUsername("Sender");

        Follow follow = new Follow()
                .setActor(sender.getReference())
                .setObject(receiver.getReference());

        Document document = new Document()
                .setId(URI.create("http://create/1"))
                .setContent("Content");
        ProxyActor pa = new ProxyActor(URI.create("http://to/1"));
        Announce announce = new Announce()
                .setActor(receiver.getReference())
                .setObject(document)
                .setId(URI.create("http://announce/1"))
                .setTo(singletonList(pa));
        when(this.activityPubObjectReferenceResolver.resolveReference(announce.getActor())).thenReturn(receiver);
        when(this.activityPubObjectReferenceResolver.resolveReference(
                (ActivityPubObjectReference<Document>) announce.getObject())).thenReturn(document);
        when(this.activityPubObjectReferenceResolver.resolveReference(follow.getActor())).thenReturn(sender);
        when(this.activityPubObjectReferenceResolver.resolveReference(receiver.getFollowers()))
                .thenReturn(followers);
        Outbox outbox = new Outbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(outboxRef)).thenReturn(outbox);
        when(this.actorHandler.getXWikiUserReference(receiver)).thenReturn(followedRef);
        Person to = new Person().setName("TO");
        when(this.activityPubObjectReferenceResolver.resolveReference(pa)).thenReturn(to);
        when(this.activityPubObjectReferenceResolver.resolveReference(to.getReference())).thenReturn(to);

        this.handler.handleOutboxRequest(
                new ActivityRequest<>(receiver, announce, this.servletRequest, this.servletResponse));
        this.verifyResponse(announce);
        verify(this.activityPubStorage).storeEntity(outbox);
        verify(this.activityPubClient).postInbox(to, announce);
        verify(this.activityPubClient).checkAnswer(any());
    }
}