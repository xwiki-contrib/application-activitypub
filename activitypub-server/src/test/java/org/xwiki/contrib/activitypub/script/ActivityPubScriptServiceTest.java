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
package org.xwiki.contrib.activitypub.script;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.httpclient.HttpMethod;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ActivityPubScriptService}.
 *
 * @version $Id$
 * @since 1.1
 */
@ComponentTest
class ActivityPubScriptServiceTest
{
    @InjectMockComponents
    private ActivityPubScriptService scriptService;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private ActivityPubClient activityPubClient;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private ActivityHandler<Create> createActivityHandler;

    @MockComponent
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Test
    void follow() throws Exception
    {
        Person remoteActor = mock(Person.class);
        Person currentActor = mock(Person.class);

        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(remoteActor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(currentActor);

        AbstractActor targetActor = mock(AbstractActor.class);
        ActivityPubObjectReference targetActorReference = mock(ActivityPubObjectReference.class);
        when(targetActor.getReference()).thenReturn(targetActorReference);
        when(this.actorHandler.getActor("test")).thenReturn(targetActor);

        when(this.activityPubClient.postInbox(any(), any())).thenReturn(mock(HttpMethod.class));
        FollowResult actual = this.scriptService.follow(remoteActor);
        assertTrue(actual.isSuccess());
        assertEquals("activitypub.follow.followRequested", actual.getMessage());
        verify(this.activityPubStorage, times(1)).storeEntity(any());
        verify(this.activityPubClient, times(1)).postInbox(eq(remoteActor), any());
        verify(this.activityPubClient, times(1)).checkAnswer(any());
    }

    @Test
    void followYourself() throws Exception
    {
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        AbstractActor targetActor = mock(AbstractActor.class);
        ActivityPubObjectReference targetActorReference = mock(ActivityPubObjectReference.class);
        when(targetActor.getReference()).thenReturn(targetActorReference);
        when(this.actorHandler.getActor("test")).thenReturn(targetActor);

        when(this.activityPubClient.postInbox(any(), any())).thenReturn(mock(HttpMethod.class));
        FollowResult actual = this.scriptService.follow(actor);
        assertFalse(actual.isSuccess());
        assertEquals("activitypub.follow.followYourself", actual.getMessage());
        verify(this.activityPubStorage, times(0)).storeEntity(any());
        verify(this.activityPubClient, times(0)).postInbox(eq(actor), any());
        verify(this.activityPubClient, times(0)).checkAnswer(any());
    }

    @Test
    void followAlreadyFollowing() throws Exception
    {
        Person actor = mock(Person.class);
        Person current = mock(Person.class);
        ActivityPubObjectReference mock = mock(ActivityPubObjectReference.class);
        when(current.getFollowing()).thenReturn(mock);
        OrderedCollection orderedCollection = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(mock)).thenReturn(orderedCollection);
        List list1 = mock(List.class);
        when(orderedCollection.getOrderedItems()).thenReturn(list1);
        Stream stream1 = mock(Stream.class);
        when(list1.stream()).thenReturn(stream1);
        Stream stream2 = mock(Stream.class);
        when(stream1.map(any())).thenReturn(stream2);
        when(stream2.anyMatch(any())).thenReturn(true);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(current);

        AbstractActor targetActor = mock(AbstractActor.class);
        ActivityPubObjectReference targetActorReference = mock(ActivityPubObjectReference.class);
        when(targetActor.getReference()).thenReturn(targetActorReference);
        when(this.actorHandler.getActor("test")).thenReturn(targetActor);

        when(this.activityPubClient.postInbox(any(), any())).thenReturn(mock(HttpMethod.class));
        FollowResult actual = this.scriptService.follow(actor);
        assertFalse(actual.isSuccess());
        assertEquals("activitypub.follow.alreadyFollowed", actual.getMessage());
        verify(this.activityPubStorage, times(0)).storeEntity(any());
        verify(this.activityPubClient, times(0)).postInbox(eq(actor), any());
        verify(this.activityPubClient, times(0)).checkAnswer(any());
    }

