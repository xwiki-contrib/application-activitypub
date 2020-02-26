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
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AcceptTest extends AbstractEntityTest
{
    @Test
    public void serializationWithReferences() throws URISyntaxException, IOException, ActivityPubException
    {
        Accept accept = new Accept()
                            .setActor(
                                new ActivityPubObjectReference<AbstractActor>()
                                    .setLink(new URI("https://social.example/alyssa/"))
                            )
                            .setObject(
                                new ActivityPubObjectReference<Note>()
                                    .setLink(new URI(
                                        "https://social.example/alyssa/posts/49e2d03d-b53a-4c4c-a95c-94a6abf45a19"))
                            )
                            .setId(new URI("https://social.example/alyssa/posts/a29a6843-9feb-4c74-a7f7-081b9c9201d3"))
                            .setTo(Collections.singletonList(
                                new ActivityPubObjectReference<AbstractActor>()
                                    .setLink(new URI("https://chatty.example/ben/"))
                            ));
        String expectedJson = this.readResource("accept/accept1.json");
        assertEquals(expectedJson, this.serializer.serialize(accept));
    }

    @Test
    public void serializationWithObjects() throws URISyntaxException, IOException, ActivityPubException
    {
        Accept accept = new Accept()
                            .setActor(
                                new Person()
                                    .setId(new URI("https://social.example/alyssa/"))
                            )
                            .setObject(
                                new Note()
                                    .setId(new URI(
                                        "https://social.example/alyssa/posts/49e2d03d-b53a-4c4c-a95c-94a6abf45a19"))
                            )
                            .setId(new URI("https://social.example/alyssa/posts/a29a6843-9feb-4c74-a7f7-081b9c9201d3"))
                            .setTo(Collections.singletonList(
                                new ActivityPubObjectReference<AbstractActor>()
                                    .setLink(new URI("https://chatty.example/ben/"))
                            ));
        String expectedJson = this.readResource("accept/accept1.json");
        assertEquals(expectedJson, this.serializer.serialize(accept));
    }

    @Test
    public void parsing() throws FileNotFoundException, URISyntaxException, ActivityPubException
    {
        Accept expectedAccept = new Accept()
                                    .setActor(
                                        new ActivityPubObjectReference<AbstractActor>()
                                            .setLink(new URI("https://social.example/alyssa/"))
                                    )
                                    .setObject(
                                        new ActivityPubObjectReference<Note>()
                                            .setLink(new URI(
                                                "https://social.example/alyssa/posts/49e2d03d-b53a-4c4c-a95c-94a6abf45a19"))
                                    )
                                    .setId(new URI(
                                        "https://social.example/alyssa/posts/a29a6843-9feb-4c74-a7f7-081b9c9201d3"))
                                    .setTo(Collections.singletonList(
                                        new ActivityPubObjectReference<AbstractActor>()
                                            .setLink(new URI("https://chatty.example/ben/"))
                                    ));
        String json = this.readResource("accept/accept1.json");
        assertEquals(expectedAccept, this.parser.parse(json));
    }
}
