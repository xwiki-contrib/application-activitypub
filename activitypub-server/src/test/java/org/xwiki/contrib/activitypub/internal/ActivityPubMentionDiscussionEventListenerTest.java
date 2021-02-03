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

import javax.inject.Inject;
import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.DiscussionsRightService;
import org.xwiki.contrib.discussions.domain.Discussion;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.contrib.discussions.domain.DiscussionContextEntityReference;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Test of {@link ActivityPubMentionDiscussionEventListener}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class ActivityPubMentionDiscussionEventListenerTest
{
    @InjectMockComponents
    private ActivityPubMentionDiscussionEventListener target;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DiscussionService discussionService;

    @MockComponent
    private DiscussionContextService discussionContextService;

    @MockComponent
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private DiscussionsRightService discussionsRightService;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    private static final DocumentReference ENTITY_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    private static final String VERSION = "1.2";

    @Test
    void onEventNoMessage() throws Exception
    {
        MentionNotificationParameters data = new MentionNotificationParameters("xwiki:XWiki.Author", ENTITY_REFERENCE,
            MentionLocation.DOCUMENT, VERSION);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        XWikiContext xWikiContext = mock(XWikiContext.class);

        when(this.authorizationManager.hasAccess(Right.VIEW, null, ENTITY_REFERENCE)).thenReturn(true);
        when(this.documentRevisionProvider.getRevision(ENTITY_REFERENCE, VERSION)).thenReturn(xWikiDocument);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWikiId()).thenReturn("xwiki");
        when(xWikiDocument
            .getXObjects(new DocumentReference("xwiki", Arrays.asList("Discussions", "Code"), "MessageClass")))
            .thenReturn(Arrays.asList());

        this.target.onEvent(null, null, data);

        verifyNoInteractions(this.discussionService);
        verifyNoInteractions(this.discussionContextService);
        verifyNoInteractions(this.activityPubObjectReferenceResolver);
        verifyNoInteractions(this.actorHandler);
        verifyNoInteractions(this.discussionsRightService);
    }

    @Test
    void onEvent() throws Exception
    {
        MentionNotificationParameters data = new MentionNotificationParameters("xwiki:XWiki.Author", ENTITY_REFERENCE,
            MentionLocation.DOCUMENT, VERSION);
        data.addMention("activitypub",
            new MentionNotificationParameter("U1@xwiki.org", "anchorId", DisplayStyle.FIRST_NAME));
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        XWikiContext xWikiContext = mock(XWikiContext.class);
        BaseObject baseObject = mock(BaseObject.class);
        String actorURI = "https://xwiki.org/U1";
        Person person = new Person().setId(URI.create(actorURI));
        ActivityPubObjectReference<ActivityPubObject> reference = new ActivityPubObjectReference<>().setObject(person);
        DiscussionContext discussionContext = new DiscussionContext("", "", "",
            new DiscussionContextEntityReference("activitypub-actor", actorURI));
        Discussion discussion =
            new Discussion("discussionRef", "discussionTitle", "discussionDescription", new Date(), "XWiki.Doc");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "U1");

        when(this.authorizationManager.hasAccess(Right.VIEW, null, ENTITY_REFERENCE)).thenReturn(true);
        when(this.documentRevisionProvider.getRevision(ENTITY_REFERENCE, VERSION)).thenReturn(xWikiDocument);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        when(xWikiContext.getWikiId()).thenReturn("xwiki");
        when(xWikiDocument
            .getXObjects(new DocumentReference("xwiki", Arrays.asList("Discussions", "Code"), "MessageClass")))
            .thenReturn(Arrays.asList(baseObject));
        when(baseObject.getStringValue("discussionReference")).thenReturn("discussionRef");
        when(this.discussionService.get("discussionRef")).thenReturn(Optional
            .of(discussion));
        when(this.discussionContextService.findByDiscussionReference("discussionRef"))
            .thenReturn(Arrays.asList(discussionContext));
        when(this.actorHandler.getActor("U1@xwiki.org")).thenReturn(person);
        when(this.activityPubObjectReferenceResolver.resolveReference(reference)).thenReturn(person);
        when(this.discussionContextService.getOrCreate("", "", "activitypub-actor", actorURI))
            .thenReturn(Optional.of(discussionContext));
        when(this.actorHandler.isLocalActor(person)).thenReturn(true);
        when(this.actorHandler.getStoreDocument(person)).thenReturn(documentReference);

        this.target.onEvent(null, null, data);

        verify(this.discussionContextService).link(discussionContext, discussion);
        verify(this.discussionsRightService).setRead(discussion, documentReference);
        verify(this.discussionsRightService).setWrite(discussion, documentReference);
    }
}