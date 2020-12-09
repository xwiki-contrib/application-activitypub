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
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.DiscussionContextEntityReference;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

    @MockComponent
    private ActivityPubObjectReferenceResolver resolver;

    @MockComponent
    private HTMLConverter htmlConverter;


    @Test
    void replyToEvent() throws Exception
    {
        DiscussionContext dc1 = new DiscussionContext("dc1", "discussionContext1Name",
            "discussionContext1Description",
            new DiscussionContextEntityReference("discussionContext1Type",
                "discussionContext1Ref"));
        DiscussionContext dc2 = new DiscussionContext("dc2", "discussionContext2Name",
            "discussionContext2Description",
            new DiscussionContextEntityReference("discussionContext2Type",
                "discussionContext2Ref"));
        ActivityPubObject mock = mock(ActivityPubObject.class);
        ProxyActor mock1 = mock(ProxyActor.class);

        when(this.discussionContextService.getOrCreate("event", "event", "event", "discussionContext1Ref"))
            .thenReturn(Optional.of(dc1));
        when(this.discussionContextService
            .getOrCreate("activitypub-activity", "activitypub-activity", "activitypub",
                "discussionContext2Ref"))
            .thenReturn(Optional.of(dc2));

        Discussion d = new Discussion("d1", "discussionTitle", "discussionDescription", new Date());
        when(this.discussionService
            .getOrCreate("Discussion for discussionContext2Ref", "Discussion for discussionContext2Ref",
                asList(dc1.getReference(), dc2.getReference())))
            .thenReturn(Optional.of(d));

        when(this.resolver.resolveReference(any())).thenReturn(mock);
        when(mock.getTo()).thenReturn(singletonList(mock1));
        ActivityPubObject mock2 = mock(ActivityPubObject.class);
        when(this.resolver.resolveReference(mock1)).thenReturn(mock2);
        when(mock2.getId()).thenReturn(URI.create("http://servr/path"));
        

        when(this.messageService.create("messageContent", d.getReference())).thenReturn(
            Optional.of(new Message("mr", "messageContent", "user", "actorRef", new Date(), new Date(), d)));

        when(this.htmlConverter.fromHTML(eq("messageContent"), any())).thenReturn("messageContent");

        boolean b = this.activityPubDiscussionsScriptService
            .replyToEvent("discussionContext1Ref", "discussionContext2Ref", "messageContent");
        assertTrue(b);
    }
}