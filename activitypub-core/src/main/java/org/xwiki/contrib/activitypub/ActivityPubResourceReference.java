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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceType;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A resource reference to be able to identify and retrieve any stored ActivityPub entity.
 * This is tightly related to {@link ActivityPubStorage}.
 *
 * @since 1.0
 * @version $Id$
 */
@Unstable
public class ActivityPubResourceReference extends AbstractResourceReference
{
    /**
     * Represents an ActivityPub Resource Type.
     */
    public static final ResourceType TYPE = new ResourceType("activitypub");

    private String entityType;
    private String uuid;

    /**
     * Default constructor.
     * @param entityType the type of the entity (e.g. Create, Note, etc)
     * @param uuid the identifier as returned by {@link ActivityPubStorage}
     */
    public ActivityPubResourceReference(String entityType, String uuid)
    {
        setType(TYPE);
        this.entityType = entityType;
        this.uuid = uuid;
    }

    /**
     * @return the entity type.
     */
    public String getEntityType()
    {
        return entityType;
    }

    /**
     * @return the unique identifier of the object in the storage.
     */
    public String getUuid()
    {
        return uuid;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("entityType", entityType)
            .append("uuid", uuid)
            .toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ActivityPubResourceReference that = (ActivityPubResourceReference) o;

        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(entityType, that.entityType)
            .append(uuid, that.uuid)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .appendSuper(super.hashCode())
            .append(entityType)
            .append(uuid)
            .toHashCode();
    }
}
