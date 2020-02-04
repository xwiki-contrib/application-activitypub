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
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = OrderedCollection.class)
public class OrderedCollection extends AbstractCollection
{
    private List<ObjectReference<?>> orderedItems;

    public OrderedCollection()
    {
        this.orderedItems = new ArrayList<>();
    }

    public List<ObjectReference<?>> getOrderedItems()
    {
        return orderedItems;
    }

    public OrderedCollection setOrderedItems(List<ObjectReference<?>> orderedItems)
    {
        this.orderedItems = orderedItems;
        return this;
    }

    @Override
    public OrderedCollection addItem(Object item)
    {
        this.orderedItems.add(new ObjectReference<>().setObject(item));
        return this;
    }

    @Override
    public int getTotalItems()
    {
        return this.orderedItems.size();
    }
}
