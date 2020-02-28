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
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.internal.json.DefaultActivityPubJsonParser;
import org.xwiki.contrib.activitypub.internal.json.DefaultActivityPubJsonSerializer;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link Collection}.
 *
 * @since 1.0
 * @version $Id$
 */
public class CollectionTest extends AbstractEntityTest
{
    @InjectMockComponents
    protected DefaultActivityPubJsonParser parser;

    @InjectMockComponents
    protected DefaultActivityPubJsonSerializer serializer;

    @Test
    void collectionEmpty()
    {
        Collection<Follow> collection = new Collection<>();
        assertTrue(collection.getItems().isEmpty());
        assertEquals(0, collection.getTotalItems());
    }

    @Test
    void collectionAddActivityPubObjects()
    {
        Follow item = new Follow();
        Accept item1 = new Accept();
        Collection<ActivityPubObject> collection = new Collection<>()
                                                       .addItem(item)
                                                       .addItem(item1);

        assertEquals(2, collection.getTotalItems());
        Set<ActivityPubObjectReference<ActivityPubObject>> items = collection.getItems();
        assertEquals(2, items.size());
        assertEquals(1, items.stream().filter(x -> x.getObject() == item).count());
        assertEquals(1, items.stream().filter(x -> x.getObject() == item1).count());
    }

    @Test
    void collectionAddActivityPubReference()
    {
        ActivityPubObjectReference<AbstractActivity> item =
            new ActivityPubObjectReference<AbstractActivity>().setObject(new Follow());
        ActivityPubObjectReference<AbstractActivity> item1 =
            new ActivityPubObjectReference<AbstractActivity>().setObject(new Accept());
        HashSet<ActivityPubObjectReference<AbstractActivity>> items1 = new HashSet<>();
        items1.add(item);
        items1.add(item1);
        Collection<AbstractActivity> collection = new Collection<AbstractActivity>().setItems(items1);

        assertEquals(2, collection.getTotalItems());
        Set<ActivityPubObjectReference<AbstractActivity>> items = collection.getItems();
        assertEquals(2, items.size());
        assertTrue(items.contains(item));
        assertTrue(items.contains(item1));
    }

    @Test
    void serialization() throws Exception
    {
        Collection<ActivityPubObject> collection = new Collection<>()
                                                       .addItem(new Follow().setId(URI.create("http://test/follow/1")))
                                                       .addItem(new Accept().setId(URI.create("http://test/accept/2")));

        String expectedSerialization = this.readResource("collection/collection1.json");
        String serialize = this.serializer.serialize(collection);
            assertEquals(expectedSerialization, serialize);
    }

    @Test
    void parsing() throws Exception
    {

        // FIXME items pas bien parsés...
        Collection<ActivityPubObject> collection = new Collection<>();
        HashSet<ActivityPubObjectReference<ActivityPubObject>> items = new HashSet<>();
        items.add(new ActivityPubObjectReference<>().setLink(URI.create("http://test/follow/1")));
        items.add(new ActivityPubObjectReference<>().setLink(URI.create("http://test/accept/2")));
        collection.setItems(items);

        String json = this.readResource("collection/collection1.json");
        assertEquals(collection, this.parser.parse(json, Collection.class));
        assertEquals(collection, this.parser.parse(json));
    }
}
