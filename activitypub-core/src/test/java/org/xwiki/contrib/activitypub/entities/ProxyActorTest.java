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
package org.xwiki.contrib.activitypub.entities;

import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ProxyActor}.
 * 
 * @since 1.1
 * @version $Id$
 */
public class ProxyActorTest extends AbstractEntityTest
{
    @Test
    public void serialize() throws Exception
    {
        Person alissa = new Person().setId(new URI("https://social.example/alyssa/"));
        Person eve = new Person().setId(new URI("https://another.example/eve/"));
        AbstractActor bob = new Person()
            .setFollowers(new OrderedCollection<AbstractActor>()
                .setOrderedItems(Arrays.asList(
                    alissa.getReference(), eve.getReference()
                ))
                .setId(new URI("https://another.example/bob/followers"))
                .getReference()
            );

        Note note = new Note().setTo(Arrays.asList(
            new ProxyActor(bob.getFollowers().getLink()),
            ProxyActor.getPublicActor(),
            alissa.getProxyActor()));

        String expectedSerialization = this.readResource("note/note2.json");
        assertEquals(expectedSerialization, this.serializer.serialize(note));
    }

    @Test
    public void parse() throws Exception
    {
        Person alissa = new Person().setId(new URI("https://social.example/alyssa/"));
        Person eve = new Person().setId(new URI("https://another.example/eve/"));
        AbstractActor bob = new Person()
            .setFollowers(new OrderedCollection<AbstractActor>()
                .setOrderedItems(Arrays.asList(
                    alissa.getReference(), eve.getReference()
                ))
                .setId(new URI("https://another.example/bob/followers"))
                .getReference()
            );

        Note note = new Note().setTo(Arrays.asList(
            new ProxyActor(bob.getFollowers().getLink()),
            ProxyActor.getPublicActor(),
            alissa.getProxyActor()));

        String expectedSerialization = this.readResource("note/note2.json");
        assertEquals(note, this.parser.parse(expectedSerialization));
    }

    @Test
    public void resolveActors() throws Exception
    {
        ActivityPubObjectReferenceResolver resolver = mock(ActivityPubObjectReferenceResolver.class);
        ProxyActor proxyActor = new ProxyActor(new URI("http://foo/bar"));

        Note note = mock(Note.class);
        when(resolver.resolveReference(proxyActor)).thenReturn(note);
        when(note.toString()).thenReturn("Note");

        ActivityPubException activityPubException =
            assertThrows(ActivityPubException.class, () -> proxyActor.resolveActors(resolver));
        assertEquals("The given element cannot be processed here: [Note]", activityPubException.getMessage());

        Person person = mock(Person.class);
        ActivityPubObjectReference reference = mock(ActivityPubObjectReference.class);
        when(person.getReference()).thenReturn(reference);
        when(resolver.resolveReference(proxyActor)).thenReturn(person);
        assertEquals(Collections.singletonList(reference), proxyActor.resolveActors(resolver));

        Collection collection = mock(Collection.class);
        when(collection.getAllItems()).thenReturn(Arrays.asList("foo", "bar", "baz"));
        when(resolver.resolveReference(proxyActor)).thenReturn(collection);
        assertEquals(Arrays.asList("foo", "bar", "baz"), proxyActor.resolveActors(resolver));
    }
}
