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
package org.xwiki.contrib.activitypub.internal;

import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.discussions.domain.ActorDescriptor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Test of {@link ActivityPubDiscussionsActorService}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ActivityPubDiscussionsActorServiceTest
{
    @InjectMockComponents
    private ActivityPubDiscussionsActorService activityPubDiscussionsActorService;

    @MockComponent
    private ActorHandler actorHandler;

    @Test
    void resolve() throws Exception
    {
        Person actor = new Person();
        URI actorID = URI.create("http://myserver.tld/myactor");
        actor.setId(actorID);
        actor.setName("My NAME");
        actor.setPreferredUsername("My pref NAME");

        when(this.actorHandler.getActor("@myactor@myserver.tld")).thenReturn(actor);
        Optional<ActorDescriptor> actual = this.activityPubDiscussionsActorService.resolve("@myactor@myserver.tld");
        ActorDescriptor expected = new ActorDescriptor();
        expected.setLink(actorID);
        expected.setName("My pref NAME");
        assertEquals(Optional.of(expected), actual);
    }

    @Test
    void resolveNoPreferredUsername() throws Exception
    {
        Person actor = new Person();
        URI actorID = URI.create("http://myserver.tld/myactor");
        actor.setId(actorID);
        actor.setName("My NAME");

        when(this.actorHandler.getActor("@myactor@myserver.tld")).thenReturn(actor);
        Optional<ActorDescriptor> actual = this.activityPubDiscussionsActorService.resolve("@myactor@myserver.tld");
        ActorDescriptor expected = new ActorDescriptor();
        expected.setLink(actorID);
        expected.setName("My NAME");
        assertEquals(Optional.of(expected), actual);
    }
}