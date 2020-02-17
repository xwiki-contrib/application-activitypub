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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class InboxTest extends AbstractEntityTest
{
    @Test
    public void serialization() throws URISyntaxException, IOException
    {
        Inbox inbox = new Inbox()
            .setId(new URI("http://localhost:8080/xwiki/activitypub/Inbox/XWiki.Foo-inbox"));

        String expectedSerialization = this.readResource("inbox/inbox1.json");
        assertEquals(expectedSerialization, this.serializer.serialize(inbox));
    }

    @Test
    public void parsing() throws URISyntaxException, IOException
    {
        Inbox expectedInbox = new Inbox()
            .setId(new URI("http://localhost:8080/xwiki/activitypub/Inbox/XWiki.Foo-inbox"));

        String json = this.readResource("inbox/inbox1.json");
        assertEquals(expectedInbox, this.parser.parse(json, Inbox.class));
        assertEquals(expectedInbox, this.parser.parse(json));
    }
}
