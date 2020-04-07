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

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubNotifier;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Reject;
import org.xwiki.contrib.activitypub.events.AbstractActivityPubEvent;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.FollowEvent;
import org.xwiki.observation.ObservationManager;

/**
 * Default implementation of the notifier: basically it creates an {@link AbstractActivityPubEvent} and send it to the
 * {@link ObservationManager}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubNotifier implements ActivityPubNotifier
{
    @Inject
    private ActorHandler actorHandler;

    @Inject
    private ObservationManager observationManager;

    @Override
    public <T extends AbstractActivity> void notify(T activity, Set<AbstractActor> targetedActors)
        throws ActivityPubException
    {
        AbstractActivityPubEvent<? extends AbstractActivity> event;
        Set<String> serializedTargets = this.serializeTargets(targetedActors);
        if (activity instanceof Create) {
            event = new CreateEvent((Create) activity, serializedTargets);
        } else if (activity instanceof Follow || activity instanceof Reject || activity instanceof Accept) {
            event = new FollowEvent<>(activity, serializedTargets);
        } else {
            throw new ActivityPubException(String.format("Cannot find the right event to notify about [%s]", activity));
        }
        this.observationManager.notify(event, "org.xwiki.contrib:activitypub-notifications", activity.getType());
    }

    private Set<String> serializeTargets(Set<AbstractActor> targets) throws ActivityPubException
    {
        Set<String> result = new HashSet<>();
        for (AbstractActor target : targets) {
            if (target == null) {
                throw new ActivityPubException("You cannot send a notification to a null target.");
            }
            result.add(this.actorHandler.getNotificationTarget(target));
        }
        return result;
    }
}
