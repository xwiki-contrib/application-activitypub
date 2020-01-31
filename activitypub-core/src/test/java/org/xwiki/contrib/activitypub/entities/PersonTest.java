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
import org.xwiki.contrib.activitypub.entities.Person;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersonTest
{
    @Test
    public void deserializePerson() throws FileNotFoundException, JsonProcessingException, URISyntaxException
    {
        String objectJsonResource = "./src/test/resources/json/person.json";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(objectJsonResource));
        String json = bufferedReader.lines().collect(Collectors.joining());
        ObjectMapper mapper = new ObjectMapper();
        Person person = mapper.readValue(json, Person.class);

        assertEquals("Person", person.getType());
        assertEquals(new URI("https://www.w3.org/ns/activitystreams"), person.getContext());
        assertEquals(new URI("https://social.example/alyssa/"), person.getId());
        assertEquals("Alyssa P. Hacker", person.getName());
        assertEquals("alyssa", person.getPreferredUsername());
        assertEquals("Lisp enthusiast hailing from MIT", person.getSummary());
        assertEquals(new URI("https://social.example/alyssa/inbox/"), person.getInbox());
        assertEquals(new URI("https://social.example/alyssa/outbox/"), person.getOutbox());
        assertEquals(new URI("https://social.example/alyssa/followers/"), person.getFollowers());
        assertEquals(new URI("https://social.example/alyssa/following/"), person.getFollowing());
        assertEquals(new URI("https://social.example/alyssa/liked/"), person.getLiked());
    }
}
