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

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an ActivityPub object with an unknown type. It has the same field as an ActivityPub object but its type is
 * set as Unknown.
 *
 * @version $Id$
 * @since 1.7.1
 */
@Unstable
@JsonDeserialize(as = UnknownTypeObject.class)
public class UnknownTypeObject extends ActivityPubObject
{
    private String unknownType;

    @Override
    public <T extends ActivityPubObject> T setType(String type)
    {
        this.unknownType = type;
        return (T) this;
    }

    @Override
    public String getType()
    {
        return "Unknown";
    }

    /**
     * The declared type of the object. This value can be used to know what is the actual type of an
     * deserialized activitypub object even if its type have not been found.
     *
     * @return the actual type of the object
     */
    public String getActualType()
    {
        return this.unknownType;
    }
}
