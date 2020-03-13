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
import org.xwiki.user.UserReference;

/**
 * Handles the mapping between XWiki Users and ActivityPub actors.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
@Role
public interface ActorHandler
{
    /**
     * Retrieve the current actor based on the context.
     * @return an actor corresponding to the current XWiki User or null.
     * @throws ActivityPubException in case of errors when storing information about the user.
     */
    AbstractActor getCurrentActor() throws ActivityPubException;

    /**
     * Retrieve or create an ActivityPub actor corresponding to the given user reference.
     *
     * @param userReference the reference to an XWiki User.
     * @return an actor corresponding to the given EntityReference or null if the reference is not linked an XWiki User.
     * @throws ActivityPubException in case of error when loading the document pointed by the reference.
     */
    AbstractActor getActor(UserReference userReference) throws ActivityPubException;

    /**
     * Retrieve the XWiki User reference related to the given actor.
     * @param actor an ActivityPub actor for which to retrieve the reference.
     * @return an entity reference or null if the actor does not belong to the current wiki.
     */
    UserReference getXWikiUserReference(AbstractActor actor) throws ActivityPubException;

    /**
     * Retrieve an actor based on the serialized user reference.
     * This method internally calls {@link #getActor(UserReference)} but resolve first the given login.
     * @param login the name of an user.
     * @return an actor corresponding to the given reference or null if the reference is not linked to an XWiki User.
     * @throws ActivityPubException in case of error when loading the document pointed by the reference.
     */
    AbstractActor getLocalActor(String login) throws ActivityPubException;

    /**
     * Retrieve a remote actor based on its serialized URL.
     *
     * @param actorURL the URL of the remote actor.
     * @return an instance of the actor.
     * @throws ActivityPubException in case of error while loading and parsing the request.
     */
    AbstractActor getRemoteActor(String actorURL) throws ActivityPubException;

    /**
     * Verify if an user with the given serialized reference exist.
     * @param login the name of an user.
     * @return {@code true} if the user exists.
     */
    boolean isExistingUser(String login);

    /**
     * Check if the given actor belongs to the current instance.
     * @param actor the actor to check.
     * @return {@code true} if the given actor belongs to the current instance.
     */
    boolean isLocalActor(AbstractActor actor);
}
