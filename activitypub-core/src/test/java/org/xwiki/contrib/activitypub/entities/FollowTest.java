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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test of {@link Follow}.
 *
 * @since 1.0
 * @version $Id$
 */
public class FollowTest extends AbstractEntityTest
{
    @Test
    void serializationWithReferences() throws URISyntaxException, IOException, ActivityPubException
    {
        Follow follow = this.initFollow();
        String expectedJson = this.readResource("follow/follow1.json");
        assertEquals(expectedJson, this.serializer.serialize(follow));
    }

    @Test
    void serializationWithObjects() throws URISyntaxException, IOException, ActivityPubException
    {
        Follow follow = this.initFollow();
        String expectedJson = this.readResource("follow/follow1.json");
        assertEquals(expectedJson, this.serializer.serialize(follow));
    }

    @Test
    void parsing() throws FileNotFoundException, URISyntaxException, ActivityPubException
    {
        Follow expectedFollow = this.initFollow();
        String json = this.readResource("follow/follow1.json");
        assertEquals(expectedFollow, this.parser.parse(json));
    }

    @Test
    void parsingNextcloud() throws Exception
    {
        String json = this.readResource("follow/nextcloud.json");
        ActivityPubObjectReference<AbstractActor> apr = new ActivityPubObjectReference<AbstractActor>()
                                                            .setLink(new URI(
                                                                "http://nextcloud.home/apps/social/@admin"));
        ActivityPubObjectReference<ActivityPubObject> object =
            new ActivityPubObjectReference<>().setLink(new URI("http://xwiki.home/xwiki/activitypub/Person/XWiki.Admin"));
        ActivityPubObject follow = new Follow()
                                       .setActor(apr)
                                       .setObject(object)
                                       .setId(new URI("http://nextcloud.home/fe4c8b2e-415a-4e68-b769-718e01ba71f7"));
        assertEquals(follow, this.parser.parse(json));
    }

    /**
     * Init a standard follow object.
     * @return the initialized follow object.
     * @throws URISyntaxException Thrown when a URI is created with an invalid input.
     */
    private Follow initFollow() throws URISyntaxException
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(951185225201L);

        return new Follow()
                   .setActor(
                       new ActivityPubObjectReference<AbstractActor>()
                           .setLink(new URI("https://social.example/alyssa/"))
                   )
                   .setObject(
                       new ActivityPubObjectReference<Note>()
                           .setLink(new URI(
                               "https://social.example/alyssa/posts/49e2d03d-b53a-4c4c-a95c-94a6abf45a19"))
                   )
                   .setId(new URI("https://social.example/alyssa/posts/a29a6843-9feb-4c74-a7f7-081b9c9201d3"))
                   .setTo(Collections.singletonList(
                       new ActivityPubObjectReference<AbstractActor>()
                           .setLink(new URI("https://chatty.example/ben/"))
                   ))
                   .setContent("test content")
                   .setPublished(cal.getTime())
                   .setAttributedTo(Arrays.asList(
                       new ActivityPubObjectReference<AbstractActor>().setLink(URI.create("http://test/person/1"))))
                   .setUrl(Arrays.asList(URI.create("http://myurl")));
    }
}
