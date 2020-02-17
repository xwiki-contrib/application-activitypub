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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an Ordered Collection as defined by ActivityStream.
 * Note that internally the storage is managed by a {@link List}.
 *
 * @param <T> the type of {@link ActivityPubObject} to store in the collection.
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-orderedcollection">ActivityStream Collection
 * definition</a>
 */
@JsonDeserialize(as = OrderedCollection.class)
public class OrderedCollection<T extends ActivityPubObject> extends AbstractCollection<T>
{
    private List<ActivityPubObjectReference<T>> orderedItems;

    /**
     * Default constructor to initialize the internal {@link ArrayList}.
     */
    public OrderedCollection()
    {
        this.orderedItems = new ArrayList<>();
    }

    /**
     * @return the list of references to all items.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-items">ActivityStream definition</a>
     */
    public List<ActivityPubObjectReference<T>> getOrderedItems()
    {
        return orderedItems;
    }

    /**
     * @param orderedItems the list of references to all items.
     * @return the current collection for fluent API.
     */
    public OrderedCollection<T> setOrderedItems(List<ActivityPubObjectReference<T>> orderedItems)
    {
        this.orderedItems = orderedItems;
        return this;
    }

    @Override
    public int getTotalItems()
    {
        return this.orderedItems.size();
    }

    /**
     * Wrap the item in an {@link ActivityPubObjectReference} before storing it.
     * {@inheritDoc}
     */
    @Override
    public <O extends AbstractCollection<T>> O addItem(T item)
    {
        this.orderedItems.add(new ActivityPubObjectReference<T>().setObject(item));
        return (O) this;
    }

    @Override
    public Iterator<ActivityPubObjectReference<T>> iterator()
    {
        return this.orderedItems.iterator();
    }
}