    @Test
    void following() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        ActivityPubObjectReference apor = mock(ActivityPubObjectReference.class);
        when(actor.getFollowing()).thenReturn(apor);
        when(this.activityPubObjectReferenceResolver.resolveReference(apor)).thenReturn(mock(OrderedCollection.class));
        List<AbstractActor> res = this.scriptService.following(actor);
        assertTrue(res.isEmpty());
    }

    @Test
    void followers() throws Exception
    {
        AbstractActor aa = mock(AbstractActor.class);
        ActivityPubObjectReference apor = mock(ActivityPubObjectReference.class);
        when(aa.getFollowers()).thenReturn(apor);
        when(this.actorHandler.getActor("User.Test")).thenReturn(aa);
        when(this.activityPubObjectReferenceResolver.resolveReference(apor)).thenReturn(mock(
            OrderedCollection.class));
        List<AbstractActor> res = this.scriptService.followers(aa);
        assertTrue(res.isEmpty());
    }

    @Test
    public void publishNoteNoTarget() throws Exception
    {
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        String noteContent = "some content";
        this.scriptService.publishNote(null, noteContent);
        Note note = new Note()
                        .setContent(noteContent)
                        .setAttributedTo(Collections.singletonList(actor.getReference()));

        ArgumentCaptor<ActivityPubObject> argumentCaptor = ArgumentCaptor.forClass(ActivityPubObject.class);
        verify(this.activityPubStorage, times(2)).storeEntity(argumentCaptor.capture());
        List<ActivityPubObject> allValues = argumentCaptor.getAllValues();
        assertEquals(2, allValues.size());
        assertTrue(allValues.get(0) instanceof Note);
        assertTrue(allValues.get(1) instanceof Create);
        assertEquals(note, allValues.get(0));

        Create create = (Create) allValues.get(1);
        assertEquals(note, create.getObject().getObject());
        assertSame(actorReference, create.getActor());
        assertEquals(note.getAttributedTo(), create.getAttributedTo());
        assertEquals(note.getTo(), create.getTo());
        assertNotNull(create.getPublished());
        verify(this.createActivityHandler).handleOutboxRequest(new ActivityRequest<>(actor, create));
    }

    @Test
    public void publishNoteFollowersAndActor() throws Exception
    {
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);

        ActivityPubObjectReference followersReference = mock(ActivityPubObjectReference.class);
        when(followersReference.getLink()).thenReturn(new URI("http://followers"));
        when(actor.getFollowers()).thenReturn(followersReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        AbstractActor targetActor = mock(AbstractActor.class);
        ProxyActor targetProxyActor = mock(ProxyActor.class);
        when(targetActor.getProxyActor()).thenReturn(targetProxyActor);
        when(actorHandler.getActor("@targetActor")).thenReturn(targetActor);

        String noteContent = "some content";
        this.scriptService.publishNote(Arrays.asList("followers", "@targetActor"), noteContent);

        Note note = new Note()
                        .setContent(noteContent)
                        .setAttributedTo(Collections.singletonList(actor.getReference()))
                        .setTo(Arrays.asList(new ProxyActor(followersReference.getLink()), targetProxyActor));

        ArgumentCaptor<ActivityPubObject> argumentCaptor = ArgumentCaptor.forClass(ActivityPubObject.class);
        verify(this.activityPubStorage, times(2)).storeEntity(argumentCaptor.capture());
        List<ActivityPubObject> allValues = argumentCaptor.getAllValues();
        assertEquals(2, allValues.size());
        assertTrue(allValues.get(0) instanceof Note);
        assertTrue(allValues.get(1) instanceof Create);
        assertEquals(note, allValues.get(0));

        Create create = (Create) allValues.get(1);
        assertEquals(note, create.getObject().getObject());
        assertSame(actorReference, create.getActor());
        assertEquals(note.getAttributedTo(), create.getAttributedTo());
        assertEquals(note.getTo(), create.getTo());
        assertNotNull(create.getPublished());
        verify(this.createActivityHandler).handleOutboxRequest(new ActivityRequest<>(actor, create));
    }

    @Test
    public void isCurrentUser() throws ActivityPubException
    {
        Person actor = mock(Person.class);
        UserReference userReference = mock(UserReference.class);

        when(this.userReferenceResolver.resolve(null)).thenReturn(userReference);
        when(this.actorHandler.getXWikiUserReference(actor)).thenReturn(userReference);
        assertTrue(this.scriptService.isCurrentUser(actor));

        when(this.userReferenceResolver.resolve(null)).thenReturn(mock(UserReference.class));
        assertFalse(this.scriptService.isCurrentUser(actor));
    }
}