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
package org.xwiki.contrib.activitypub;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Object;
import org.xwiki.contrib.activitypub.entities.Outbox;

@Role
public interface ActivityPubStore
{
    /**
     * Store a given entity and return a UUID to retrieve it.
     *
     * @param entity the entity to persist.
     * @return an UUID to retrieve this entity.
     */
    String storeEntity(Object entity);

    /**
     * Extract an entity from its UUID.
     *
     * @param uuid the unique identifier of the entity as given by {@link #storeEntity(Object)}.
     * @param <T>
     * @return the stored entity or null if it has not been found.
     */
    <T extends Object> T retrieveEntity(String entityType, String uuid);

    Inbox retrieveActorInbox(Actor actor);

    Outbox retrieveActorOutbox(Actor actor);
}
