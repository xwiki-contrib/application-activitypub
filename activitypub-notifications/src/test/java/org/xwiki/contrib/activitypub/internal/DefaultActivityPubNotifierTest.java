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
import java.util.HashSet;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Announce;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Link;
import org.xwiki.contrib.activitypub.entities.Mention;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.activitypub.events.AbstractActivityPubEvent;
import org.xwiki.contrib.activitypub.events.AnnounceEvent;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.MentionEvent;
import org.xwiki.contrib.activitypub.events.MessageEvent;
import org.xwiki.contrib.activitypub.events.UpdateEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultActivityPubNotifier}.
 *
 * @version $Id$
 * @since 1.0
 */
@ComponentTest
class DefaultActivityPubNotifierTest
{
    @InjectMockComponents
    private DefaultActivityPubNotifier defaultActivityPubNotifier;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private ActivityPubObjectReferenceResolver resolver;

    @Test
    void notifyNoTargets() throws Exception
    {
        this.defaultActivityPubNotifier.notify(new Accept(), new HashSet<>());
        verify(this.observationManager, never()).notify(any(), any(), any());
    }

    @Test
    void notifyNullTarget() throws Exception
    {
        Note note = new Note();
        Create activity = new Create()
            .setObject(note);

        when(this.resolver.resolveReference((ActivityPubObjectReference<Note>) activity.getObject())).thenReturn(note);

        ActivityPubException activityPubException = assertThrows(ActivityPubException.class,
            () -> this.defaultActivityPubNotifier.notify(activity, singleton(null)));
        assertEquals("You cannot send a notification to a null target.", activityPubException.getMessage());
    }

    @Test
    void notifyUnknownType() throws ActivityPubException
    {
        // Anonymous activity to be sure to fail the type matching.
        AbstractActor actor = mock(AbstractActor.class);
        when(this.actorHandler.getNotificationTarget(actor)).thenReturn("Foobar");
        AbstractActivity activity = new AbstractActivity()
        {
        };
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class,
            () -> this.defaultActivityPubNotifier.notify(activity, singleton(actor)));
        assertEquals(
            "Cannot find the right event to notify about "
                + "[type = [], id = [<null>], name = [<null>], published = [<null>], summary = [<null>], "
                + "to = [<null>], attributedTo = [<null>]]",
            activityPubException.getMessage());
    }

    @Test
    void notifyOneTarget() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        when(this.actorHandler.getNotificationTarget(actor)).thenReturn("Foobar");
        this.defaultActivityPubNotifier.notify(new Accept(), singleton(actor));
        verify(this.observationManager).notify(argThat(
            (AbstractActivityPubEvent<?> activityPubEvent) ->
                activityPubEvent.getTarget().size() == 1 && activityPubEvent.getTarget().contains("Foobar")),
            eq("org.xwiki.contrib:activitypub-notifications"), eq("activitypub.follow"));
    }

    @Test
    void notifyCreateWithPage() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        Page page = new Page();
        Create create = new Create().setObject(page);

        when(this.actorHandler.getNotificationTarget(actor)).thenReturn("actor1");

        when(this.resolver.resolveReference(new ActivityPubObjectReference<>().setObject(page))).thenReturn(page);

        this.defaultActivityPubNotifier.notify(create, singleton(actor));

        verify(this.observationManager)
            .notify(argThat(event -> event instanceof CreateEvent
                    && Objects.equals(((CreateEvent) event).getActivity(), create)
                    && Objects.equals(((CreateEvent) event).getTarget(), singleton("actor1"))),
                eq("org.xwiki.contrib:activitypub-notifications"),
                eq(CreateEvent.EVENT_TYPE));
    }

    @Test
    void notifyCreateWithNoteNoMention() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        AbstractActor sender = mock(AbstractActor.class);
        Note note = new Note();
        Create create = new Create()
            .setObject(note)
            .setActor(sender);

        when(this.actorHandler.getNotificationTarget(actor)).thenReturn("actor1");

        when(this.resolver.resolveReference((ActivityPubObjectReference<Note>) create.getObject())).thenReturn(note);

        this.defaultActivityPubNotifier.notify(create, singleton(actor));

        verify(this.observationManager)
            .notify(argThat(event -> event instanceof MessageEvent
                    && Objects.equals(((MessageEvent) event).getActivity(), create)
                    && Objects.equals(((MessageEvent) event).getTarget(), singleton("actor1"))),
                eq("org.xwiki.contrib:activitypub-notifications"),
                eq(MessageEvent.EVENT_TYPE));
    }

    @Test
    void notifyCreateWithNoteIsMentioned() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        AbstractActor sender = mock(AbstractActor.class);
        Link mention = new Mention()
            .setHref(URI.create("http://actor.org"));
        ActivityPubObjectReference<ActivityPubObject> mentionReference = new ActivityPubObjectReference<>()
            .setObject(mention);
        Note note = new Note()
            .setTag(singletonList(mentionReference));
        Create create = new Create()
            .setObject(note)
            .setActor(sender);

        when(actor.getId()).thenReturn(URI.create("http://actor.org"));
        when(this.actorHandler.getNotificationTarget(actor)).thenReturn("actor1");

        when(this.resolver.resolveReference(new ActivityPubObjectReference<>().setObject(note))).thenReturn(note);
        when(this.resolver.resolveReference(mentionReference)).thenReturn(mention);

        this.defaultActivityPubNotifier.notify(create, singleton(actor));

        verify(this.observationManager)
            .notify(argThat(event -> event instanceof MentionEvent
                    && Objects.equals(((MentionEvent) event).getActivity(), create)
                    && Objects.equals(((MentionEvent) event).getTarget(), singleton("actor1"))),
                eq("org.xwiki.contrib:activitypub-notifications"),
                eq(MentionEvent.EVENT_TYPE));
    }

    @Test
    void notifyUpdate() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        URI noteURI = URI.create("https://server.tld/note/1");
        Note note = new Note()
            .setId(noteURI);
        Update update = new Update()
            .setObject(note);

        when(this.actorHandler.getNotificationTarget(actor)).thenReturn("actor1");
        when(this.resolver.resolveReference((ActivityPubObjectReference<Note>) update.getObject())).thenReturn(note);

        this.defaultActivityPubNotifier.notify(update, singleton(actor));

        verify(this.observationManager)
            .notify(argThat(event -> event instanceof UpdateEvent
                    && Objects.equals(((UpdateEvent) event).getActivity(), update)
                    && Objects.equals(((UpdateEvent) event).getTarget(), singleton("actor1"))),
                eq("org.xwiki.contrib:activitypub-notifications"),
                eq(UpdateEvent.EVENT_TYPE));
    }

    @Test
    void notifyAnnounce() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        Announce announce = new Announce();

        when(this.actorHandler.getNotificationTarget(actor)).thenReturn("actor1");

        this.defaultActivityPubNotifier.notify(announce, singleton(actor));

        verify(this.observationManager)
            .notify(argThat(event -> event instanceof AnnounceEvent
                    && Objects.equals(((AnnounceEvent) event).getActivity(), announce)
                    && Objects.equals(((AnnounceEvent) event).getTarget(), singleton("actor1"))),
                eq("org.xwiki.contrib:activitypub-notifications"),
                eq(AnnounceEvent.EVENT_TYPE));
    }
}
