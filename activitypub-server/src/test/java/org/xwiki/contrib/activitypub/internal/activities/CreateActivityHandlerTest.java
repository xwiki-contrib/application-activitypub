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
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CreateActivityHandler}.
 *
 * @version $Id$
 */
@ComponentTest
public class CreateActivityHandlerTest extends AbstractHandlerTest
{
    @InjectMockComponents
    private CreateActivityHandler handler;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @BeforeEach
    public void setup() throws Exception
    {
        this.initMock();
    }

    @Test
    public void handleInboxNoId() throws Exception
    {
        this.handler.handleInboxRequest(new ActivityRequest<>(null, new Create().setObject(new Note())));
        assertEquals("The ID of the activity must not be null.", logCapture.getMessage(0));

        this.handler.handleInboxRequest(
            new ActivityRequest<>(null, new Create().setObject(new Note()), this.servletRequest, this.servletResponse));
        this.verifyResponse(400, "The ID of the activity must not be null.");
    }

    @Test
    public void handleInbox() throws Exception
    {
        Create activity = new Create().setObject(new Note()).setId(new URI("http://www.xwiki.org"));
        Person actor = new Person()
            .setPreferredUsername("XWiki.Foo")
            .setInbox(new ActivityPubObjectReference<Inbox>());
        Inbox inbox = new Inbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(actor.getInbox())).thenReturn(inbox);

        this.handler.handleInboxRequest(
            new ActivityRequest<>(actor, activity, this.servletRequest, this.servletResponse));
        assertEquals(1, inbox.getAllItems().size());
        assertTrue(inbox.getAllItems().contains(activity.getReference()));
        verify(this.activityPubStorage).storeEntity(inbox);
        verify(this.notifier).notify(activity, Collections.singleton(actor));
        verifyResponse(activity);
    }

    @Test
    public void handleOutboxNoFollowersNoId() throws Exception
    {
        Create activity = new Create();
        UserReference userReference = mock(UserReference.class);
        Person actor = new Person()
            .setPreferredUsername("XWiki.Foo")
            .setOutbox(new ActivityPubObjectReference<Outbox>());
        Outbox outbox = new Outbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(actor.getOutbox())).thenReturn(outbox);
        when(this.actorHandler.getXWikiUserReference(actor)).thenReturn(userReference);

        when(this.activityPubStorage.storeEntity(activity)).then(invocationOnMock -> {
            Create create = (Create) invocationOnMock.getArguments()[0];
            create.setId(new URI("http://www.xwiki.org"));
            return null;
        });
        this.handler.handleOutboxRequest(
            new ActivityRequest<>(actor, activity, this.servletRequest, this.servletResponse));
        verify(this.activityPubStorage).storeEntity(activity);
        assertEquals(1, outbox.getAllItems().size());
        assertTrue(outbox.getAllItems().contains(activity.getReference()));
        verify(this.activityPubStorage).storeEntity(outbox);
        verify(this.notifier, never()).notify(any(), any());
        verifyResponse(activity);
    }

    @Test
    public void handleOutbox() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        Person follower1 = new Person()
                               .setPreferredUsername("Bar");

        Person follower2 = new Person().setPreferredUsername("Baz");

        Create activity = new Create()
                              .setObject(new Note())
                              .setId(new URI("http://www.xwiki.org"))
                              .setTo(Arrays.asList(new ProxyActor(URI.create("http://followers"))));
        when(this.activityPubObjectReferenceResolver.resolveTargets(activity))
            .thenReturn(new HashSet<>(Arrays.asList(follower1, follower2)));

        Person actor = new Person()
                           .setPreferredUsername("XWiki.Foo")
                           .setOutbox(new ActivityPubObjectReference<Outbox>());
        Outbox outbox = new Outbox();
        when(this.activityPubObjectReferenceResolver.resolveReference(actor.getOutbox())).thenReturn(outbox);
        when(this.actorHandler.getXWikiUserReference(actor)).thenReturn(userReference);

        this.handler.handleOutboxRequest(
            new ActivityRequest<>(actor, activity, this.servletRequest, this.servletResponse));
        assertEquals(1, outbox.getAllItems().size());
        assertTrue(outbox.getAllItems().contains(activity.getReference()));
        verify(this.activityPubStorage).storeEntity(outbox);
        verify(this.notifier, never()).notify(any(), any());
        this.verifyResponse(activity);
        verify(this.activityPubClient, times(2)).checkAnswer(any());
        verify(this.activityPubClient).postInbox(follower1, activity);
        verify(this.activityPubClient).postInbox(follower2, activity);
        verify(this.postMethod, times(2)).releaseConnection();
    }
}
