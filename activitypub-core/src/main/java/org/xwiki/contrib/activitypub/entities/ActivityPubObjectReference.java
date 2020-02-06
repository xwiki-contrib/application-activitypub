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
import java.util.Objects;

import org.xwiki.contrib.activitypub.internal.json.ActivityPubObjectReferenceDeserializer;
import org.xwiki.text.XWikiToStringBuilder;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Defines a reference towards an {@link ActivityPubObject}.
 * The idea is to be able to deserialize some properties either as link or as object since most of the properties
 * are serialized in one or another type.
 * Note that it is then possible that in case of link the object is null: a specific resolver should be used to retrieve
 * (and potentially set) the proper object.
 * @param <T>
 */
@JsonDeserialize(using = ActivityPubObjectReferenceDeserializer.class)
public class ActivityPubObjectReference<T extends ActivityPubObject>
{
    private boolean isLink;
    private URI link;
    private T object;

    public boolean isLink()
    {
        return isLink;
    }

    public ActivityPubObjectReference<T> setLink(boolean link)
    {
        isLink = link;
        return this;
    }

    public T getObject()
    {
        return object;
    }

    public ActivityPubObjectReference<T> setObject(T object)
    {
        this.object = object;
        return this;
    }

    public URI getLink()
    {
        return link;
    }

    public ActivityPubObjectReference<T> setLink(URI link)
    {
        this.link = link;
        return this;
    }

    @Override
    public boolean equals(java.lang.Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActivityPubObjectReference<?> that = (ActivityPubObjectReference<?>) o;
        return isLink == that.isLink &&
            Objects.equals(link, that.link) &&
            Objects.equals(object, that.object);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(isLink, link, object);
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this).append("isLink", isLink()).append("link", getLink()).build();
    }
}
