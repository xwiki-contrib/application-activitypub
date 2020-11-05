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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubNotifier;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.Announce;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Mention;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.Reject;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.activitypub.events.AbstractActivityPubEvent;
import org.xwiki.contrib.activitypub.events.AnnounceEvent;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.FollowEvent;
import org.xwiki.contrib.activitypub.events.MentionEvent;
import org.xwiki.contrib.activitypub.events.MessageEvent;
import org.xwiki.contrib.activitypub.events.UpdateEvent;
import org.xwiki.observation.ObservationManager;

import static java.util.Collections.singleton;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

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
    private static final String EVENT_SOURCE = "org.xwiki.contrib:activitypub-notifications";

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private ObservationManager observationManager;

    @Inject
    private ActivityPubObjectReferenceResolver resolver;

    @Inject
    private Logger logger;

    @Override
    public <T extends AbstractActivity> void notify(T activity, Set<AbstractActor> targetedActors)
        throws ActivityPubException
    {
        for (AbstractActor targetedActor : targetedActors) {
            notify(activity, targetedActor);
        }
    }

    private <T extends AbstractActivity> void notify(T activity, AbstractActor targetedActor)
        throws ActivityPubException
    {
        ActivityPubObject activityObject = this.resolver.resolveReference(activity.getObject());
        if (activity instanceof Create) {
            notifyCreate((Create) activity, targetedActor, activityObject);
        } else if (activity instanceof Update) {
            notifyUpdate((Update) activity, targetedActor, activityObject);
        } else if (activity instanceof Announce) {
            AnnounceEvent event = new AnnounceEvent((Announce) activity, serializeTargets(singleton(targetedActor)));
            this.observationManager.notify(event, EVENT_SOURCE, event.getType());
        } else if (activity instanceof Follow || activity instanceof Reject || activity instanceof Accept) {
            FollowEvent<T> event = new FollowEvent<>(activity, serializeTargets(singleton(targetedActor)));
            this.observationManager.notify(event, EVENT_SOURCE, event.getType());
        } else {
            throw new ActivityPubException(
                String.format("Cannot find the right event to notify about [%s]", activity));
        }
    }

    /**
     * Handles the notifications for the {@link Update} activities.
     *
     * @param activity an {@link Update} activity
     * @param targetedActor the target of the activity
     * @param activityObject the resolved object of the activityt
     */
    private void notifyUpdate(Update activity,
        AbstractActor targetedActor, ActivityPubObject activityObject) throws ActivityPubException
    {
        if (isActorMentioned(targetedActor, activityObject)) {
            MentionEvent event = new MentionEvent(activity, serializeTargets(singleton(targetedActor)));
            this.observationManager.notify(event, EVENT_SOURCE, event.getType());
        }
        UpdateEvent event = new UpdateEvent(activity, serializeTargets(singleton(targetedActor)));
        this.observationManager.notify(event, EVENT_SOURCE, event.getType());
    }

    /**
     * Handles the notifications for the {@link Create} activities.
     *
     * @param activity a {@link Create} activity
     * @param targetedActor the target of the activity
     * @param activityObject the resolved object of the activity
     */
    private void notifyCreate(Create activity,
        AbstractActor targetedActor, ActivityPubObject activityObject) throws ActivityPubException
    {
        AbstractActivityPubEvent<? extends AbstractActivity> event;
        if (isActorMentioned(targetedActor, activityObject)) {
            event = new MentionEvent(activity, serializeTargets(singleton(targetedActor)));
            this.observationManager.notify(event, EVENT_SOURCE, event.getType());
        }

        if (activityObject instanceof Note) {
            event = new MessageEvent(activity, serializeTargets(singleton(targetedActor)));
            this.observationManager.notify(event, EVENT_SOURCE, event.getType());
        } else {
            event = new CreateEvent(activity, serializeTargets(singleton(targetedActor)));
            this.observationManager.notify(event, EVENT_SOURCE, event.getType());
        }
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

    /**
     * Checks of the actor is part of the mentioned actors of the object.
     *
     * @param actor the actor
     * @param object the activity pub object
     * @return {@code true} if the actor is found in the mentioned actors of the object. {@code false} otherwise
     */
    private boolean isActorMentioned(AbstractActor actor, ActivityPubObject object)
    {
        return Optional.ofNullable(object.getTag())
            .map(tags ->
                tags.stream()
                    .map(tag -> {
                        try {
                            return Optional.of(this.resolver.resolveReference(tag));
                        } catch (ActivityPubException e) {
                            this.logger.warn("Unable to resolve [{}]. Cause: [{}]", tag, getRootCauseMessage(e));
                            return Optional.<ActivityPubObject>empty();
                        }
                    })
                    .anyMatch(resolveTag -> resolveTag
                        .map(tag -> Objects.equals(tag.getType(), Mention.class.getSimpleName())
                            && Objects.equals(((Mention) tag).getHref(), actor.getId()))
                        .orElse(false)))
            .orElse(false);
    }
}
