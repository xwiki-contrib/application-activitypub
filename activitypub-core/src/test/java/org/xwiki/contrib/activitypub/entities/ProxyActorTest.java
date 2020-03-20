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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
