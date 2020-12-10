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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.Message;

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
    private static final String ACTIVITYPUB_ACTIVITY = "activitypub-activity";

    private static final String ACTIVITYPUB_ACTOR = "activitypub-actor";

    private static final String ACTIVITYPUB_OBJECT = "activitypub-object";

    @Inject
    private MessageService messageService;

    @Inject
    private DiscussionService discussionService;

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private ActivityPubObjectReferenceResolver resolver;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    /**
     * Find or create a discussion for the activity, linked at least to the provided list of discussion contexts.
     *
     * @param activityId the activity id
     * @param discussionContexts the list of discussion contexts
     * @return the discussion
     */
    public Optional<Discussion> getOrCreateDiscussion(String activityId, List<DiscussionContext> discussionContexts)
    {
        List<String> discussionContextReferences =
            discussionContexts.stream().map(DiscussionContext::getReference).collect(Collectors.toList());
        String discussionTitle = String.format("Discussion for %s", activityId);
        Optional<Discussion> discussionOpt = this.discussionService.getOrCreate(discussionTitle, discussionTitle,
            discussionContextReferences);
        // probably useless
//        discussionOpt.ifPresent(discussion ->
//            discussionContexts
//                .forEach(discussionContext -> this.discussionContextService.link(discussionContext, discussion)));
        return discussionOpt;
    }

    /**
     * Links an activity to a discussion. The actor of the activity, as well as the objects related to the activity are
     * linked too.
     *
     * @param activityId the id of the activity to link to the discussion
     * @param discussion the discussion to link to the activity
     */
    public void linkActivityToDiscussion(String activityId, Discussion discussion)
    {
        try {
            ActivityPubObject object =
                this.resolver.resolveReference(new ActivityPubObjectReference<>().setLink(
                    URI.create(activityId)));
            for (ProxyActor activityPubObjectReference : object.getTo()) {
                linkToActor(discussion, activityPubObjectReference);
            }

            if (object instanceof AbstractActivity) {
                AbstractActivity abstractActivity = (AbstractActivity) object;
                List<ActivityPubObjectReference<AbstractActor>> attributedTo =
                    this.resolver.resolveReference(abstractActivity.getObject()).getAttributedTo();
                if (attributedTo != null) {
                    for (ActivityPubObjectReference<AbstractActor> a : attributedTo) {
                        linkToActor(discussion, this.resolver.resolveReference(a).getProxyActor());
                    }
                }
                if (abstractActivity.getObject() != null) {
                    linkToObject(discussion, this.resolver.resolveReference(abstractActivity.getObject()));
                }
            } else {
                linkToObject(discussion, object);
            }
            if (object.getAttributedTo() != null) {
                for (ActivityPubObjectReference<AbstractActor> a : object.getAttributedTo()) {
                    linkToActor(discussion, a.getObject().getProxyActor());
                }
            }
        } catch (ActivityPubException e) {
            e.printStackTrace();
        }
    }

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
     * Load the list of activitypub objects linked together by in-reply-to values. The first element of the reply chain
     * is the object of the activity passed in parameter.
     *
     * @param activityId the id of the activity to analyze
     * @return the list objects of the discussion
     */
    public List<ActivityPubObject> loadReplyChain(String activityId)
    {
        try {
            ActivityPubObject object =
                this.resolver.resolveReference(new ActivityPubObjectReference<>().setLink(URI.create(activityId)));
            return loadReplyChain(object.getReference());
        } catch (ActivityPubException e) {
            e.printStackTrace();
            return Arrays.asList();
        }
    }

    /**
     * Checks if the activity is not already part of a discussion, and if not, created the discussions entities required
     * to add the message of the activity to a discussion.
     *
     * @param activity the activity to handle
     */
    public void handleActivity(AbstractActivity activity)
    {
        boolean notAlreadyHandled = this.discussionService
            .findByDiscussionContext(ACTIVITYPUB_OBJECT, activity.getObject().getLink().toASCIIString());
        if (!notAlreadyHandled) {
            try {

                ActivityPubObjectReference<ActivityPubObject> reference = activity.getReference();
                ActivityPubObject object = this.activityPubObjectReferenceResolver.resolveReference(reference);
                List<ActivityPubObject> replyChain = loadReplyChain(reference);
                // The list of discussions that involves at least one of the message of the reply chain.
                getOrCreateDiscussions(activity, replyChain).forEach(discussion -> {
                    // TODO : link the actors and objects to the discussion.
                    // TODO : make sure to avoid unwanted events propagations, or to add a stopping condition on AP
                    // new events listener.
                    // TODO: check that the context is initialized with an actor which has the rights
                    // or simply allow to skip them.
                    // or make sure that the current actors is guest, AP actors should not be allowed to participate
                    // to private conversations.
//                    this.messageService
//                        .create(object.getContent(), discussion.getReference(), "activitypub", target.get(0));
                    String authorId = activity.getActor().getLink().toASCIIString();
                    createMessage(discussion, object.getContent(), "activitypub", authorId);
                });
            } catch (ActivityPubException e) {
                e.printStackTrace();
            }
        }
    }

    private void linkToObject(Discussion discussion, ActivityPubObject activityPubObject)
    {
        this.discussionContextService
            .getOrCreate(activityPubObject.getName(), activityPubObject.getName(), ACTIVITYPUB_OBJECT,
                activityPubObject.getId().toASCIIString())
            .ifPresent(dc -> this.discussionContextService.link(dc, discussion));
    }

    private void linkToActor(Discussion discussion, ProxyActor activityPubObjectReference) throws ActivityPubException
    {
        ActivityPubObject object = this.resolver.resolveReference(activityPubObjectReference);
        this.discussionContextService
            .getOrCreate(object.getName(), object.getName(), ACTIVITYPUB_ACTOR, object.getId().toASCIIString())
            .ifPresent(discussionContext -> this.discussionContextService.link(discussionContext, discussion));
    }

    /**
     * For each discussion context passed in parameter, find if it already exists and creates it otherwise?
     *
     * @param discussionContexts the discussion contexts to get of create
     * @return the initialized list of discussion contexts
     */
    public List<DiscussionContext> initializeDiscussionContexts(DiscussionContext... discussionContexts)
    {
        return Arrays.stream(discussionContexts)
            .map(this::getOrCreateDiscussionContext)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * Find or create a discussion context.
     *
     * @param discussionContext the discussion context to find or create
     * @return the discussion context, possibly new
     */
    public Optional<DiscussionContext> getOrCreateDiscussionContext(DiscussionContext discussionContext)
    {
        String name = discussionContext.getName();
        String description = discussionContext.getDescription();
        String type = discussionContext.getEntityReference().getType();
        String reference = discussionContext.getEntityReference().getReference();
        return this.discussionContextService.getOrCreate(name, description, type, reference);
    }

    /**
     * Creates a message in a discussion, with the current user as the author.
     *
     * @param discussion the discussion of the message
     * @param content the content of the message
     * @return the created message
     */
    public Optional<Message> createMessage(Discussion discussion, String content)
    {
        return this.messageService.create(content, discussion.getReference());
    }

    /**
     * Create a message in a discussion, for the actor type and reference passed in parameter.
     *
     * @param discussion the discussion of the message
     * @param content the content of the message
     * @param actorType the actor type
     * @param actorReference the actor reference
     * @return the created message
     */
    public Optional<Message> createMessage(Discussion discussion, String content, String actorType,
        String actorReference)
    {
        return this.messageService.create(content, discussion.getReference(), actorType, actorReference);
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
            .findByEntityReference(ACTIVITYPUB_ACTOR, replyChain.get(0).getId().toASCIIString(), null,
                null).stream())
            .distinct().collect(Collectors.toList());
        if (discussions.isEmpty()) {
            String title = "Discussion for " + activity.getId();
            discussions = this.discussionService.create(title, title)
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
        ActivityPubObject object = this.activityPubObjectReferenceResolver.resolveReference(reference);
        ArrayList<ActivityPubObject> replyChain = new ArrayList<>();
        replyChain.add(object);

        while (object.getInReplyTo() != null) {
            URI inReplyTo = object.getInReplyTo();
            object = this.activityPubObjectReferenceResolver
                .resolveReference(new ActivityPubObjectReference<>().setLink(inReplyTo));
            replyChain.add(object);
        }
        return replyChain;
    }
}
