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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.DiscussionContext;

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

    @Inject
    private DiscussionService discussionService;

    @Inject
    private DiscussionContextService discussionContextService;

    @Inject
    private MessageService messageService;

    /**
     * Reply to an event by adding a message to a discussion.
     * <p>
     * If the discussion do not exists, or the discussion context, they are created too.
     * <p>
     * Parameters is a map with three keys:
     * <ul>
     *     <li>discussionContexts: a list of maps with four keys: name, description, referenceType, entityReference</li>
     *     <li>discussion: a map with two keys: title and description</li>
     *     <li>content: the content of the message</li>
     * </ul>
     *
     * @param parameters a map of key values easily usable with Velocity
     * @return {@code} true if the operation succeeded, {@code false} otherwise
     */
    public boolean replyToEvent(Map<String, Object> parameters) throws ActivityPubException
    {
        List<Object> discussionContextsParamList = (List<Object>) parameters.get("discussionContexts");
        Map<String, String> discussionMap = (Map<String, String>) parameters.get("discussion");
        String content = (String) parameters.get("content");

        checkParameters(discussionContextsParamList, discussionMap, content);

        List<DiscussionContext> discussionContexts =
            initializeDiscussionContexts(discussionContextsParamList);
        return this.discussionService
            .getOrCreate(discussionMap.get("title"), discussionMap.get(DESCRIPTION_STR),
                discussionContexts.stream().map(DiscussionContext::getReference).collect(Collectors.toList()))
            .map(it -> this.messageService.create(content, it.getReference()).isPresent())
            .orElse(false);
    }

    private void checkParameters(List<Object> discussionContextsParamList, Map<String, String> discussionMap,
        String content)
        throws ActivityPubException
    {
        if (discussionContextsParamList == null) {
            throw new ActivityPubException("Parameter [discussionContexts] missing.");
        }

        if (discussionMap == null) {
            throw new ActivityPubException("Parameter [discussion] missing.");
        }

        if (content == null) {
            throw new ActivityPubException("Parameter [content] missing.");
        }
    }

    private List<DiscussionContext> initializeDiscussionContexts(List<Object> discussionContextsParamList)
    {
        List<DiscussionContext> discussionContexts = new ArrayList<>();
        for (Object discussionContextParam : discussionContextsParamList) {
            Map<String, String> discussionContext = (Map<String, String>) discussionContextParam;
            this.discussionContextService
                .getOrCreate(discussionContext.get("name"), discussionContext.get(DESCRIPTION_STR),
                    discussionContext.get("referenceType"), discussionContext.get("entityReference"))
                .ifPresent(discussionContexts::add);
        }
        return discussionContexts;
    }
}
