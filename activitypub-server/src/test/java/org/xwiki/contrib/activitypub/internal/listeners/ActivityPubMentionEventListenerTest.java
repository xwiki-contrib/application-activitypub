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

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.internal.ActivityPubMentionsSender;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.mentions.MentionLocation;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ActivityPubMentionEventListener}.
 *
 * @version $Id$
 * @since 1.4
 */
@ComponentTest
class ActivityPubMentionEventListenerTest
{
    private static final DocumentReference ENTITY_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    private static final String VERSION = "1.2";

    private static final String DOC_URL = "http://wiki/doc/1";

    @InjectMockComponents
    private ActivityPubMentionEventListener activityPubMentionEventListener;

    @MockComponent
    private ActivityPubMentionsSender activityPubMentionsSender;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DefaultURLHandler urlHandler;

    @Test
    void onEventUnknownDataType()
    {
        // Anything but a MentionNotificationParameters
        Object data = new Object();
        this.activityPubMentionEventListener.onEvent(null, null, data);

        verifyNoInteractions(this.activityPubMentionsSender);
        verifyNoInteractions(this.authorizationManager);
        verifyNoInteractions(this.documentRevisionProvider);
        verifyNoInteractions(this.contextProvider);
        verifyNoInteractions(this.urlHandler);
    }

    @Test
    void onEventViewNotAllowed()
    {
        Object data =
            new MentionNotificationParameters("xwiki:XWiki.Author", ENTITY_REFERENCE,
                MentionLocation.DOCUMENT, "1.2");

        when(this.authorizationManager.hasAccess(Right.VIEW, null, ENTITY_REFERENCE)).thenReturn(false);

        this.activityPubMentionEventListener.onEvent(null, null, data);

        verifyNoInteractions(this.activityPubMentionsSender);
        verifyNoInteractions(this.documentRevisionProvider);
        verifyNoInteractions(this.contextProvider);
        verifyNoInteractions(this.urlHandler);
    }

    @Test
    void onEvent() throws Exception
    {
        MentionNotificationParameters data =
            new MentionNotificationParameters("xwiki:XWiki.Author", ENTITY_REFERENCE,
                MentionLocation.DOCUMENT, VERSION);
        XWikiDocument doc = mock(XWikiDocument.class);

        when(this.authorizationManager.hasAccess(Right.VIEW, null, ENTITY_REFERENCE)).thenReturn(true);
        when(this.documentRevisionProvider.getRevision(ENTITY_REFERENCE, VERSION)).thenReturn(doc);
        when(doc.getURL(eq("view"), any())).thenReturn(DOC_URL);
        URI uri = URI.create(DOC_URL);
        when(this.urlHandler.getAbsoluteURI(uri)).thenReturn(uri);

        this.activityPubMentionEventListener.onEvent(null, null, data);

        verify(this.activityPubMentionsSender).sendNotification(data, doc, uri);
    }
}
