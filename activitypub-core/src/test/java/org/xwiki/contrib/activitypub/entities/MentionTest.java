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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link Mention}.
 *
 * @version $Id$
 * @since 1.4
 */
class MentionTest extends AbstractEntityTest
{
    private static final String MENTION_ID = "http://localhost:8080/xwiki/activitypub/mention/1";

    @Test
    void serialization() throws Exception
    {
        Mention inbox = new Mention().setId(new URI(MENTION_ID));

        String expectedSerialization = this.readResource("mention/mention1.json");
        assertEquals(expectedSerialization, this.serializer.serialize(inbox));
    }

    @Test
    void parsing() throws Exception
    {
        Mention expectedInbox = new Mention().setId(new URI(MENTION_ID));

        String json = this.readResource("mention/mention1.json");
        assertEquals(expectedInbox, this.parser.parse(json, Mention.class));
        assertEquals(expectedInbox, this.parser.parse(json));
    }
}