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

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.stability.Unstable;

/**
 * Webfinger query operations.
 *
 * @since 1.1
 * @version $Id$
 */
@Role
@Unstable
public interface WebfingerClient
{
    /**
     * Query a webfinger endpoint with a given resource.
     * @param webfingerResource The webfinger resource.
     * @return The JSON Resource Descript of the webfinger resource.
     * @throws WebfingerException In case of error during the query.
     */
    JSONResourceDescriptor get(String webfingerResource) throws WebfingerException;

    /**
     * Test if the given domain do have a working WebFinger implementation.
     * Note that this method should only be used to test an XWiki WebFinger implementation.
     *
     * @param domain the domain on which WebFinger is supposed to be working.
     * @return {@code true} if WebFinger sent the right answer.
     * @throws WebfingerException in case of error when performing the check.
     */
    boolean testWebFingerConfiguration(String domain) throws WebfingerException;
}
