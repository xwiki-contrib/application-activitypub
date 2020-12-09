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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.DiscussionContextEntityReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.wysiwyg.converter.HTMLConverter;

/**
 * Script services for the discussions operations.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = { ActivityPubDiscussionsScriptService.class })
@Singleton
public class ActivityPubDiscussionsScriptService
{
    private static final String DESCRIPTION_STR = "description";

    private static final String EVENT_STR = "event";

    private static final String ACTIVITYPUB_ACTIVITY_STR = "activitypub-activity";

    private static final String ACTIVITYPUB_ENTITY_TYPE = "activitypub";

    @Inject
    private DiscussionService discussionService;

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private MessageService messageService;

    @Inject
    private ActivityPubObjectReferenceResolver resolver;

    @Inject
    private HTMLConverter htmlConverter;

    /**
     * Reply to an event by adding a message to a discussion.
     * <p>
     * If the discussion do not exists, or the discussion context, they are created too.
     * <p>
     *
     * @param eventId the event id
     * @param activityId the activity id
     * @param content the message content
     * @return {@code} true if the operation succeeded, {@code false} otherwise
     */
    public boolean replyToEvent(String eventId, String activityId, String content)
    {
        // First search or creates a discussion according to the discussion contexts for the event and the activity of 
        // the message.
        DiscussionContext eventDiscussionContext = new DiscussionContext(null, EVENT_STR,
            EVENT_STR, new DiscussionContextEntityReference(EVENT_STR, eventId));
        DiscussionContext activityDiscussionContext =
            new DiscussionContext(null, ACTIVITYPUB_ACTIVITY_STR, ACTIVITYPUB_ACTIVITY_STR,
                new DiscussionContextEntityReference(ACTIVITYPUB_ENTITY_TYPE, activityId));
        List<DiscussionContext> discussionContexts =
            initializeDiscussionContexts(eventDiscussionContext, activityDiscussionContext);
        String discussionTitle = String.format("Discussion for %s", activityId);
        List<String> discussionContextReferences =
            discussionContexts.stream().map(DiscussionContext::getReference).collect(Collectors.toList());
        Optional<Discussion> discussionOpt = this.discussionService.getOrCreate(discussionTitle, discussionTitle,
            discussionContextReferences);
        discussionOpt.ifPresent(discussion -> {

            // Link the actors as discussion context of discussion.
            try {
                ActivityPubObject object =
                    this.resolver.resolveReference(new ActivityPubObjectReference<>().setLink(URI.create(activityId)));
                for (ProxyActor activityPubObjectReference : object.getTo()) {
                    ActivityPubObject object1 = this.resolver.resolveReference(activityPubObjectReference);
                    this.discussionContextService
                        .getOrCreate(object1.getName(), object1.getName(), ACTIVITYPUB_ENTITY_TYPE,
                            object1.getId().toASCIIString())
                        .ifPresent(
                            discussionContext -> this.discussionContextService.link(discussionContext, discussion));
                }
            } catch (ActivityPubException e) {
                e.printStackTrace();
            }
        });

        // Finally, creates the message.
        return discussionOpt
            // TODO: take into account the syntax and check if the conversion is required
            .map(it -> this.messageService
                .create(this.htmlConverter.fromHTML(content, Syntax.XWIKI_2_1.toIdString()), it.getReference())
                .isPresent())
            .orElse(false);
    }

    private List<DiscussionContext> initializeDiscussionContexts(DiscussionContext... discussionContextsIns)
    {
        List<DiscussionContext> discussionContexts = new ArrayList<>();
        for (DiscussionContext discussionContextsIn : discussionContextsIns) {
            String name = discussionContextsIn.getName();
            String description = discussionContextsIn.getDescription();
            String type = discussionContextsIn.getEntityReference().getType();
            String reference = discussionContextsIn.getEntityReference().getReference();
            this.discussionContextService.getOrCreate(name, description, type, reference)
                .ifPresent(discussionContexts::add);
        }
        return discussionContexts;
    }
}
