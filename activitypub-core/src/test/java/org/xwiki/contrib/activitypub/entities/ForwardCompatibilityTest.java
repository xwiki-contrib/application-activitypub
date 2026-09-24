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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Forward-compatibility tests: guard the single most common way a federation partner breaks us over time, namely by
 * adding fields or activity types we do not know yet. Parsing such payloads must degrade gracefully (unknown types fall
 * back to {@link UnknownTypeObject}, unknown fields are ignored) rather than throw.
 *
 * @version $Id$
 */
class ForwardCompatibilityTest extends AbstractEntityTest
{
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void unknownTypeFallsBackToUnknownTypeObject() throws Exception
    {
        // A partner sends an activity type that did not exist when this extension was written.
        String json = "{"
            + "\"@context\": \"https://www.w3.org/ns/activitystreams\","
            + "\"id\": \"https://remote.example/activities/1\","
            + "\"type\": \"EmojiReact\""
            + "}";

        ActivityPubObject parsed = this.parser.parse(json);

        assertTrue(parsed instanceof UnknownTypeObject,
            "Unknown type must degrade to UnknownTypeObject, got " + parsed.getClass());
        assertEquals(URI.create("https://remote.example/activities/1"), parsed.getId());
        assertEquals(1, this.logCapture.size());
        assertEquals("ActivityPub Object type [EmojiReact] not found.", this.logCapture.getMessage(0));
    }

    @Test
    void missingTypeFallsBackToUnknownTypeObject() throws Exception
    {
        String json = "{"
            + "\"@context\": \"https://www.w3.org/ns/activitystreams\","
            + "\"id\": \"https://remote.example/objects/1\""
            + "}";

        ActivityPubObject parsed = this.parser.parse(json);

        assertTrue(parsed instanceof UnknownTypeObject,
            "Missing type must degrade to UnknownTypeObject, got " + parsed.getClass());
        assertEquals(URI.create("https://remote.example/objects/1"), parsed.getId());
    }

    @Test
    void unknownFieldsOnKnownTypeAreIgnored() throws Exception
    {
        // A partner adds a brand-new property to an otherwise known Person actor; it must be ignored, not fatal.
        String json = "{"
            + "\"@context\": \"https://www.w3.org/ns/activitystreams\","
            + "\"id\": \"https://remote.example/actors/alice\","
            + "\"type\": \"Person\","
            + "\"preferredUsername\": \"alice\","
            + "\"inbox\": \"https://remote.example/actors/alice/inbox\","
            + "\"outbox\": \"https://remote.example/actors/alice/outbox\","
            + "\"aBrandNewFieldFrom2099\": {\"nested\": [\"value\"]},"
            + "\"anotherUnknownScalar\": 42"
            + "}";

        ActivityPubObject parsed = this.parser.parse(json);

        assertTrue(parsed instanceof Person, "Known type must still parse to Person, got " + parsed.getClass());
        Person person = (Person) parsed;
        assertEquals("alice", person.getPreferredUsername());
        assertEquals(URI.create("https://remote.example/actors/alice"), person.getId());
    }
}
