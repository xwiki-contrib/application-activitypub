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
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = Collection.class)
public class Collection extends AbstractCollection
{
    private Set<ObjectReference<?>> items;

    public Collection()
    {
        this.items = new HashSet<>();
    }

    public Set<ObjectReference<?>> getItems()
    {
        return items;
    }

    public Collection setItems(Set<ObjectReference<?>> items)
    {
        this.items = items;
        return this;
    }

    @Override
    public int getTotalItems()
    {
        return this.items.size();
    }

    @Override
    public Collection addItem(Object item)
    {
        this.items.add(new ObjectReference<>().setObject(item));
        return this;
    }
}
