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
import java.util.Comparator;
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
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
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

    private static final String ACTIVITYPUB_OBJECT = "activitypub-object";

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
    private ActivityPubXDOMService activityPubXDOMService;

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

                // Handles only messages with activitypub discussion contexts.
                if (discussionContexts.stream()
                    .anyMatch(it -> it != null && it.getName() != null && it.getEntityReference().getType()
                        .startsWith("activitypub-")))
                {

                    handleMessage(event, message, actionType, discussionContexts);
                }
            } else {
                // TODO: handle delete messages
                this.logger.debug("Delete event are not handled currently.");
            }
        }
    }

    private void handleMessage(Event event, Message message, ActionType actionType,
        List<DiscussionContext> discussionContexts)
    {
        try {
            String actorReference = message.getActorReference();
            if (actorReference.startsWith("xwiki:")) {
                actorReference = actorReference.substring(6);
            }
            AbstractActor actor = this.actorHandler.getActor(actorReference);
            List<ProxyActor> to = getExternalRelatedActors(discussionContexts);
            to.remove(actor.getProxyActor());

            List<ActivityPubObject> objects = getRelatedObjects(discussionContexts);

            /*
             If some activities are found, we send the note multiple times, to each activity, with the
             in-reply-to filled with the activity id.
             Otherwise, we send the note only once
            */
            if (objects.isEmpty()) {
                sendMessage(message, actionType, actor, to, null);
            } else {
                objects.stream()
                    .max(Comparator.comparing(ActivityPubObject::getLastUpdated))
                    .ifPresent(itx -> sendMessage(message, actionType, actor, to, itx));
            }
        } catch (ActivityPubException e) {
            this.logger
                .warn("Failed to share the message [{}] from event [{}] with the fediverse. Cause: [{}].",
                    message, event, getRootCauseMessage(e));
        }
    }

    private void sendMessage(Message message, ActionType actionType, AbstractActor actor, List<ProxyActor> to,
        ActivityPubObject object)
    {

        this.activityPubXDOMService.convertToHTML(message.getContent(), message.getSyntax()).
            ifPresent(messageContent -> {
                ActivityPubObject note = new Note()
                    .<Note>setContent(messageContent)
                    .setPublished(message.getUpdateDate());

                // TODO: decide what to do: in mastodon if a Note in linked to a Follow object for instance, it get
                //  dropped when received, so the sender is sending a message but the recipient will never be able to
                // read it.
                // But without a reply-to field filled, the message is received without any context with is not really 
                // great.
                if (object != null && object.getType().equals(Note.class.getSimpleName())) {
                    note = note.setInReplyTo(object.getId());
                }
                try {
                    getActivityHandler(actionType)
                        .handleOutboxRequest(new ActivityRequest<>(actor, getActivity(actionType)
                            .<AbstractActivity>setTo(to)
                            .setActor(actor)
                            .<AbstractActivity>setPublished(message.getUpdateDate())
                            .setObject(note.setTo(to))));

                    // register the created note in the discussion 
                    String name = note.getId().toASCIIString();
                    this.discussionContextService.getOrCreate(name, name, ACTIVITYPUB_OBJECT, name)
                        .ifPresent(it -> this.discussionContextService.link(it, message.getDiscussion()));
                } catch (IOException | ActivityPubException e) {
                    this.logger.warn("Failed to send the message [{}]. Cause: [{}].", message, getRootCauseMessage(e));
                }
            });
    }

    private List<ProxyActor> getExternalRelatedActors(List<DiscussionContext> discussionContexts)
    {
        return discussionContexts.stream()
            .filter(it -> it.getEntityReference().getType().equals("activitypub-actor"))
            .flatMap(it -> {
                String reference = it.getEntityReference().getReference();
                try {
                    return Stream.of(
                        this.actorHandler.getActor(reference));
                } catch (ActivityPubException | ClassCastException e) {
                    this.logger.warn("Failed to resolve actor with reference [{}]. Cause: [{}].", reference,
                        getRootCauseMessage(e));
                    return Stream.empty();
                }
            })
            .filter(it -> !this.actorHandler.isLocalActor(it))
            .map(AbstractActor::getProxyActor)
            .collect(Collectors.toList());
    }

    private List<ActivityPubObject> getRelatedObjects(List<DiscussionContext> discussionContexts)
    {
        return discussionContexts.stream()
            .filter(it -> it.getEntityReference().getType().equals(ACTIVITYPUB_OBJECT)).flatMap(it -> {
                try {
                    return Stream.of(this.activityPubObjectReferenceResolver.resolveReference(
                        new ActivityPubObjectReference<>()
                            .setLink(URI.create(it.getEntityReference().getReference()))));
                } catch (ActivityPubException e) {
                    this.logger.warn("Failed to resolve [{}]. Cause: [{}].", it, getRootCauseMessage(e));
                    return Stream.empty();
                }
            })
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
