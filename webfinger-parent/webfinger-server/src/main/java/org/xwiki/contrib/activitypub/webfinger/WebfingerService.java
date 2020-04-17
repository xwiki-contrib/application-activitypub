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
import org.xwiki.contrib.activitypub.entities.AbstractActor;
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
     * Resolve a username to an actual ActivityPub Actor.
     * The following formats are supported for username resolutioon:
     *   - {@code identifier}: the user named identifier will be looked in the current wiki
     *   - {@code identifier.otherwiki}: the user named identifier will be looked in the subwiki named otherwiki.
     *   - {@code identifier.xwiki}: the sub wiki named identifier will be looked.
     *   - {@code xwiki.xwiki}: it returns always the current wiki actor.
     * @param username The username of the actor to find.
     * @return the resolve {@link AbstractActor} corresponding of the username.
     * @throws WebfingerException in case of serialization error
     */
    AbstractActor resolveActivityPubUser(String username) throws WebfingerException;

    /**
     * Resolve the activitypub resource of the user. 
     * @param actor the actor URL to obtain.
     * @return the url of the activitypub resource of the user
     */
    URI resolveXWikiUserUrl(AbstractActor actor) throws WebfingerException;
}
