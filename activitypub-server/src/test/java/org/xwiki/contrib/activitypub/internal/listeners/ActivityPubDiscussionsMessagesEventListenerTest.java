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
package org.xwiki.contrib.activitypub.internal.listeners;

import java.net.URI;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.activitypub.internal.ActivityPubXDOMService;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.DiscussionContextEntityReference;
import org.xwiki.contrib.discussions.domain.Message;
import org.xwiki.contrib.discussions.domain.MessageContent;
import org.xwiki.contrib.discussions.events.MessageEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.discussions.events.ActionType.CREATE;
import static org.xwiki.contrib.discussions.events.ActionType.DELETE;
import static org.xwiki.contrib.discussions.events.ActionType.UPDATE;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_1;

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

    @MockComponent
    private ActivityPubXDOMService activityPubXDOMService;

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

    @Test
    void onEvent() throws Exception
    {
        Discussion discussion =
            new Discussion("discussionReference", "discussionTitle", "discussionDescription", new Date(), null);
        Message message =
            new Message("reference", new MessageContent("content", XWIKI_2_1), "actorType", "actorReference",
                new Date(), new Date(), discussion);
        URI link = URI.create("http://server/newnoteid");

        DiscussionContext dc1 =
            new DiscussionContext("dc1", "dc1", "dc1",
                new DiscussionContextEntityReference("activitypub-actor", "https://server/actor"));
        DiscussionContext dc2 =
            new DiscussionContext("dc2", "dc2", "dc2",
                new DiscussionContextEntityReference("activitypub-object", "https://server/object"));
        DiscussionContext dc3 = new DiscussionContext("dc3", link.toASCIIString(), link.toASCIIString(),
            new DiscussionContextEntityReference("activitypub-object", link.toASCIIString()));
        Person emiter = new Person()
            .setId(URI.create("https://server/emiter"));
        Person actor = new Person()
            .setId(URI.create("https://server/actor"));

        when(this.discussionContextService.findByDiscussionReference("discussionReference"))
            .thenReturn(asList(dc1, dc2));
        when(this.actorHandler.getActor("actorReference")).thenReturn(emiter);
        when(this.actorHandler.getActor("https://server/actor")).thenReturn(actor);
        Note existingNote = new Note()
            .setId(URI.create("https://server/object"));
        when(this.activityPubObjectReferenceResolver
            .resolveReference(new ActivityPubObjectReference<>().setLink(URI.create("https://server/object"))))
            .thenReturn(existingNote);
        when(this.discussionContextService
            .getOrCreate(link.toASCIIString(), link.toASCIIString(), "activitypub-object", link.toASCIIString()))
            .thenReturn(Optional.of(dc3));

        doAnswer(invocation -> {
            ActivityRequest argument = invocation.getArgument(0);
            argument.getActivity().getObject().getObject().setId(link);
            return null;
        }).when(this.createActivityHandler).handleOutboxRequest(any());

        when(this.activityPubXDOMService.convertToHTML("content", XWIKI_2_1))
            .thenReturn(Optional.of("<h1>content</h1>"));

        this.activityPubDiscussionsMessagesEventListener.onEvent(new MessageEvent(CREATE), "src", message);

        verify(this.createActivityHandler).handleOutboxRequest(new ActivityRequest<>(emiter, new Create()
            .setActor(emiter)
            .setPublished(message.getUpdateDate())
            .<Create>setTo(Arrays.asList(actor.getProxyActor()))
            .setObject(new Note()
                .setId(link)
                .setPublished(message.getUpdateDate())
                .setTo(Arrays.asList(actor.getProxyActor()))
                .setInReplyTo(existingNote.getId())
            )
        ));
        verify(this.discussionContextService).link(dc3, discussion);
    }
}