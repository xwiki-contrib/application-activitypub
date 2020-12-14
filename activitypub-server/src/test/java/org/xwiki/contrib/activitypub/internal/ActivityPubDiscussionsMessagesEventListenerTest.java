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

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.events.MessageEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.xwiki.contrib.discussions.events.ActionType.*;

/**
 * Test of {@link ActivityPubDiscussionsMessagesEventListener}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ActivityPubDiscussionsMessagesEventListenerTest
{
    @InjectMockComponents
    private ActivityPubDiscussionsMessagesEventListener activityPubDiscussionsMessagesEventListener;

    @MockComponent
    private DiscussionContextService discussionContextService;

    @MockComponent
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @MockComponent
    private ActivityHandler<Create> createActivityHandler;

    @MockComponent
    private ActivityHandler<Update> updateActivityHandler;

    @MockComponent
    private ActorHandler actorHandler;

    @Test
    void getName()
    {
        assertEquals("ActivityPubDiscussionsMessagesEventListener",
            this.activityPubDiscussionsMessagesEventListener.getName());
    }

    @Test
    void getEvents()
    {
        assertEquals(asList(
            new MessageEvent(CREATE),
            new MessageEvent(UPDATE),
            new MessageEvent(DELETE)
        ), this.activityPubDiscussionsMessagesEventListener.getEvents());
    }

//    @Test
//    void onEvent()
//    {
//        Discussion discussion =
//            new Discussion("discussionReference", "discussionTitle", "discussionDescription", new Date());
//        Message message = new Message("reference", "content", "actorType", "actorReference", new Date(), new Date(),
//            discussion);
//        this.activityPubDiscussionsMessagesEventListener.onEvent(new MessageEvent(CREATE), "src", message);
//    }
}