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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.internal.DateProvider;
import org.xwiki.contrib.activitypub.internal.script.ActivityPubScriptServiceActor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wysiwyg.converter.HTMLConverter;

/**
 * Test of {@link PublishNoteScriptService}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class PublishNoteScriptServiceTest
{
    @InjectMockComponents
    private PublishNoteScriptService publishNoteScriptService;

    @MockComponent
    private ActivityPubScriptServiceActor activityPubScriptServiceActor;

    @MockComponent
    private DateProvider dateProvider;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private HTMLConverter htmlConverter;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @Test
    void publishNoteNoTarget() throws Exception
    {
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);
        when(this.activityPubScriptServiceActor.getSourceActor(null)).thenReturn(actor);
        when(this.dateProvider.currentTime()).thenReturn(new Date());

        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.activityPubScriptServiceActor.getActivityHandler(any())).thenReturn(activityHandler);
        String noteContent = "some content";
        this.publishNoteScriptService.publishNote(null, noteContent);
        Note note = new Note()
            .setContent(noteContent)
            .setAttributedTo(Collections.singletonList(actor.getReference()));

        ArgumentCaptor<ActivityPubObject> argumentCaptor = ArgumentCaptor.forClass(ActivityPubObject.class);
        verify(this.activityPubStorage, times(2)).storeEntity(argumentCaptor.capture());
        List<ActivityPubObject> allValues = argumentCaptor.getAllValues();
        assertEquals(2, allValues.size());
        assertTrue(allValues.get(0) instanceof Note);
        assertTrue(allValues.get(1) instanceof Create);
        Note obtainedNote = (Note) allValues.get(0);
        assertNotNull(obtainedNote.getPublished());
        obtainedNote.setPublished(null);
        assertEquals(note, allValues.get(0));

        Create create = (Create) allValues.get(1);
        assertEquals(note, create.getObject().getObject());
        assertSame(actorReference, create.getActor());
        assertEquals(note.getAttributedTo(), create.getAttributedTo());
        assertEquals(note.getTo(), create.getTo());
        assertNotNull(create.getPublished());
        verify(activityHandler).handleOutboxRequest(new ActivityRequest<>(actor, create));
    }

    @Test
    void publishNoteFollowersAndActor() throws Exception
    {

        when(this.dateProvider.currentTime()).thenReturn(new Date());
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);

        ActivityPubObjectReference followersReference = mock(ActivityPubObjectReference.class);
        when(followersReference.getLink()).thenReturn(new URI("http://followers"));
        when(actor.getFollowers()).thenReturn(followersReference);
        when(this.activityPubScriptServiceActor.getSourceActor(null)).thenReturn(actor);

        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.activityPubScriptServiceActor.getActivityHandler(any())).thenReturn(activityHandler);

        AbstractActor targetActor = mock(AbstractActor.class);
        ProxyActor targetProxyActor = mock(ProxyActor.class);
        when(targetActor.getProxyActor()).thenReturn(targetProxyActor);

        String noteContent = "some content";
        this.publishNoteScriptService.publishNote(Arrays.asList("followers", "@targetActor"), noteContent);

        Note note = new Note()
            .setContent(noteContent)
            .setAttributedTo(Collections.singletonList(actor.getReference()))
            .setTo(Arrays.asList(new ProxyActor(followersReference.getLink()), targetProxyActor));

        ArgumentCaptor<ActivityPubObject> argumentCaptor = ArgumentCaptor.forClass(ActivityPubObject.class);
        verify(this.activityPubStorage, times(2)).storeEntity(argumentCaptor.capture());
        List<ActivityPubObject> allValues = argumentCaptor.getAllValues();
        assertEquals(2, allValues.size());
        ActivityPubObject objectNote = allValues.get(0);
        ActivityPubObject objectCreate = allValues.get(1);
        assertTrue(objectNote instanceof Note);
        assertTrue(objectCreate instanceof Create);
        Note obtainedNote = (Note) objectNote;
        assertNotNull(obtainedNote.getPublished());
        obtainedNote.setPublished(null);

        Create create = (Create) objectCreate;
        assertSame(actorReference, create.getActor());
        assertEquals(note.getAttributedTo(), create.getAttributedTo());
        assertNotNull(create.getPublished());
        verify(activityHandler).handleOutboxRequest(new ActivityRequest<>(actor, create));
    }

    @Test
    void publishNoteRendersTheContentWithTheRightsOfTheCurrentUser() throws Exception
    {
        DocumentReference sender = new DocumentReference("xwiki", "XWiki", "Sender");
        DocumentReference currentDocument = new DocumentReference("xwiki", "ActivityPub", "MessageUser");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(sender);
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(currentDocument);
        // Run the callable like the real executor does, once the author has been switched to the sender.
        when(this.authorExecutor.call(any(), eq(sender), eq(currentDocument))).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            return callable.call();
        });
        String rawContent = "a \"}} {{/html}}{{info}}b";
        when(this.htmlConverter.toHTML(rawContent, Syntax.XWIKI_2_1, null)).thenReturn("<p>rendered</p>");
        mockSendingOfNote();

        assertTrue(this.publishNoteScriptService.publishNote(null, rawContent, null, "xwiki/2.1"));

        verify(this.authorExecutor).call(any(), eq(sender), eq(currentDocument));
        assertEquals("<p>rendered</p>", storedNote().getContent());
    }

    @Test
    void publishNoteRenderingFailure() throws Exception
    {
        DocumentReference sender = new DocumentReference("xwiki", "XWiki", "Sender");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(sender);
        when(this.authorExecutor.call(any(), eq(sender), any())).thenThrow(new RuntimeException("failure"));
        mockSendingOfNote();

        assertFalse(this.publishNoteScriptService.publishNote(null, "some content", null, "xwiki/2.1"));

        verify(this.activityPubStorage, never()).storeEntity(any());
        assertEquals("Error while posting a note.", this.logCapture.getMessage(0));
    }

    @Test
    void publishNoteUnknownSyntax() throws Exception
    {
        mockSendingOfNote();

        assertTrue(this.publishNoteScriptService.publishNote(null, "some content", null, "invalid"));

        verify(this.authorExecutor, never()).call(any(), any(), any());
        assertEquals("some content", storedNote().getContent());
    }

    private void mockSendingOfNote() throws Exception
    {
        Person actor = mock(Person.class);
        when(actor.getReference()).thenReturn(mock(ActivityPubObjectReference.class));
        when(this.activityPubScriptServiceActor.getSourceActor(null)).thenReturn(actor);
        when(this.dateProvider.currentTime()).thenReturn(new Date());
        when(this.activityPubScriptServiceActor.getActivityHandler(any())).thenReturn(mock(ActivityHandler.class));
    }

    private Note storedNote() throws Exception
    {
        ArgumentCaptor<ActivityPubObject> argumentCaptor = ArgumentCaptor.forClass(ActivityPubObject.class);
        verify(this.activityPubStorage, times(2)).storeEntity(argumentCaptor.capture());
        return (Note) argumentCaptor.getAllValues().get(0);
    }
}
