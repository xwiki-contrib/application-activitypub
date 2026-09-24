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
 * Test of {@link Like}: a Like is a federated activity a remote server (e.g. Mastodon) can send, so the parser must
 * dispatch the {@code Like} type to it and round-trip it without loss.
 *
 * @version $Id$
 * @since 1.4
 */
public class LikeTest extends AbstractEntityTest
{
    @Test
    void serializeAndParseRoundTrip() throws Exception
    {
        Like like = new Like()
            .setActor(new ActivityPubObjectReference<AbstractActor>()
                .setLink(new URI("https://social.example/alyssa/")))
            .setObject(new ActivityPubObjectReference<Note>()
                .setLink(new URI("https://chatty.example/ben/posts/1")))
            .setId(new URI("https://social.example/alyssa/likes/1"));

        String json = this.serializer.serialize(like);
        ActivityPubObject parsed = this.parser.parse(json);

        assertTrue(parsed instanceof Like, "Type Like must be dispatched to Like, got " + parsed.getClass());
        assertEquals(like, parsed);
    }

    @Test
    void parseWithExplicitType() throws ActivityPubException
    {
        Like like = new Like().setId(URI.create("https://social.example/alyssa/likes/1"));
        assertEquals(like, this.parser.parse(this.serializer.serialize(like), Like.class));
    }
}
