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

import java.net.URI;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.stability.Unstable;

/**
 * A storage for ActivityPub persistency.
 *
 * @since 1.0
 * @version $Id$
 */
@Unstable
@Role
public interface ActivityPubStorage
{
    /**
     * Store a given entity and return a UUID to retrieve it.
     * This method performs the following checks:
     *   1. if the entity already has an UUID, then we check that it's already stored in the current instance and we
     *   override previous record if it's the case, else we throw an exception.
     *   2. if the entity is an Inbox or an Outbox, we expect it to have an attributedTo attribute, so we can link it
     *   to an actor, we throw an exception if it's not the case.
     *
     * @param entity the entity to persist.
     * @return an UUID to retrieve this entity.
     * @throws ActivityPubException in case one of the check failed or the storing failed for some reason.
     */
    String storeEntity(ActivityPubObject entity) throws ActivityPubException;

    /**
     * Store an entity with a given UID.
     *
     * @param uid the ID to use to store the entity.
     * @param entity the entity to store.
     * @return {@code true} iff the entity has been overridden.
     * @throws ActivityPubException in case the storing failed (e.g. because of a serialization issue)
     */
    boolean storeEntity(String uid, ActivityPubObject entity) throws ActivityPubException;

    /**
     * This method resolve the given URI to identify the UID of the entity and
     * calls {@link #storeEntity(String, ActivityPubObject)} in order to store it.
     * @param uri the URI to resolve
     * @param entity the entity to persist
     * @return {@code true} iff the entity has been overridden
     * @throws ActivityPubException in case the URI has not been properly resolved or if the storing failed.
     */
    boolean storeEntity(URI uri, ActivityPubObject entity) throws ActivityPubException;

    /**
     * Extract an entity from its UUID.
     *
     * @param uuid the unique identifier of the entity as given by {@link #storeEntity(ActivityPubObject)}.
     * @param <T> the concrete type of the entity to retrieve.
     * @return the stored entity or null if it has not been found.
     * @throws ActivityPubException if the parsing of the entity failed.
     */
    <T extends ActivityPubObject> T retrieveEntity(String uuid) throws ActivityPubException;
}
