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
import java.util.List;
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
import org.xwiki.contrib.activitypub.entities.ProxyActor;
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
        AbstractActivityPubEvent<? extends AbstractActivity> event;
        ActivityPubObject activityObject = this.resolver.resolveReference(activity.getObject());
        if (activity instanceof Create) {
            event = notifyCreate((Create) activity, targetedActor, activityObject);
        } else if (activity instanceof Update) {
            event = new UpdateEvent((Update) activity, serializeTargets(singleton(targetedActor)));
        } else if (activity instanceof Announce) {
            event = new AnnounceEvent((Announce) activity, serializeTargets(singleton(targetedActor)));
        } else if (activity instanceof Follow || activity instanceof Reject || activity instanceof Accept) {
            event = new FollowEvent<>(activity, serializeTargets(singleton(targetedActor)));
        } else {
            throw new ActivityPubException(
                String.format("Cannot find the right event to notify about [%s]", activity));
        }
        this.observationManager.notify(event, "org.xwiki.contrib:activitypub-notifications", event.getType());
    }

    /**
     * Handles the notifications for the {@link Create} activities.
     *
     * @param activity a {@link Create} activity
     * @param targetedActor the target of the activity
     * @param activityObject the resolved object of the activity
     * @return the resulting event
     */
    private AbstractActivityPubEvent<? extends AbstractActivity> notifyCreate(Create activity,
        AbstractActor targetedActor, ActivityPubObject activityObject) throws ActivityPubException
    {
        AbstractActivityPubEvent<? extends AbstractActivity> event;
        if (activityObject instanceof Note) {
            // if the actor is not a direct

            if (!isActorDirectRecipient(activityObject, targetedActor) && isActorMentioned(activityObject,
                targetedActor))
            {
                event = new MentionEvent(activity, serializeTargets(singleton(targetedActor)));
            } else {
                event = new MessageEvent(activity, serializeTargets(singleton(targetedActor)));
            }
        } else {
            event = new CreateEvent(activity, serializeTargets(singleton(targetedActor)));
        }
        return event;
    }

    /**
     * Checks if the actor is mentioned in the object.
     *
     * @param object the object
     * @param actor the actor
     * @return {@code true} if the actor is mentioned, {@code false} otherwise
     */
    private boolean isActorMentioned(ActivityPubObject object, AbstractActor actor)
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

    /**
     * Checks of the subject actor is part of the list of actors.
     *
     * @param actors the list of actors
     * @param subjectActor the subject actor
     * @return {@code true} if the subject actor is found in the list of actors, {@code false} otherwise
     */
    private boolean isActorRecipient(List<ProxyActor> actors, AbstractActor subjectActor)
    {
        return Optional.ofNullable(actors)
            .map(to -> to.stream()
                .anyMatch(it1 -> Objects.equals(it1.getLink(), subjectActor.getId())))
            .orElse(false);
    }

    /**
     * Checks if the actor is a direct recipient of the object.
     *
     * @param object the object
     * @param actor the actor
     * @return {@code true} if the actor is one of the direct recipients of the object, {code false} otherwise
     */
    private boolean isActorDirectRecipient(ActivityPubObject object, AbstractActor actor)
    {
        return isActorRecipient(object.getTo(), actor);
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
