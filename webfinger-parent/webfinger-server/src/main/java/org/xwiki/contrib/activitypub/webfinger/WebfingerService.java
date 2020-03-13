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

import java.net.URI;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 *
 * Defines the business operations for {@link org.xwiki.contrib.activitypub.webfinger.internal.WebfingerResourceReferenceHandler}.
 * @since 1.1
 * @version $Id$
 */
@Role
@Unstable
public interface WebfingerService
{
    /**
     * Resolve to user of the profile page of the user.
     * @param username The username of the user.
     * @return the resolve {@link URI} of the username.
     * @throws WebfingerException in case of serialization error
     */
    URI resolveActivityPubUserUrl(String username) throws WebfingerException;

    /**
     * Resolve the activitypub resource of the user. 
     * @param user the user reference.
     * @return the url of the activitypub resource of the user
     */
    String resolveXWikiUserUrl(UserReference user) throws WebfingerException;
}
