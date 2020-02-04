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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActorTest
{
    @Test
    public void deserializePerson() throws FileNotFoundException, JsonProcessingException, URISyntaxException
    {
        String objectJsonResource = "./src/test/resources/json/person.json";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(objectJsonResource));
        String json = bufferedReader.lines().collect(Collectors.joining());
        ObjectMapper mapper = new ObjectMapper();
        Actor actor = mapper.readValue(json, Actor.class);

        assertEquals("Person", actor.getType());
        assertEquals(new URI("https://www.w3.org/ns/activitystreams"), actor.getContext());
        assertEquals(new URI("https://social.example/alyssa/"), actor.getId());
        assertEquals("Alyssa P. Hacker", actor.getName());
        assertEquals("alyssa", actor.getPreferredUsername());
        assertEquals("Lisp enthusiast hailing from MIT", actor.getSummary());
        assertEquals(new URI("https://social.example/alyssa/inbox/"), actor.getInbox());
        assertEquals(new URI("https://social.example/alyssa/outbox/"), actor.getOutbox());
        assertEquals(new ObjectReference<>().setLink(true).setLink(new URI("https://social.example/alyssa/followers/")),
            actor.getFollowers());
        assertEquals(new ObjectReference<>().setLink(true).setLink(new URI("https://social.example/alyssa/following/")),
            actor.getFollowing());
        assertEquals(new URI("https://social.example/alyssa/liked/"), actor.getLiked());
    }
}
