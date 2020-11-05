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
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link Note}.
 *
 * @since 1.0
 * @version $Id$
 */
class NoteTest extends AbstractEntityTest
{
    @Test
    void serialization() throws Exception
    {
        Note note = initializeNote();

        String expectedSerialization = this.readResource("note/note1.json");
        assertEquals(expectedSerialization, this.serializer.serialize(note));
    }

    @Test
    void parsing() throws Exception
    {
        Note expectedNote = initializeNote();

        ActivityPubObjectReference<AbstractActivity> a = new ActivityPubObjectReference<>();
        a.setLink(URI.create("http://test/create/1"));

        String json = this.readResource("note/note3.json");
        Note actual0 = this.parser.parse(json, Note.class);
        assertEquals(expectedNote, actual0);
        ActivityPubObject actual1 = this.parser.parse(json);
        assertEquals(expectedNote, actual1);
    }

    private Note initializeNote() throws URISyntaxException
    {
        return new Note()
            .setId(new URI("http://localhost:8080/xwiki/activitypub/Note/XWiki.Foo-note"))
            .setTag(singletonList(new ActivityPubObjectReference<>().setObject(
                new Mention().setId(URI.create("https://localhost:8080/xwiki/activitypub/Mentions/mentionid")))));
    }
}
