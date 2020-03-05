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
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.stability.Unstable;

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
     * Converts an username to a {@link DocumentReference}.
     *
     * @param username the username.
     * @return the document reference.
     */
    DocumentReference resolveUser(String username);

    /**
     * Checks if a document correspond to an user of the wiki.
     * @param documentReference the document reference.
     * @return true if the document reference corresponds to a user of the wiki.
     */
    boolean isExistingUser(DocumentReference documentReference);

    /**
     * Checks if the username correspond to an existing user of the wiki.
     * @param username the username.
     * @return true if the username correspond to an user of the wiki.
     */
    boolean isExistingUser(String username);

    /**
     * Resolve to user of the profile page of the user.
     * @param username The username of the user.
     * @return the resolve {@link URI} of the username.
     * @throws SerializeResourceReferenceException in case pf serialization error
     * @throws UnsupportedResourceReferenceException in case of unsupported resource
     */
    URI resolveActivityPubUserUrl(String username) throws SerializeResourceReferenceException,
                                                          UnsupportedResourceReferenceException;

    /**
     * Resolve the activitypub resource of the user. 
     * @param user the user document reference.
     * @return the url of the activitypub resource of the user
     * @throws Exception in case of error.
     */
    String resolveXWikiUserUrl(DocumentReference user) throws Exception;
}
