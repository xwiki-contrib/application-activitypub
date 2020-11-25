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

import java.net.URI;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ActivityPubObjectReference}.
 */
public class ActivityPubObjectReferenceTest
{
    @Test
    void equals()
    {
        ActivityPubObjectReference<Person> personRef1 = new ActivityPubObjectReference<Person>()
            .setLink(URI.create("http://foo"));
        ActivityPubObjectReference<Person> personRef2 = new ActivityPubObjectReference<Person>()
            .setLink(URI.create("http://foo"));

        assertFalse(personRef1.equals(null));
        assertNotEquals(personRef1, new ActivityPubObjectReference<>());
        assertEquals(personRef1, personRef2);

        personRef2 = new ActivityPubObjectReference<Person>()
            .setLink(URI.create("http://bar"));
        assertNotEquals(personRef1, personRef2);

        Person person = mock(Person.class);
        when(person.getId()).thenReturn(URI.create("http://foo"));
        personRef2 = new ActivityPubObjectReference<Person>()
            .setObject(person);
        assertNotEquals(personRef1, personRef2);

        personRef1 = new ActivityPubObjectReference<Person>()
            .setObject(person);
        assertEquals(personRef1, personRef2);
    }

    @Test
    void concernsSameObject()
    {
        ActivityPubObjectReference<Person> personRef1 = new ActivityPubObjectReference<Person>()
            .setLink(URI.create("http://foo"));

        Person person = mock(Person.class);
        when(person.getId()).thenReturn(URI.create("http://foo"));
        ActivityPubObjectReference<Person> personRef2 = new ActivityPubObjectReference<Person>()
            .setObject(person);
        assertNotEquals(personRef1, personRef2);
        assertTrue(personRef1.concernsSameObject(personRef2));
        assertTrue(personRef2.concernsSameObject(personRef1));

        personRef2 = new ActivityPubObjectReference<Person>()
            .setLink(URI.create("http://foo"));
        assertEquals(personRef1, personRef2);
        assertTrue(personRef1.concernsSameObject(personRef2));
        assertTrue(personRef2.concernsSameObject(personRef1));

        personRef2 = new ActivityPubObjectReference<Person>()
            .setLink(URI.create("http://bar"));
        assertNotEquals(personRef1, personRef2);
        assertFalse(personRef1.concernsSameObject(personRef2));
        assertFalse(personRef2.concernsSameObject(personRef1));

        when(person.getId()).thenReturn(URI.create("http://bar"));
        personRef2 = new ActivityPubObjectReference<Person>()
            .setObject(person);
        assertNotEquals(personRef1, personRef2);
        assertFalse(personRef1.concernsSameObject(personRef2));
        assertFalse(personRef2.concernsSameObject(personRef1));
    }
}
