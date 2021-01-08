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
package org.xwiki.contrib.activitypub.script;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
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
import org.xwiki.script.service.ScriptService;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.contrib.activitypub.ActivityPubConfiguration.ACTIVITYPUB_DISCUSSION_TYPE;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

/**
 * Script services for the discussions operations.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named("activitypub.discussions")
@Singleton
public class ActivityPubDiscussionsScriptService implements ScriptService
{
    private static final String EVENT_STR = "event";

    private static final String ACTIVITYPUB_OBJECT = "activitypub-object";

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private DiscussionService discussionService;

    @Inject
    private MessageService messageService;

    @Inject
    private ActivityPubObjectReferenceResolver resolver;

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    private Logger logger;

    /**
     * Reply to an event by adding a message to a discussion.
     * <p>
     * If the discussion do not exists, or the discussion context, they are created too.
     * <p>
     *
     * @param eventId the event id
     * @param activityId the activity id
     * @param actorId the ID of the actor sending the message
     * @param content the message content
     * @return {@code} true if the operation succeeded, {@code false} otherwise
     */
    public boolean replyToEvent(String eventId, String activityId, String actorId, String content)
    {
        // First search or creates a discussion according to the discussion contexts for the event and the activity of 
        // the message.

        try {
            AbstractActivity activityPubObject =
                (AbstractActivity) this.resolver
                    .resolveReference(new ActivityPubObjectReference<>().setLink(URI.create(activityId)));

            ActivityPubObject object = this.resolver.resolveReference(activityPubObject.getObject());

            // Replace with an optional since only one parameter.
            Optional<DiscussionContext> activityDiscussionContextIn =
                this.discussionContextService.getOrCreate(ACTIVITYPUB_OBJECT,
                    ACTIVITYPUB_OBJECT, ACTIVITYPUB_OBJECT,
                    object.getId().toASCIIString());
            Optional<DiscussionContext> eventDiscussionContext =
                this.discussionContextService.getOrCreate(EVENT_STR, EVENT_STR, EVENT_STR, eventId);

            String discussionTitle = String.format("Discussion for %s", activityId);
            Optional<Discussion> discussionOpt = activityDiscussionContextIn.map(DiscussionContext::getReference)
                .flatMap(z -> this.discussionService.getOrCreate(discussionTitle, discussionTitle, singletonList(z)));
            discussionOpt.ifPresent(discussion -> {
                activityDiscussionContextIn
                    .ifPresent(discussionContext -> this.discussionContextService.link(discussionContext, discussion));
                eventDiscussionContext
                    .ifPresent(discussionContext -> this.discussionContextService.link(discussionContext, discussion));
                // Link the actors as discussion context of discussion.
                try {

                    for (ProxyActor activityPubObjectReference : object.getTo()) {
                        linkToActor(discussion, activityPubObjectReference);
                    }

                    List<ActivityPubObjectReference<AbstractActor>> attributedToList = object.getAttributedTo();
                    if (attributedToList != null) {
                        for (ActivityPubObjectReference<AbstractActor> attributedTo : attributedToList) {
                            linkToActor(discussion, this.resolver.resolveReference(attributedTo).getProxyActor());
                        }
                    }
                } catch (ActivityPubException e) {
                    this.logger.warn("Failed to link an actor to the discussion [{}]. Cause: [{}].", discussion,
                        getRootCauseMessage(e));
                }
            });

            // Finally, creates the message.
            return discussionOpt
                // TODO: take into account the syntax and check if the conversion is required
                .map(it -> this.messageService
                    .create(this.htmlConverter.fromHTML(content, XWIKI_2_1.toIdString()), XWIKI_2_1,
                        it.getReference(), ACTIVITYPUB_DISCUSSION_TYPE, actorId)
                    .isPresent())
                .orElse(false);
        } catch (ActivityPubException e) {
            this.logger.warn("Failed to add a message content = [{}], "
                    + "activityId = [{}], eventId = [{}] to a discussion. Cause: [{}].", content, activityId, eventId,
                getRootCauseMessage(e));
            return false;
        }
    }

    private void linkToActor(Discussion discussion, ProxyActor activityPubObjectReference) throws ActivityPubException
    {
        ActivityPubObject object = this.resolver.resolveReference(activityPubObjectReference);
        this.discussionContextService
            .getOrCreate(object.getName(), object.getName(), "activitypub-actor",
                object.getId().toASCIIString())
            .ifPresent(
                discussionContext -> this.discussionContextService.link(discussionContext, discussion));
    }
}
