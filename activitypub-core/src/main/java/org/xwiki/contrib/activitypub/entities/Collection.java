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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an Unordered Collection as defined by ActivityStream.
 * Note that it is internally managed by a {@link Set}.
 * @param <T> the type of {@link ActivityPubObject} to store.
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-collection">ActivityStream definition</a>
 */
@JsonDeserialize(as = Collection.class)
public class Collection<T extends ActivityPubObject> extends AbstractCollection<T>
{
    private Set<ActivityPubObjectReference<T>> items;

    /**
     * Default constructor which initialize the internal representation of the collection with a {@link HashSet}.
     */
    public Collection()
    {
        this.items = new HashSet<>();
    }

    /**
     * @return references to all items of the collection.
     */
    public Set<ActivityPubObjectReference<T>> getItems()
    {
        return items;
    }

    /**
     * @param items references to all items of the collection.
     * @param <O> the type of the collection.
     * @return the current instance for fluent API.
     */
    public <O extends Collection<T>> O setItems(Set<ActivityPubObjectReference<T>> items)
    {
        this.items = items;
        return (O) this;
    }

    @Override
    public int getTotalItems()
    {
        return this.items.size();
    }

    /**
     * Wrap the item in an {@link ActivityPubObjectReference} before storing it.
     * {@inheritDoc}
     */
    @Override
    public <O extends AbstractCollection<T>> O addItem(T item)
    {
        this.items.add(new ActivityPubObjectReference<T>().setObject(item));
        return (O) this;
    }

    @Override
    public Iterator<ActivityPubObjectReference<T>> iterator()
    {
        return this.items.iterator();
    }
}
