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
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.internal.async.PageChangedRequest;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DocumentUpdatedEventListener}
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
public class DocumentUpdatedEventListenerTest
{
    private static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER);

    @InjectMockComponents
    private DocumentUpdatedEventListener listener;

    @MockComponent
    private ActivityHandler<Update> updateActivityHandler;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private DefaultURLHandler urlHandler;

    @MockComponent
    private XWikiUserBridge xWikiUserBridge;

    @MockComponent
    private JobExecutor jobExecutor;

    @MockComponent
    private ActivityPubConfiguration configuration;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiContext context;

    private Person person;

    @BeforeEach
    public void setup() throws Exception
    {
        this.person = new Person()
            .setPreferredUsername("Foobar")
            .setFollowers(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>()
                    .setLink(new URI("http://foobar/followers")));

        UserReference userReference = mock(UserReference.class);
        when(this.xWikiUserBridge.resolveDocumentReference(this.document.getAuthorReference()))
            .thenReturn(userReference);
        when(this.actorHandler.getActor(userReference)).thenReturn(this.person);
    }

    @Test
    public void onEventPrivateDocument() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(false);
        this.defineMinorRevision();

        this.listener.onEvent(new DocumentUpdatedEvent(), this.document, this.context);

        verify(this.updateActivityHandler, never()).handleOutboxRequest(any());
    }

    @Test
    public void onEventNoFollowers() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers()))
            .thenReturn(new OrderedCollection<>());
        this.defineMinorRevision();
        
        this.listener.onEvent(new DocumentUpdatedEvent(), this.document, this.context);

        verify(this.updateActivityHandler, never()).handleOutboxRequest(any());
    }

    @Test
    public void onEvent() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers())).thenReturn(
            new OrderedCollection<AbstractActor>()
                .addItem(new Person())
                .setId(new URI("http://followers"))
        );

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);
        this.defineMajorRevision();

        Page apDoc = new Page()
            .setName(documentTile)
            .setAttributedTo(
                Collections.singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(this.person))
            )
            .setPublished(creationDate)
            .setUrl(Collections.singletonList(new URI(absoluteDocumentUrl)));
        Update update = new Update()
            .setActor(this.person)
            .setObject(apDoc)
            .setName("Creation of document [A document title]")
            .setPublished(creationDate)
            .setTo(Collections.singletonList(new ProxyActor(this.person.getFollowers().getLink())));
        ActivityRequest<Update> activityRequest = new ActivityRequest<>(this.person, update);

        when(this.configuration.isPageNotificationsEnabled()).thenReturn(true);

        this.listener.onEvent(new DocumentUpdatedEvent(), this.document, this.context);

        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        // FIXME: update when a configuration is added to the creation event handler
        verify(this.jobExecutor).execute(eq("activitypub-update-page"), eq(request));
        verify(this.activityPubStorage, never()).storeEntity(apDoc);
        verify(this.updateActivityHandler, never()).handleOutboxRequest(activityRequest);
    }

    @Test
    public void onEventDeactivated() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers())).thenReturn(
            new OrderedCollection<AbstractActor>()
                .addItem(new Person())
                .setId(new URI("http://followers"))
        );

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);
        this.defineMajorRevision();

        Page apDoc = new Page()
            .setName(documentTile)
            .setAttributedTo(
                Collections.singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(this.person))
            )
            .setPublished(creationDate)
            .setUrl(Collections.singletonList(new URI(absoluteDocumentUrl)));
        Update update = new Update()
            .setActor(this.person)
            .setObject(apDoc)
            .setName("Creation of document [A document title]")
            .setPublished(creationDate)
            .setTo(Collections.singletonList(new ProxyActor(this.person.getFollowers().getLink())));
        ActivityRequest<Update> activityRequest = new ActivityRequest<>(this.person, update);

        when(this.configuration.isPageNotificationsEnabled()).thenReturn(false);

        this.listener.onEvent(new DocumentUpdatedEvent(), this.document, this.context);

        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        // FIXME: update when a configuration is added to the creation event handler
        verify(this.jobExecutor, never()).execute(eq("activitypub-update-page"), eq(request));
        verify(this.activityPubStorage, never()).storeEntity(apDoc);
        verify(this.updateActivityHandler, never()).handleOutboxRequest(activityRequest);
    }

    @Test
    public void onEventMinor() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, this.document.getDocumentReference()))
            .thenReturn(true);
        when(this.objectReferenceResolver.resolveReference(this.person.getFollowers())).thenReturn(
            new OrderedCollection<AbstractActor>()
                .addItem(new Person())
                .setId(new URI("http://followers"))
        );

        String absoluteDocumentUrl = "http://www.xwiki.org/xwiki/bin/view/Main";
        String relativeDocumentUrl = "/xwiki/bin/view/Main";
        String documentTile = "A document title";
        Date creationDate = new Date();

        when(this.document.getURL("view", this.context)).thenReturn(relativeDocumentUrl);
        when(this.urlHandler.getAbsoluteURI(new URI(relativeDocumentUrl))).thenReturn(new URI(absoluteDocumentUrl));
        when(this.document.getCreationDate()).thenReturn(creationDate);
        when(this.document.getTitle()).thenReturn(documentTile);
        this.defineMinorRevision();

        Page apDoc = new Page()
            .setName(documentTile)
            .setAttributedTo(
                Collections.singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(this.person))
            )
            .setPublished(creationDate)
            .setUrl(Collections.singletonList(new URI(absoluteDocumentUrl)));
        Update update = new Update()
            .setActor(this.person)
            .setObject(apDoc)
            .setName("Creation of document [A document title]")
            .setPublished(creationDate)
            .setTo(Collections.singletonList(new ProxyActor(this.person.getFollowers().getLink())));
        ActivityRequest<Update> activityRequest = new ActivityRequest<>(this.person, update);
        this.listener.onEvent(new DocumentUpdatedEvent(), this.document, this.context);

        PageChangedRequest request =
            new PageChangedRequest()
                .setDocumentReference(this.document.getDocumentReference())
                .setAuthorReference(this.document.getAuthorReference())
                .setDocumentTitle(this.document.getTitle())
                .setContent(this.document.getXDOM())
                .setCreationDate(this.document.getCreationDate())
                .setViewURL(this.document.getURL("view", this.context));
        request.setId("activitypub-update-page", this.document.getKey());

        // FIXME: update when a configuration is added to the creation event handler
        verify(this.jobExecutor, never()).execute(eq("activitypub-update-page"), eq(request));
        verify(this.activityPubStorage, never()).storeEntity(apDoc);
        verify(this.updateActivityHandler, never()).handleOutboxRequest(activityRequest);
    }

    private void defineMajorRevision() throws XWikiException
    {
        XWikiRCSNodeInfo value = new XWikiRCSNodeInfo();
        XWikiRCSNodeId id = new XWikiRCSNodeId(1, new Version(1, 1));
        value.setId(id);
        when(this.document.getRevisionInfo(this.document.getVersion(), this.context))
            .thenReturn(value);
    }

    private void defineMinorRevision() throws XWikiException
    {
        XWikiRCSNodeInfo value = new XWikiRCSNodeInfo();
        // the revision of the document is minor.
        XWikiRCSNodeId id = new XWikiRCSNodeId(1, new Version(1, 0));
        value.setId(id);
        when(this.document.getRevisionInfo(this.document.getVersion(), this.context))
            .thenReturn(value);
    }
}
