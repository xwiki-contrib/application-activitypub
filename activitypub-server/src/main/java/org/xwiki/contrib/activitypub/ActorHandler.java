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
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.model.reference.EntityReference;

@Role
public interface ActorHandler
{
    Actor getCurrentActor() throws ActivityPubException;

    Actor getActor(EntityReference entityReference) throws ActivityPubException;

    EntityReference getXWikiUserReference(Actor actor);

    Actor getLocalActor(String serializedUserReference) throws ActivityPubException;

    Actor getRemoteActor(String actorURL) throws ActivityPubException;

    Inbox getInbox(Actor actor) throws ActivityPubException;

    Outbox getOutbox(Actor actor) throws ActivityPubException;

    boolean isExistingUser(String serializedUserReference);
}
