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
package org.xwiki.contrib.activitypub.internal.filters;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractCollection;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;

/**
 * A generic interface to filter any activitypub collection.
 *
 * @param <T> the type of collection to filter.
 * @since 1.2
 * @version $Id$
 */
@Role
public interface CollectionFilter<T  extends AbstractCollection<? extends ActivityPubObject>>
{
    /**
     * Filter the given collection according to some strategy.
     * The returned element can be a new collection or the same one as the argument but modified.
     *
     * @param collection the collection to filter.
     * @return a filtered collection.
     */
    T filter(T collection);
}
