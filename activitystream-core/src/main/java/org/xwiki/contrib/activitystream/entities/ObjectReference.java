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
package org.xwiki.contrib.activitystream.entities;

import java.net.URI;
import java.util.Objects;

import org.xwiki.contrib.activitystream.tools.ObjectReferenceDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@JsonDeserialize(using = ObjectReferenceDeserializer.class)
public class ObjectReference<T extends Object>
{
    private boolean isLink;
    private URI link;
    private T object;

    public boolean isLink()
    {
        return isLink;
    }

    public void setLink(boolean link)
    {
        isLink = link;
    }

    public T getObject()
    {
        return object;
    }

    public void setObject(T object)
    {
        this.object = object;
    }

    public URI getLink()
    {
        return link;
    }

    public void setLink(URI link)
    {
        this.link = link;
    }

    public void resolveObject()
    {
        throw new NotImplementedException();
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
        ObjectReference<?> that = (ObjectReference<?>) o;
        return isLink == that.isLink &&
            Objects.equals(link, that.link) &&
            Objects.equals(object, that.object);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(isLink, link, object);
    }
}
