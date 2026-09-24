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
import org.xwiki.contrib.activitypub.ActivityPubException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link Update}: an Update is a federated activity a remote server (e.g. Mastodon) can send, so the parser
 * must dispatch the {@code Update} type to it and round-trip it without loss.
 *
 * @version $Id$
 * @since 1.2
 */
public class UpdateTest extends AbstractEntityTest
{
    @Test
    void serializeAndParseRoundTrip() throws Exception
    {
        Update update = new Update()
            .setActor(new ActivityPubObjectReference<AbstractActor>()
                .setLink(new URI("https://social.example/alyssa/")))
            .setObject(new ActivityPubObjectReference<Note>()
                .setLink(new URI("https://social.example/alyssa/posts/1")))
            .setId(new URI("https://social.example/alyssa/updates/1"));

        String json = this.serializer.serialize(update);
        ActivityPubObject parsed = this.parser.parse(json);

        assertTrue(parsed instanceof Update, "Type Update must be dispatched to Update, got " + parsed.getClass());
        assertEquals(update, parsed);
    }

    @Test
    void parseWithExplicitType() throws ActivityPubException
    {
        Update update = new Update().setId(URI.create("https://social.example/alyssa/updates/1"));
        assertEquals(update, this.parser.parse(this.serializer.serialize(update), Update.class));
    }
}
