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
package org.xwiki.contrib.activitypub.internal.scipt;

import java.net.URI;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import ch.qos.logback.classic.Level;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ActivityPubScriptServiceActor}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ActivityPubScriptServiceActorTest
{
    @InjectMockComponents
    private ActivityPubScriptServiceActor target;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void getSourceActor() throws Exception
    {
        UserReference mock = mock(UserReference.class);
        Person anotherActor = new Person();
        when(this.userReferenceResolver.resolve(null)).thenReturn(mock);
        when(this.actorHandler.getCurrentActor()).thenReturn(anotherActor);
        when(this.actorHandler.isAuthorizedToActFor(mock, anotherActor)).thenReturn(true);
        Person expected = new Person();
        AbstractActor actual = this.target.getSourceActor(expected);
        assertSame(expected, actual);
    }

    @Test
    void getSourceActorNotAutorized() throws Exception
    {
        UserReference mock = mock(UserReference.class);
        Person expected = new Person();
        when(this.userReferenceResolver.resolve(null)).thenReturn(mock);
        when(this.actorHandler.getCurrentActor()).thenReturn(expected);
        when(this.actorHandler.isAuthorizedToActFor(mock, expected)).thenReturn(false);
        ActivityPubException activityPubException =
            assertThrows(ActivityPubException.class, () -> this.target.getSourceActor(new Person()));
        assertEquals("You cannot act on behalf of actor [id = [<null>], name = [<null>], "
            + "preferredUsername = [<null>], publicKey = [<null>]]", activityPubException.getMessage());
    }

    @Test
    void getSourceActorNull() throws Exception
    {
        when(this.userReferenceResolver.resolve(null)).thenReturn(mock(UserReference.class));
        Person expected = new Person();
        when(this.actorHandler.getCurrentActor()).thenReturn(expected);
        AbstractActor actual = this.target.getSourceActor(null);
        assertSame(expected, actual);
    }

    @Test
    void getSourceActorIsGuest() throws Exception
    {
        when(this.userReferenceResolver.resolve(null)).thenReturn(GuestUserReference.INSTANCE);
        ActivityPubException activityPubException =
            assertThrows(ActivityPubException.class, () -> this.target.getSourceActor(null));
        assertEquals("You need to be authenticated to use this method.", activityPubException.getMessage());
    }

    @Test
    void fillRecipients() throws Exception
    {
        Note object = new Note();
        ActivityPubObjectReference<OrderedCollection<AbstractActor>> activityPubObjectActivityPubObjectReference =
            new ActivityPubObjectReference<OrderedCollection<AbstractActor>>()
                .setLink(URI.create("https://server/actor/followers"));
        Person actor = new Person()
            .setFollowers(activityPubObjectActivityPubObjectReference);
        AbstractActor user = new Person().setId(URI.create("https://server2/user"));
        when(this.actorHandler.getActor("user@server2")).thenReturn(user);
        when(this.actorHandler.getActor("notfound@server3")).thenThrow(ActivityPubException.class);

        this.target.fillRecipients(asList("followers", "public", "user@server2", "notfound@server3"), actor, object);
        assertEquals(3, object.getTo().size());
        assertEquals(asList(
            new ProxyActor(actor.getFollowers().getLink()),
            ProxyActor.getPublicActor(),
            user.getProxyActor()

        ), object.getTo());
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while trying to get the actor [notfound@server3].", this.logCapture.getMessage(0));
    }

    @Test
    void getActor() throws Exception
    {
        Person value = new Person();
        when(this.actorHandler.getActor("actorId")).thenReturn(value);
        AbstractActor actorId = this.target.getActor("actorId");
        assertEquals(value, actorId);
    }

    @Test
    void getActorException() throws Exception
    {
        when(this.actorHandler.getActor("actorId")).thenThrow(ActivityPubException.class);
        AbstractActor actorId = this.target.getActor("actorId");
        assertNull(actorId);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while trying to get the actor [actorId].", this.logCapture.getMessage(0));
    }

    @Test
    void getActorNull() throws Exception
    {
        Person value = new Person();
        when(this.userReferenceResolver.resolve(null)).thenReturn(mock(UserReference.class));
        when(this.actorHandler.getCurrentActor()).thenReturn(value);
        AbstractActor actor = this.target.getActor(null);
        assertEquals(value, actor);
    }

    @Test
    void getActorNullUserIsGuest()
    {
        when(this.userReferenceResolver.resolve(null)).thenReturn(GuestUserReference.INSTANCE);
        AbstractActor actor = this.target.getActor(null);
        assertNull(actor);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while trying to get the actor [null].", this.logCapture.getMessage(0));
    }

    @Test
    void checkAuthentication() throws Exception
    {
        when(this.userReferenceResolver.resolve(null)).thenReturn(mock(UserReference.class));
        this.target.checkAuthentication();
        verify(this.userReferenceResolver).resolve(null);
    }

    @Test
    void checkAuthenticationGuest()
    {
        when(this.userReferenceResolver.resolve(null)).thenReturn(GuestUserReference.INSTANCE);
        ActivityPubException activityPubException =
            assertThrows(ActivityPubException.class, () -> this.target.checkAuthentication());
        assertEquals("You need to be authenticated to use this method.", activityPubException.getMessage());
    }

    @Test
    void getActivityHandler() throws Exception
    {
        ActivityHandler expected = mock(ActivityHandler.class);
        when(this.componentManager
            .getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Create.class)))
            .thenReturn(expected);
        ActivityHandler<Create> actual = this.target.getActivityHandler(new Create());
        assertEquals(expected, actual);
    }

    @Test
    void getActivityHandlerUnknownActivity() throws Exception
    {
        when(this.componentManager
            .getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Create.class)))
            .thenThrow(ComponentLookupException.class);
        ActivityPubException activityPubException =
            assertThrows(ActivityPubException.class, () -> this.target.getActivityHandler(new Create()));
        assertEquals("Cannot find activity handler component for activity "
                + "[class org.xwiki.contrib.activitypub.entities.Create]",
            activityPubException.getMessage());
    }
}