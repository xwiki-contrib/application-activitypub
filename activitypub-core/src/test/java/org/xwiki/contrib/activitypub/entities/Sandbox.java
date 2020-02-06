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
import java.util.Collections;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.entities.activities.Create;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class Sandbox
{
    // FIXME: currently lead to stackoverflow: should we be able to deserialize abstract objects?...
    @Ignore
    @Test
    public void deserializeObject() throws FileNotFoundException, JsonProcessingException, URISyntaxException
    {
        String objectJsonResource = "./src/test/resources/json/object.json";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(objectJsonResource));
        String json = bufferedReader.lines().collect(Collectors.joining());
        ObjectMapper mapper = new ObjectMapper();
        ActivityPubObject object = mapper.readValue(json, ActivityPubObject.class);

        assertEquals("Object", object.getType());
        assertEquals(new URI("http://www.test.example/object/1"), object.getId());
        assertEquals(new URI("https://www.w3.org/ns/activitystreams"), object.getContext());
        assertEquals("A Simple, non-specific object", object.getName());
    }

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
        assertEquals("Alyssa P. Hacker", person.getName());
    }

    @Test
    public void deserializeNote() throws FileNotFoundException, JsonProcessingException, URISyntaxException
    {
        String objectJsonResource = "./src/test/resources/json/note.json";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(objectJsonResource));
        String json = bufferedReader.lines().collect(Collectors.joining());
        ObjectMapper mapper = new ObjectMapper().configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Note note = (Note) mapper.readValue(json, ActivityPubObject.class);

        assertEquals("Note", note.getType());
        assertEquals(new URI("https://www.w3.org/ns/activitystreams"), note.getContext());

        ActivityPubObjectReference<Person> benReference = new ActivityPubObjectReference<>();
        benReference.setLink(true);
        benReference.setLink(new URI("https://chatty.example/ben/"));

        ActivityPubObjectReference<Person> alyssaReference = new ActivityPubObjectReference<>();
        alyssaReference.setLink(true);
        alyssaReference.setLink(new URI("https://social.example/alyssa/"));

        assertEquals(Collections.singletonList(benReference), note.getTo());
        assertEquals(Collections.singletonList(alyssaReference), note.getAttributedTo());
        assertEquals("Say, did you finish reading that book I lent you?", note.getContent());
    }

    @Test
    public void deserializeCreate() throws FileNotFoundException, JsonProcessingException, URISyntaxException
    {
        String objectJsonResource = "./src/test/resources/json/create.json";
        BufferedReader bufferedReader = new BufferedReader(new FileReader(objectJsonResource));
        String json = bufferedReader.lines().collect(Collectors.joining());
        ObjectMapper mapper = new ObjectMapper().configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        Create create = (Create) mapper.readValue(json, ActivityPubObject.class);

        ActivityPubObjectReference<Actor> benReference = new ActivityPubObjectReference<>();
        benReference.setLink(true);
        benReference.setLink(new URI("https://chatty.example/ben/"));

        ActivityPubObjectReference<Actor> alyssaReference = new ActivityPubObjectReference<>();
        alyssaReference.setLink(true);
        alyssaReference.setLink(new URI("https://social.example/alyssa/"));

        assertEquals("Create", create.getType());
        assertEquals(new URI("https://www.w3.org/ns/activitystreams"), create.getContext());
        assertEquals(new URI("https://social.example/alyssa/posts/a29a6843-9feb-4c74-a7f7-081b9c9201d3"),
            create.getId());
        assertEquals(Collections.singletonList(benReference), create.getTo());
        assertEquals(alyssaReference, create.getActor());

        Note note = new Note();
        note.setId(new URI("https://social.example/alyssa/posts/49e2d03d-b53a-4c4c-a95c-94a6abf45a19"));
        note.setAttributedTo(Collections.singletonList(alyssaReference));
        note.setTo(Collections.singletonList(benReference));
        note.setContent("Say, did you finish reading that book I lent you?");
        ActivityPubObjectReference<Note> noteReference = new ActivityPubObjectReference<>();
        noteReference.setLink(false);
        noteReference.setObject(note);
        assertEquals(noteReference, create.getObject());
    }
}
