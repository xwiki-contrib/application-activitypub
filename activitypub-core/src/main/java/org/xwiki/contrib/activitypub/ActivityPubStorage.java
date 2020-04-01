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
     * Check if the current storage is ready to be used.
     * @return {@code true} if the storage is ready.
     * @since 1.1
     */
    @Unstable
    boolean isStorageReady();

    /**
     * Check that the given URI is part of the current instance.
     * @param id the URI to check.
     * @return {@code true} if it belongs to the current instance.
     */
    boolean belongsToCurrentInstance(URI id);

    /**
     * Store a given entity and return an ID to retrieve it.
     * This method performs the following checks:
     *   1. if the entity already has an id, this id will be used for the storing.
     *   2. if the entity does not have an id, and is an Inbox or an Outbox,
     *   we expect it to have an attributedTo attribute, so we can link it to an actor,
     *   we throw an exception if it's not the case.
     *
     * @param entity the entity to persist.
     * @return the URI corresponding to the genered or existing ID of this entity.
     * @throws ActivityPubException in case one of the check failed or the storing failed for some reason.
     */
    URI storeEntity(ActivityPubObject entity) throws ActivityPubException;

    /**
     * Extract an entity from its UUID.
     *
     * @param id the unique identifier of the entity as given by {@link #storeEntity(ActivityPubObject)}.
     * @param <T> the concrete type of the entity to retrieve.
     * @return the stored entity or null if it has not been found.
     * @throws ActivityPubException if the parsing of the entity failed.
     */
    <T extends ActivityPubObject> T retrieveEntity(URI id) throws ActivityPubException;
}
