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

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.DiscussionsRightService;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.contrib.activitypub.ActivityPubConfiguration.ACTIVITYPUB_DISCUSSION_TYPE;
import static org.xwiki.rendering.syntax.Syntax.XHTML_1_0;

/**
 * Services to interact with the discussions from ActivityPub.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = { ActivityPubDiscussionsService.class })
@Singleton
public class ActivityPubDiscussionsService
{
    private static final String ACTIVITYPUB_ACTOR = "activitypub-actor";

    private static final String ACTIVITYPUB_OBJECT = "activitypub-object";

    @Inject
    private MessageService messageService;

    @Inject
    private DiscussionService discussionService;

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    private DiscussionsRightService discussionsRightService;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private Logger logger;

    /**
     * Links a discussion and a discussion context.
     *
     * @param discussionContext the discussion context
     * @param discussion the discussion
     */
    public void link(DiscussionContext discussionContext, Discussion discussion)
    {
        this.discussionContextService.link(discussionContext, discussion);
    }

    /**
     * Checks if the activity is not already part of a discussion, and if not, creates the discussions entities required
     * to add the message of the activity to a discussion.
     *
     * @param activity the activity to handle
     * @throws ActivityPubException if something unexpected happens during activity resolution
     */
    public void handleActivity(AbstractActivity activity) throws ActivityPubException
    {
        handleActivity(activity, true);
    }

    /**
     * Checks if the activity is not already part of a discussion, and if not, creates the discussions entities required
     * to add the message of the activity to a discussion.
     *
     * @param activity the activity to handle
     * @param notify if {@code true} a notification will be sent when the message is created, if {@code false} no
     *     notification is sent for the message creation
     * @throws ActivityPubException if something unexpected happens during activity resolution
     */
    public void handleActivity(AbstractActivity activity, boolean notify) throws ActivityPubException
    {
        boolean notAlreadyHandled = this.discussionService
            .findByDiscussionContext(ACTIVITYPUB_OBJECT, activity.getObject().getLink().toASCIIString());
        ActivityPubObject object = this.activityPubObjectReferenceResolver.resolveReference(activity.getObject());
        if (!notAlreadyHandled && object.getType().equals(Note.class.getSimpleName())) {
            processActivity(activity, notify);
        }
    }

    private void processActivity(AbstractActivity activity, boolean notify)
    {
        try {
            ActivityPubObjectReference<ActivityPubObject> reference = activity.getReference();
            ActivityPubObject activityObject = this.activityPubObjectReferenceResolver.resolveReference(reference);
            List<ActivityPubObject> replyChain = loadReplyChain(reference);
            // The list of discussions that involves at least one of the message of the reply chain.
            getOrCreateDiscussions(activity, replyChain).forEach(discussion -> {
                replyChain.forEach(replyChainObject -> {
                    String objectID = replyChainObject.getId().toASCIIString();
                    this.discussionContextService.getOrCreate(objectID, objectID, ACTIVITYPUB_OBJECT, objectID)
                        .ifPresent(ctx -> this.discussionContextService.link(ctx, discussion));

                    registerActors(discussion, replyChainObject);
                });
                String authorId = activity.getActor().getLink().toASCIIString();
                ActivityPubObjectReference<? extends ActivityPubObject> activityPubObjectReference =
                    ((AbstractActivity) activityObject).getObject();
                try {
                    ActivityPubObject object =
                        this.activityPubObjectReferenceResolver
                            .resolveReference(activityPubObjectReference);
                    createMessage(discussion, object.getContent(), XHTML_1_0, ACTIVITYPUB_DISCUSSION_TYPE, authorId,
                        notify);
                } catch (ActivityPubException e) {
                    this.logger.warn("Failed to resolve [{}]. Cause: [{}].", activityPubObjectReference,
                        getRootCauseMessage(e));
                }
            });
        } catch (ActivityPubException e) {
            this.logger.warn("Failed to process the activity [{}]. Cause: [{}]", activity, getRootCauseMessage(e));
        }
    }

    /**
     * Registers the actors related to an activitypub object in a discussion.
     *
     * @param discussion the discussion
     * @param object the activitypub object
     */
    public void registerActors(Discussion discussion, ActivityPubObject object)
    {
        if (object.getTo() != null) {
            object.getTo().forEach(it -> handleTo(discussion, it));
        }

        List<ActivityPubObjectReference<AbstractActor>> attributedTo = object.getAttributedTo();
        if (attributedTo != null) {
            attributedTo.forEach(it -> handleTo(discussion, it));
        }
    }

    private <T extends ActivityPubObject> void handleTo(Discussion discussion, ActivityPubObjectReference<T> it)
    {
        try {
            T activityPubObject = this.activityPubObjectReferenceResolver.resolveReference(it);
            String actorId = activityPubObject.getId().toASCIIString();
            this.discussionContextService.getOrCreate(actorId, actorId, ACTIVITYPUB_ACTOR, actorId)
                .ifPresent(ctx -> this.discussionContextService.link(ctx, discussion));
            // TODO: handle the cases where the target is "public" or "followers"
            if (activityPubObject instanceof AbstractActor) {
                AbstractActor actor = (AbstractActor) activityPubObject;
                if (this.actorHandler.isLocalActor(actor)) {
                    DocumentReference storeDocument = this.actorHandler.getStoreDocument(actor);
                    this.discussionsRightService.setRead(discussion, storeDocument);
                    this.discussionsRightService.setWrite(discussion, storeDocument);
                }
            }
        } catch (ActivityPubException e) {
            this.logger.warn("Failed to resolve the reference for [{}]. Cause: [{}].", it, getRootCauseMessage(e));
        }
    }

    /**
     * Create a message in a discussion, for the actor type and reference passed in parameter.
     *
     * @param discussion the discussion of the message
     * @param content the content of the message
     * @param syntax the syntax of the content of the message
     * @param actorType the actor type
     * @param actorReference the actor reference
     * @return the created message
     */
    public Optional<Message> createMessage(Discussion discussion, String content,
        Syntax syntax, String actorType,
        String actorReference)
    {
        return createMessage(discussion, content, syntax, actorType, actorReference, true);
    }

    private Optional<Message> createMessage(Discussion discussion, String content,
        Syntax syntax, String actorType,
        String actorReference, boolean notify)
    {
        return this.messageService
            .create(content, syntax, discussion.getReference(), actorType, actorReference, notify);
    }

    /**
     * Search for a discussion involving at least one of the activity of the reply chain. If no existing discussion is
     * found, a new one is created with the whole reply chain.
     *
     * @param activity the activity
     * @param replyChain the reply chain of the activity
     * @return the found or created list of discussions
     */
    public List<Discussion> getOrCreateDiscussions(AbstractActivity activity, List<ActivityPubObject> replyChain)
    {
        List<Discussion> discussions = replyChain.stream().flatMap(it -> this.discussionService
            .findByEntityReferences(ACTIVITYPUB_OBJECT, Arrays.asList(it.getId().toASCIIString()), null,
                null).stream())
            .distinct().collect(Collectors.toList());
        if (discussions.isEmpty()) {
            String title;
            if (activity.getSummary() != null) {
                title = activity.getSummary();
            } else {
                Date lastUpdated = activity.getLastUpdated();
                if (lastUpdated == null) {
                    lastUpdated = new Date();
                }
                String dateFormat = new SimpleDateFormat("MMMM d, yyyy 'at' HH:mm").format(lastUpdated);
                title = "Discussion for the " + activity.getType() + " activity of " + dateFormat;
            }

            discussions = this.discussionService.create(title, title, "ActivityPub.Discussion")
                .map(Arrays::asList)
                .orElseGet(Arrays::asList);
        }
        return discussions;
    }

    /**
     * Load the reply chain of the ActivityPub passed in parameter.
     *
     * @param reference the ActivityPub object reference
     * @return the list of objects of the chain
     * @throws ActivityPubException in case of error during the chain construction
     */
    public List<ActivityPubObject> loadReplyChain(ActivityPubObjectReference<ActivityPubObject> reference)
        throws ActivityPubException
    {
        ActivityPubObject activityObject = this.activityPubObjectReferenceResolver.resolveReference(reference);
        ArrayList<ActivityPubObject> replyChain = new ArrayList<>();
        if (activityObject instanceof AbstractActivity) {
            AbstractActivity abstractActivity = (AbstractActivity) activityObject;

            ActivityPubObject object =
                this.activityPubObjectReferenceResolver.resolveReference(abstractActivity.getObject());
            replyChain.add(object);

            try {
                while (object.getInReplyTo() != null) {
                    URI inReplyTo = object.getInReplyTo();
                    object = this.activityPubObjectReferenceResolver
                        .resolveReference(new ActivityPubObjectReference<>().setLink(inReplyTo));
                    replyChain.add(object);
                }
            } catch (ActivityPubException e) {
                this.logger.debug(
                    "Stop getting the messages of the chain since one element [{}] cannot be resolved. Cause: [{}].",
                    object.getInReplyTo(), getRootCauseMessage(e));
            }
        }
        return replyChain;
    }
}
