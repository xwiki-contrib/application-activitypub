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
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.MessageService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.DiscussionContextEntityReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Arrays.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ActivityPubDiscussionsService}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ActivityPubDiscussionsServiceTest
{
    @InjectMockComponents
    private ActivityPubDiscussionsService activityPubDiscussionsService;

    @MockComponent
    private MessageService messageService;

    @MockComponent
    private DiscussionService discussionService;

    @MockComponent
    private DiscussionContextService discussionContextService;

    @MockComponent
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Test
    void link()
    {
        DiscussionContext discussionContext = new DiscussionContext(null, null, null, null);
        Discussion discussion = new Discussion(null, null, null, null, null);
        this.activityPubDiscussionsService.link(discussionContext, discussion);
        verify(this.discussionContextService).link(discussionContext, discussion);
    }

    @Test
    void handleActivityAlreadyHandled() throws Exception
    {
        Create create = new Create();
        create.setObject(new Note()
            .setId(URI.create("http//server/note")));

        when(this.discussionService
            .findByDiscussionContext("activitypub-object", "http//server/note"))
            .thenReturn(true);

        this.activityPubDiscussionsService.handleActivity(create);
        verifyNoInteractions(this.messageService);
    }

    @Test
    void handleActivityNotANote() throws Exception
    {
        Create create = new Create();
        Document document = new Document()
            .setId(URI.create("http//server/note"));
        create.setObject(document);

        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Document>) create.getObject())).thenReturn(document);

        when(this.discussionService
            .findByDiscussionContext("activitypub-object", "http//server/note"))
            .thenReturn(false);

        this.activityPubDiscussionsService.handleActivity(create);
        verifyNoInteractions(this.messageService);
    }

    @Test
    void handleActivity() throws Exception
    {
        Person actor = new Person()
            .setId(URI.create("https://server/actor"));
        Create create = new Create()
            .<Create>setId(URI.create("https://server/create"))
            .setActor(actor)
            .setSummary("Discussion for the Create activity of January 8, 2021 at 15:56");
        URI recipientID = URI.create("https://server/recipient");
        ProxyActor recipientReference = new ProxyActor(recipientID);
        Person recipient = new Person()
            .setId(recipientID);
        Note document = new Note()
            .setId(URI.create("https://server/note"))
            .setTo(asList(recipientReference));
        create.setObject(document);
        String title = "Discussion for the Create activity of January 8, 2021 at 15:56";
        Discussion d1 = new Discussion("d1", title, title, new Date(), null);
        DiscussionContext dc1 = new DiscussionContext("dc1", "https://server/note", "https://server/note",
            new DiscussionContextEntityReference("activitypub-object", "https://server/note"));
        DiscussionContext dc2 = new DiscussionContext("dc2", recipientID.toASCIIString(), recipientID.toASCIIString(),
            new DiscussionContextEntityReference("activitypub-object", recipientID.toASCIIString()));

        when(this.activityPubObjectReferenceResolver.resolveReference(
            (ActivityPubObjectReference<Note>) create.getObject())).thenReturn(document);
        when(this.discussionService
            .findByDiscussionContext("activitypub-object", "http//server/note"))
            .thenReturn(false);
        when(this.discussionService.create(title, title, "ActivityPub.Discussion"))
            .thenReturn(Optional.of(d1));
        when(this.activityPubObjectReferenceResolver.resolveReference(create.getReference())).thenReturn(create);
        when(this.discussionContextService
            .getOrCreate("https://server/note", "https://server/note", "activitypub-object", "https://server/note"))
            .thenReturn(Optional.of(dc1));
        when(this.activityPubObjectReferenceResolver.resolveReference(recipientReference)).thenReturn(recipient);
        when(this.discussionContextService
            .getOrCreate(recipientID.toASCIIString(), recipientID.toASCIIString(), "activitypub-actor",
                recipientID.toASCIIString()))
            .thenReturn(Optional
                .of(dc2));

        this.activityPubDiscussionsService.handleActivity(create);
        verify(this.discussionContextService).link(dc1, d1);
        verify(this.discussionContextService).link(dc2, d1);
    }
}