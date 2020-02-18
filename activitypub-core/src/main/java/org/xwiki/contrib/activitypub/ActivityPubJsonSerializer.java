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

import java.io.OutputStream;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;

/**
 * Define serializing operations for ActivityPub.
 * @version $Id$
 */
@Role
public interface ActivityPubJsonSerializer
{
    /**
     * Serialize the given {@link ActivityPubObject} (or any inherited type) to a {@link String}.
     * @param object the object to serialize.
     * @param <T> the concrete type of the given object.
     * @return a string representing the serialization of the given object.
     * @throws ActivityPubException in case any issue occurred during the serialization.
     */
    <T extends ActivityPubObject> String serialize(T object) throws ActivityPubException;

    /**
     * Serialize the given {@link ActivityPubObject} (or any inherited type) to the output stream.
     * @param stream the stream where to output the serialized object.
     * @param object the object to serialize.
     * @param <T> the concrete type of the given object.
     * @throws ActivityPubException in case any issue occurred during the serialization.
     */
    <T extends ActivityPubObject> void serialize(OutputStream stream, T object) throws ActivityPubException;
}
