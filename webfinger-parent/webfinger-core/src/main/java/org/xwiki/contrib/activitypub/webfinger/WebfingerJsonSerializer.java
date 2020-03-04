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
package org.xwiki.contrib.activitypub.webfinger;

import java.io.IOException;
import java.io.OutputStream;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.webfinger.entities.WebfingerJRD;
import org.xwiki.stability.Unstable;

/**
 * Define serializing operations for ActivityPub.
 *
 * @since 1.1
 * @version $Id$
 */
@Unstable
@Role
public interface WebfingerJsonSerializer
{
    /**
     * Serialize a {@link WebfingerJRD} to a {@link String}.
     * @param object the {@link WebfingerJRD} to serialize. 
     * @return a string representing the serialization of the object to json.
     * @throws IOException in case of issue occurring during the serialization.
     */
    String serialize(WebfingerJRD object) throws IOException;

    /**
     * Serialize a {@link WebfingerJRD} to a {@link String}.
     * @param stream the stream where to output the serialized object.
     * @param object the {@link WebfingerJRD} to serialize.
     * @throws IOException in case of issue occurring during the serialization.
     */
    void serialize(OutputStream stream, WebfingerJRD object) throws IOException;
}
