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
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
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
     * Storage field used to store ID of the documents.
     */
    String ID_FIELD = "id";
    /**
     * Storage field used to store the type of the documents.
     */
    String TYPE_FIELD = "type";
    /**
     * Storage field used to store the serialized content of the documents.
     */
    String CONTENT_FIELD = "content";
    /**
     * Storage field used to store the XWiki Reference of the documents.
     */
    String XWIKI_REFERENCE_FIELD = "xwikiReference";
    /**
     * Storage field used to store the Updated Date of the documents.
     */
    String UPDATED_DATE_FIELD = "updatedDate";
    /**
     * Storage field used to store the authors of an entity.
     * (retrieved from {@link ActivityPubObject#getAttributedTo()})
     */
    String AUTHORS_FIELD = "authors";
    /**
     * Storage field used to store the targets of an entity.
     * (retrieved from {@link ActivityPubObject#getComputedTargets()})
     */
    String TARGETED_FIELD = "targeted";
    /**
     * Storage field used to store the information if the entity is public or not.
     * (retrieved from {@link ActivityPubObject#isPublic()})
     */
    String IS_PUBLIC_FIELD = "isPublic";

    /**
     * Check if the current storage is ready to be used.
     * @return {@code true} if the storage is ready.
     * @since 1.1
     */
    @Unstable
    boolean isStorageReady();

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

    /**
     * Store information about WebFinger.
     *
     * @param jsonResourceDescriptor a representation of a WebFinger record.
     * @throws ActivityPubException in case of problem during the storage.
     * @since 1.2
     */
    @Unstable
    void storeWebFinger(JSONResourceDescriptor jsonResourceDescriptor) throws ActivityPubException;

    /**
     * Perform a search in the DB for WebFinger records.
     * Only the WebFinger identifiers will be used to perform the search.
     *
     * @param query the string to look for.
     * @param limit the maximum number of results to return.
     * @return a list of WebFinger records matching the query.
     * @throws ActivityPubException in case of problem when performing the query.
     * @since 1.2
     */
    @Unstable
    List<JSONResourceDescriptor> searchWebFinger(String query, int limit) throws ActivityPubException;

    /**
     * Allow to perform a query of type T in the DB.
     *
     * @param type the type of element to retrieve: only concrete types should be used.
     * @param query a SolR query to find an element.
     * @param limit the limit number of result to get.
     * @param <T> the concrete type of element to get.
     * @return a list of stored elements matching the query
     * @throws ActivityPubException in case of problem during the query.
     * @since 1.2
     */
    @Unstable
    <T extends ActivityPubObject> List<T> query(Class<T> type, String query, int limit) throws ActivityPubException;

    /**
     * Escaping utility for parts of the queries to be performed.
     *
     * @param queryElement part of the query to be escaped
     * @return an escaped string.
     * @since 1.5
     */
    @Unstable
    default String escapeQueryChars(String queryElement)
    {
        return queryElement;
    }
}
