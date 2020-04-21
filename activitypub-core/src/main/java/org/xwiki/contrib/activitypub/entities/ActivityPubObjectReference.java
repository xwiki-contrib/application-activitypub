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

import java.net.URI;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Defines a reference towards an {@link ActivityPubObject}.
 * The idea is to be able to deserialize some properties either as link or as object since most of the properties
 * are serialized in one or another type.
 * Note that it is then possible that in case of link the object is null: a specific resolver should be used to retrieve
 * (and potentially set) the proper object.
 * See {@link org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver}.
 * @param <T> the type of {@link ActivityPubObject} it refers to.
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class ActivityPubObjectReference<T extends ActivityPubObject>
{
    private boolean isLink;
    private boolean isExpand;
    private URI link;
    private T object;

    /**
     * @return {@code true} if it contains a link and {@code false} if it contains a concrete object.
     */
    public boolean isLink()
    {
        return isLink;
    }

    /**
     * @return the concrete object this refers to.
     */
    public T getObject()
    {
        return object;
    }

    /**
     * Set the concrete object of this reference.
     * This reference won't need to be resolved anymore so {@link #isLink} would then return {@code false} only if the
     * given object is not null.
     * @param object the concrete object to set.
     * @return the current reference for fluent API.
     */
    public ActivityPubObjectReference<T> setObject(T object)
    {
        this.object = object;
        this.isLink = (object == null);
        return this;
    }

    /**
     * @return the current link this refers to: if the concrete object is resolved, it returns its id.
     */
    public URI getLink()
    {
        return (!isLink() && this.object != null) ? this.object.getId() : this.link;
    }

    /**
     * Set the link of this reference.
     * Note that once this method is called, {@link #isLink} will return {@code true} for this instance.
     * @param link the link to resolve the concrete object.
     * @return the current reference for fluent API.
     */
    public ActivityPubObjectReference<T> setLink(URI link)
    {
        this.link = link;
        this.isLink = true;
        return this;
    }

    /**
     * 
     * @param link the link to the object.
     */
    public void setLink(boolean link)
    {
        this.isLink = link;
    }

    /**
     * 
     * @return the expand status of the object.
     */
    public boolean isExpand()
    {
        return this.isExpand;
    }

    /**
     * Set the expand status. If the expand status is true, the object will always be serialized to its object form
     * and not as a link.
     * @param expand the expand status.
     * @return the current ActivityPub object.
     */
    public ActivityPubObjectReference<T> setExpand(boolean expand)
    {
        this.isExpand = expand;
        return this;
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

        ActivityPubObjectReference<?> rhs = (ActivityPubObjectReference<?>) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder()
            .append(link, rhs.link)
            .append(isLink, rhs.isLink);

        // TODO: Improve this piece of code. We cannot really do an equals on the objects since it might lead to
        // stackoverflow exceptions
        if (object == null && rhs.object == null) {
            return equalsBuilder.build();
        } else if (object != null && rhs.object != null) {
            return equalsBuilder.append(object.toString(), rhs.object.toString()).build();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder()
            .append(link)
            .append(isLink);

        // TODO: Improve this piece of code. We cannot really do an hashCode on the objects since it might lead to
        // stackoverflow exceptions
        if (object != null) {
            hashCodeBuilder.append(object.toString());
        }
        return hashCodeBuilder.build();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this).append("isLink", isLink()).append("link", getLink()).build();
    }
}
