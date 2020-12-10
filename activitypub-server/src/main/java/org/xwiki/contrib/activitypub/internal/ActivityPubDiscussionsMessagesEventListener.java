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

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.events.ActionType;
import org.xwiki.contrib.discussions.events.MessageEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.contrib.activitypub.internal.ActivityPubDiscussionsMessagesEventListener.TYPE;

/**
 * Event listener for the discussions messages.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(TYPE)
@Singleton
public class ActivityPubDiscussionsMessagesEventListener implements EventListener
{
    /**
     * Type of the component.
     */
    public static final String TYPE = "ActivityPubDiscussionsMessagesEventListener";

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    private ActivityHandler<Create> createActivityHandler;

    @Inject
    private ActivityHandler<Update> updateActivityHandler;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return TYPE;
    }

    @Override
    public List<Event> getEvents()
    {
        return asList(
            new MessageEvent(ActionType.CREATE),
            new MessageEvent(ActionType.UPDATE),
            new MessageEvent(ActionType.DELETE)
        );
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof MessageEvent) {
            MessageEvent messageEvent = (MessageEvent) event;
            Message message = (Message) data;
            ActionType actionType = messageEvent.getActionType();
            if (actionType == ActionType.CREATE || actionType == ActionType.UPDATE) {
                List<DiscussionContext> discussionContexts =
                    this.discussionContextService.findByDiscussionReference(message.getDiscussion().getReference());

                try {
                    AbstractActor actor = this.actorHandler.getActor(message.getActorReference());
                    List<ProxyActor> to = getRelatedActors(discussionContexts);

                    List<AbstractActivity> collect =
                        getRelatedActivities(discussionContexts);

                    /*
                     If some activities are found, we send the note multiple times, to each activity, with the 
                     in-reply-to filled with the activity id.
                     Otherwise, we send the note only once
                    */
                    if (collect.isEmpty()) {
                        sendMessage(message, actionType, actor, to, null);
                    } else {
                        for (AbstractActivity it : collect) {
                            sendMessage(message, actionType, actor, to, it);
                        }
                    }
                } catch (ActivityPubException e) {
                    e.printStackTrace();
                }
            } else {
                // TODO: handle delete messages
                this.logger.debug("Delete event are not handled currently.");
            }
        }
    }

    private void sendMessage(Message message, ActionType actionType, AbstractActor actor, List<ProxyActor> to,
        AbstractActivity it)
    {
        try {
            getActivityHandler(actionType)
                .handleOutboxRequest(new ActivityRequest<>(actor, getActivity(actionType)
                    .<AbstractActivity>setTo(to)
                    .setActor(actor)
                    .<AbstractActivity>setPublished(message.getUpdateDate())
                    .setObject(new Note()
                        .<Note>setContent(message.getContent())
                        .setPublished(message.getUpdateDate())
                        .setInReplyTo(it.getObject().getLink())
                        .setTo(to))));
        } catch (IOException | ActivityPubException e) {
            this.logger.warn("Failed to send the message [{}]. Cause: [{}].", message, getRootCauseMessage(e));
        }
    }

    private List<ProxyActor> getRelatedActors(List<DiscussionContext> discussionContexts)
    {
        return discussionContexts.stream()
            .filter(it -> it.getEntityReference().getType().equals("activitypub-actor"))
            .flatMap(it -> {
                String reference = it.getEntityReference().getReference();
                try {
                    return Stream.of(
                        this.actorHandler.getActor(reference).getProxyActor());
                } catch (ActivityPubException | ClassCastException e) {
                    this.logger.warn("Failed to resolve actor with reference [{}]. Cause: [{}].", reference,
                        getRootCauseMessage(e));
                    return Stream.empty();
                }
            }).collect(Collectors.toList());
    }

    private List<AbstractActivity> getRelatedActivities(List<DiscussionContext> discussionContexts)
    {
        return discussionContexts.stream()
            .filter(it -> it.getEntityReference().getType().equals("activitypub-activity")).flatMap(it -> {
                try {
                    return Stream.of(this.activityPubObjectReferenceResolver.resolveReference(
                        new ActivityPubObjectReference<>()
                            .setLink(URI.create(it.getEntityReference().getReference()))));
                } catch (ActivityPubException e) {
                    e.printStackTrace();
                    return Stream.empty();
                }
            })
            .filter(it -> it instanceof AbstractActivity)
            .map(it -> (AbstractActivity) it)
            .collect(Collectors.toList());
    }

    private ActivityHandler<? extends AbstractActivity> getActivityHandler(ActionType actionType)
    {
        if (actionType == ActionType.CREATE) {
            return this.createActivityHandler;
        } else {
            return this.updateActivityHandler;
        }
    }

    private AbstractActivity getActivity(ActionType actionType)
    {
        if (actionType == ActionType.CREATE) {
            return new Create();
        } else {
            return new Update();
        }
    }
}
