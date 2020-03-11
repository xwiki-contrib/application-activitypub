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

import java.io.InputStream;
import java.io.Reader;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.stability.Unstable;

/**
 * Define parsing operations for Webfinger.
 *
 * @since 1.1
 * @version $Id$
 */
@Unstable
@Role
public interface WebfingerJsonParser
{
    /**
     * Parse the given string into a {@link JSONResourceDescriptor}.
     * @param json The json representation of a {@link JSONResourceDescriptor}.
     * @return The parsed {@link JSONResourceDescriptor}.
     * @throws WebfingerException in case of error during the parsing.w
     */
    JSONResourceDescriptor parse(String json) throws WebfingerException;

    /**
     * Parse the given reader into a {@link JSONResourceDescriptor}.
     * @param reader The reader of the json representation of a {@link JSONResourceDescriptor}.
     * @return The parsed {@link JSONResourceDescriptor}.
     * @throws WebfingerException in case of error during the parsing.w
     */
    JSONResourceDescriptor parse(Reader reader) throws WebfingerException;

    /**
     * Parse the given stream into a {@link JSONResourceDescriptor}.
     * @param stream The stream of a json representation of a {@link JSONResourceDescriptor}.
     * @return The parsed {@link JSONResourceDescriptor}.
     * @throws WebfingerException in case of error during the parsing.
     */
    JSONResourceDescriptor parse(InputStream stream) throws WebfingerException;
}
