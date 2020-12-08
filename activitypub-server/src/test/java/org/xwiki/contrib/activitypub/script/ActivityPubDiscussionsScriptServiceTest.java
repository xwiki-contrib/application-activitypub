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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ActivityPubDiscussionsScriptService}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ActivityPubDiscussionsScriptServiceTest
{
    @InjectMockComponents
    private ActivityPubDiscussionsScriptService activityPubDiscussionsScriptService;

    @MockComponent
    private DiscussionService discussionService;

    @MockComponent
    private DiscussionContextService discussionContextService;

    @MockComponent
    private MessageService messageService;

    @Test
    void replyToEvent() throws Exception
    {
        Map<String, Object> parameters = new HashMap<>();
        Map<Object, Object> discussionContext1 = new HashMap<>();
        discussionContext1.put("name", "discussionContext1Name");
        discussionContext1.put("description", "discussionContext1Description");
        discussionContext1.put("referenceType", "discussionContext1Type");
        discussionContext1.put("entityReference", "discussionContext1Ref");
        Map<Object, Object> discussionContext2 = new HashMap<>();
        discussionContext2.put("name", "discussionContext2Name");
        discussionContext2.put("description", "discussionContext2Description");
        discussionContext2.put("referenceType", "discussionContext2Type");
        discussionContext2.put("entityReference", "discussionContext2Ref");
        List<Object> discussionContexts = asList(discussionContext1, discussionContext2);

        Map<Object, Object> discussion = new HashMap<>();
        discussion.put("title", "discussionTitle");
        discussion.put("description", "discussionDescription");

        parameters.put("discussionContexts", discussionContexts);
        parameters.put("discussion", discussion);
        parameters.put("content", "messageContent");

        DiscussionContext dc1 = new DiscussionContext("dc1", "discussionContext1Name",
            "discussionContext1Description",
            "discussionContext1Type",
            "discussionContext1Ref");
        when(this.discussionContextService
            .getOrCreate("discussionContext1Name", "discussionContext1Description", "discussionContext1Type",
                "discussionContext1Ref"))
            .thenReturn(Optional.of(dc1));
        DiscussionContext dc2 = new DiscussionContext("dc2", "discussionContext2Name",
            "discussionContext2Description",
            "discussionContext2Type",
            "discussionContext2Ref");
        when(this.discussionContextService
            .getOrCreate("discussionContext2Name", "discussionContext2Description", "discussionContext2Type",
                "discussionContext2Ref"))
            .thenReturn(Optional.of(dc2));

        Discussion d = new Discussion("d1", "discussionTitle", "discussionDescription");
        when(this.discussionService.getOrCreate("discussionTitle", "discussionDescription",
            asList(dc1.getReference(), dc2.getReference())))
            .thenReturn(Optional.of(d));

        when(this.messageService.create("messageContent", d.getReference())).thenReturn(
            Optional.of(new Message("mr", "messageContent", "user", "actorRef", new Date(), new Date(), d)));

        boolean b = this.activityPubDiscussionsScriptService.replyToEvent(parameters);
        assertTrue(b);
    }

    @Test
    void replyToEventDiscussionContextsMissing()
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("wrong", null);

        ActivityPubException activityPubException = assertThrows(ActivityPubException.class,
            () -> this.activityPubDiscussionsScriptService.replyToEvent(parameters));
        assertEquals("Parameter [discussionContexts] missing.", activityPubException.getMessage());
    }

    @Test
    void replyToEventDiscussionMissing()
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("discussionContexts", Arrays.asList());
        parameters.put("wrong", new HashMap<>());

        ActivityPubException activityPubException = assertThrows(ActivityPubException.class,
            () -> this.activityPubDiscussionsScriptService.replyToEvent(parameters));
        assertEquals("Parameter [discussion] missing.", activityPubException.getMessage());
    }
    
    @Test
    void replyToEventContextMissing()
    {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("discussionContexts", Arrays.asList());
        parameters.put("discussion", new HashMap<>());

        ActivityPubException activityPubException = assertThrows(ActivityPubException.class,
            () -> this.activityPubDiscussionsScriptService.replyToEvent(parameters));
        assertEquals("Parameter [content] missing.", activityPubException.getMessage());
    }
}