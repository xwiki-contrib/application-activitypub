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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Reject;
import org.xwiki.model.reference.DocumentReference;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RejectActivityHandler}.
 *
 * @version $Id$
 */
@ComponentTest
public class RejectActivityHandlerTest extends AbstractHandlerTest
{
    @InjectMockComponents
    private RejectActivityHandler handler;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeEach
    public void setup() throws Exception
    {
        this.initMock();
    }

    @Test
    public void handleInboxIDNull() throws Exception
    {
        this.handler.handleInboxRequest(new ActivityRequest<>(null, new Reject()));
        assertEquals("The ID of the activity must not be null.", logCapture.getMessage(0));

        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, new Reject(), this.servletRequest, this.servletResponse));
        this.verifyResponse(400, "The ID of the activity must not be null.");
    }

    @Test
    public void handleInboxNotFollowReject() throws Exception
    {
        Reject reject = new Reject().setObject(new Document()).setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Document>) reject.getObject())).thenReturn(new Document());
        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, reject, this.servletRequest, this.servletResponse));
        this.verifyResponse(501, "Only follow activities can be accepted in the current implementation.");
    }

    @Test
    public void handleInbox() throws Exception
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

        Reject reject = new Reject()
                            .setActor(followedPerson.getReference())
                            .setObject(follow.getReference())
                            .setId(new URI("http://www.xwiki.org"));
        when(this.activityPubObjectReferenceResolver.resolveReference(reject.getActor())).thenReturn(followedPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Follow>) reject.getObject())).thenReturn(follow);
        when(this.activityPubObjectReferenceResolver.resolveReference(follow.getActor())).thenReturn(followingPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(followingPerson.getFollowing()))
            .thenReturn(following);
        when(this.actorHandler.getXWikiUserReference(followingPerson)).thenReturn(followingRef);

        this.handler.handleInboxRequest(
            new ActivityRequest<>(followingPerson, reject, this.servletRequest, this.servletResponse));
        this.verifyResponse(reject);
        assertEquals(new ArrayList<>(), following.getOrderedItems());
    }

    @Test
    public void handleOutbox() throws Exception
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

        Reject reject = new Reject()
                            .setActor(followedPerson.getReference())
                            .setObject(follow.getReference());
        when(this.activityPubObjectReferenceResolver.resolveReference(reject.getActor())).thenReturn(followedPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Follow>) reject.getObject())).thenReturn(follow);
        when(this.activityPubObjectReferenceResolver.resolveReference(follow.getActor())).thenReturn(followingPerson);
        when(this.activityPubObjectReferenceResolver.resolveReference(followedPerson.getFollowers()))
            .thenReturn(followers);
        when(this.actorHandler.getXWikiUserReference(followedPerson)).thenReturn(followedRef);

        this.handler.handleOutboxRequest(
            new ActivityRequest<>(followedPerson, reject, this.servletRequest, this.servletResponse));
        verifyResponse(reject);
        assertEquals(new ArrayList<>(), followers.getOrderedItems());
        verify(this.notifier, never()).notify(eq(reject), any(Set.class));
        verify(this.activityPubClient).checkAnswer(any());
        verify(this.activityPubClient).postInbox(followingPerson, reject);
        verify(this.postMethod).releaseConnection();
    }
}
