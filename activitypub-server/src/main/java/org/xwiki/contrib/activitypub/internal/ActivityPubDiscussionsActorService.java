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
package org.xwiki.contrib.activitypub.internal;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.discussions.DiscussionsActorService;
import org.xwiki.contrib.discussions.domain.ActorDescriptor;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Creates an actor descriptor from an activitypub actor ID.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Singleton
@Named("activitypub")
public class ActivityPubDiscussionsActorService implements DiscussionsActorService
{
    @Inject
    private ActorHandler actorHandler;

    @Inject
    private Logger logger;

    @Override
    public Optional<ActorDescriptor> resolve(String reference)
    {
        try {
            AbstractActor actor = this.actorHandler.getActor(reference);
            ActorDescriptor actorDescriptor = new ActorDescriptor();
            initializeName(actor, actorDescriptor);
            actorDescriptor.setLink(actor.getId());
            return Optional.of(actorDescriptor);
        } catch (ActivityPubException e) {
            this.logger.warn("Failed to revolve the actor [{}]. Cause: [{}].", reference, getRootCauseMessage(e));
            return Optional.empty();
        }
    }

    private void initializeName(AbstractActor actor, ActorDescriptor actorDescriptor)
    {
        if (actor.getPreferredUsername() != null) {
            actorDescriptor.setName(actor.getPreferredUsername());
        } else if (actor.getName() != null) {
            actorDescriptor.setName(actor.getName());
        }
    }
}
