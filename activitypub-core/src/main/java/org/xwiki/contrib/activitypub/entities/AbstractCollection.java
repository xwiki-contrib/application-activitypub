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

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract type to represent ordered and unordered collection as defined by ActivityStream.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/#collections">ActivityStream explanation
 * about Collections</a>
 * @param <I> the type of item contained in the collection to be able to iterate on them.
 * @version $Id$
 * @since 1.0
 */
@Unstable
public abstract class AbstractCollection<I extends ActivityPubObject>
    extends ActivityPubObject
    implements Iterable<ActivityPubObjectReference<I>>
{
    /**
     * @return the number of items contained in the collection.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-totalitems">ActivityStream definition</a>
     */
    public abstract int getTotalItems();

    /**
     * @return all elements in this collection.
     * @since 1.1
     */
    @JsonIgnore
    public abstract java.util.Collection<ActivityPubObjectReference<I>> getAllItems();

    /**
     * Put the item in the collection.
     * It depends on the implementation of the collection to chose where to put it.
     * @param item the item to add.
     * @param <T> the type of collection to actually return.
     * @return the current collection.
     */
    public abstract <T extends AbstractCollection<I>> T addItem(I item);

    /**
     * @return {@code true} if {@link #getTotalItems()} equals 0.
     */
    @JsonIgnore
    public boolean isEmpty()
    {
        return getTotalItems() == 0;
    }

    /**
     * This is a helper method which should only be used for {@link AbstractCollection<AbstractActor>}.
     *
     * @return the proxy actor for the current collection.
     * @since 1.1
     */
    @JsonIgnore
    public ProxyActor getProxyActor()
    {
        return new ProxyActor(this.getReference().getLink());
    }
}
