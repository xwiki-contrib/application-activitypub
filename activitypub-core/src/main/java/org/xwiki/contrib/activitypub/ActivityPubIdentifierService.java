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

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.stability.Unstable;

/**
 * Provides the logic to deal with the identification of user and other actors in activitypub.
 *
 * @version $Id$
 * @since 1.2
 */
@Role
@Unstable
public interface ActivityPubIdentifierService
{
    /**
     * 
     * @return the wiki separator between a wiki name and the wiki identifier.
     */
    default String getWikiSeparator()
    {
        return ".";
    }

    /**
     * 
     * @return the wiki identifier.
     */
    default String getWikiIdentifier()
    {
        return "xwiki";
    }

    /**
     * Return the activitypub identifier of the actor.
     * @param actor The actor.
     * @param username
     * @param wikiName The name of the wiki.
     * @return the identifier.
     * @throws ActivityPubException In case of errror during the identifier resolution.
     */
    String createIdentifier(AbstractActor actor, String username, String wikiName) throws ActivityPubException;
}
