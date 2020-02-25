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

import java.io.InputStream;
import java.io.Reader;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.stability.Unstable;

/**
 * Defines parsing operations for ActivityPub.
 *
 * @since 1.0
 * @version $Id$
 */
@Unstable
@Role
public interface ActivityPubJsonParser
{
    /**
     * Parse the given JSON to an {@link ActivityPubObject} or one of its inherited type.
     * Note that no check is performed in advance on the JSON: it is directly used for parsing. Any error that might
     * occur will be wrapped in an {@link ActivityPubException}.
     * @param json a complete JSON representation of an ActivityStream or ActivityPub element.
     * @param <T> the type of object to retrieve
     * @return a POJO corresponding to the given JSON
     * @throws ActivityPubException in case of exception during the parsing.
     */
    <T extends ActivityPubObject> T parse(String json) throws ActivityPubException;

    /**
     * Parse the given JSON to the given type.
     * Note that no check is performed in advance on the JSON: it is directly used for parsing. Any error that might
     * occur will be wrapped in an {@link ActivityPubException}.
     * @param json a complete JSON representation of an ActivityStream or ActivityPub element.
     * @param type the expected return type
     * @param <T> the type of object to retrieve
     * @return a POJO of the given type corresponding to the given JSON
     * @throws ActivityPubException in case of exception during the parsing.
     */
    <T extends ActivityPubObject> T parse(String json, Class<T> type) throws ActivityPubException;

    /**
     * Parse the given JSON to an {@link ActivityPubObject} or one of its inherited type.
     * Note that no check is performed in advance on the JSON: it is directly used for parsing. Any error that might
     * occur will be wrapped in an {@link ActivityPubException}.
     * @param jsonReader a {@link Reader} to the complete JSON representation of an ActivityStream or ActivityPub
     * element.
     * @param <T> the type of object to retrieve
     * @return a POJO corresponding to the given JSON
     * @throws ActivityPubException in case of exception during the parsing.
     */
    <T extends ActivityPubObject> T parse(Reader jsonReader) throws ActivityPubException;

    /**
     * Parse the given JSON to the given type.
     * Note that no check is performed in advance on the JSON: it is directly used for parsing. Any error that might
     * occur will be wrapped in an {@link ActivityPubException}.
     * @param jsonReader a {@link Reader} to the complete JSON representation of an ActivityStream or ActivityPub
     * element.
     * @param type the expected return type
     * @param <T> the type of object to retrieve
     * @return a POJO of the given type corresponding to the given JSON
     * @throws ActivityPubException in case of exception during the parsing.
     */
    <T extends ActivityPubObject> T parse(Reader jsonReader, Class<T> type) throws ActivityPubException;

    /**
     * Parse the given JSON to an {@link ActivityPubObject} or one of its inherited type.
     * Note that no check is performed in advance on the JSON: it is directly used for parsing. Any error that might
     * occur will be wrapped in an {@link ActivityPubException}.
     * @param jsonInput a {@link InputStream} to the complete JSON representation of an ActivityStream or ActivityPub
     * element.
     * @param <T> the type of object to retrieve
     * @return a POJO corresponding to the given JSON
     * @throws ActivityPubException in case of exception during the parsing.
     */
    <T extends ActivityPubObject> T parse(InputStream jsonInput) throws ActivityPubException;

    /**
     * Parse the given JSON to the given type.
     * Note that no check is performed in advance on the JSON: it is directly used for parsing. Any error that might
     * occur will be wrapped in an {@link ActivityPubException}.
     * @param jsonInput a {@link InputStream} to the complete JSON representation of an ActivityStream or ActivityPub
     * element.
     * @param type the expected return type
     * @param <T> the type of object to retrieve
     * @return a POJO of the given type corresponding to the given JSON
     * @throws ActivityPubException in case of exception during the parsing.
     */
    <T extends ActivityPubObject> T parse(InputStream jsonInput, Class<T> type)
        throws ActivityPubException;
}
