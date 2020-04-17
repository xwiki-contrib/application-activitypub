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
package org.xwiki.contrib.activitypub.webfinger.internal;

import java.net.URI;
import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubIdentifierService;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Test of {@link DefaultWebfingerService}.
 *
 * @since 1.1
 * @version $Id$
 */
@ComponentTest
class DefaultWebfingerServiceTest
{
    @InjectMockComponents
    private DefaultWebfingerService defaultWebfingerService;

    @MockComponent
    private XWikiUserBridge xWikiUserBridge;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

  
    @MockComponent
    private ActivityPubIdentifierService activityPubIdentifierService;

    @Mock
    private XWikiContext context;

    @BeforeEach
    public void setup()
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.activityPubIdentifierService.getWikiIdentifier()).thenReturn("xwiki");
        when(this.activityPubIdentifierService.getWikiSeparator()).thenReturn(".");
    }

    @Test
    void resolveActivityPubUserSimpleUser() throws Exception
    {
        Person person = mock(Person.class);
        UserReference userReference = mock(UserReference.class);
        when(this.xWikiUserBridge.resolveUser("aaa")).thenReturn(userReference);
        when(this.xWikiUserBridge.isExistingUser(userReference)).thenReturn(true);
        when(this.actorHandler.getActor(userReference)).thenReturn(person);

        assertSame(person, this.defaultWebfingerService.resolveActivityPubUser("aaa"));

        when(this.xWikiUserBridge.isExistingUser(userReference)).thenReturn(false);
        assertNull(this.defaultWebfingerService.resolveActivityPubUser("aaa"));
    }

    @Test
    void resolveActivityPubUserMainWiki() throws Exception
    {
        Service mainWiki = mock(Service.class);
        WikiReference currentWiki = new WikiReference("mainwiki");
        when(this.context.getWikiReference()).thenReturn(currentWiki);
        when(this.actorHandler.getActor(currentWiki)).thenReturn(mainWiki);

        assertSame(mainWiki, this.defaultWebfingerService.resolveActivityPubUser("xwiki.xwiki"));
        verify(this.context, never()).setWikiReference(any());
    }

    @Test
    void resolveActivityPubUserSubWiki() throws Exception
    {
        Service otherwikiActor = mock(Service.class);
        WikiReference currentWiki = new WikiReference("mainwiki");
        when(this.context.getWikiReference()).thenReturn(currentWiki);

        WikiReference otherwiki = new WikiReference("subwiki");
        when(this.actorHandler.getActor(otherwiki)).thenReturn(otherwikiActor);

        when(this.wikiDescriptorManager.exists("subwiki")).thenReturn(true);
        assertSame(otherwikiActor, this.defaultWebfingerService.resolveActivityPubUser("subwiki.xwiki"));

        when(this.wikiDescriptorManager.exists("subwiki")).thenReturn(false);
        assertNull(this.defaultWebfingerService.resolveActivityPubUser("subwiki.xwiki"));
        verify(this.context, never()).setWikiReference(any());
    }

    @Test
    void resolveActivityPubUserUserInSubWiki() throws Exception
    {
        Person subwikiActor = mock(Person.class);
        UserReference userReference = mock(UserReference.class);
        WikiReference currentWiki = new WikiReference("mainwiki");
        when(this.context.getWikiReference()).thenReturn(currentWiki);

        WikiReference otherwiki = new WikiReference("subwiki");
        when(this.xWikiUserBridge.resolveUser("foo", otherwiki)).thenReturn(userReference);
        when(this.actorHandler.getActor(userReference)).thenReturn(subwikiActor);

        when(this.wikiDescriptorManager.exists("subwiki")).thenReturn(true);
        when(this.xWikiUserBridge.isExistingUser(userReference)).thenReturn(true);
        assertSame(subwikiActor, this.defaultWebfingerService.resolveActivityPubUser("foo.subwiki"));

        when(this.wikiDescriptorManager.exists("subwiki")).thenReturn(false);
        when(this.xWikiUserBridge.isExistingUser(userReference)).thenReturn(true);
        assertNull(this.defaultWebfingerService.resolveActivityPubUser("foo.subwiki"));

        when(this.wikiDescriptorManager.exists("subwiki")).thenReturn(true);
        when(this.xWikiUserBridge.isExistingUser(userReference)).thenReturn(false);
        assertNull(this.defaultWebfingerService.resolveActivityPubUser("foo.subwiki"));
        verify(this.context, never()).setWikiReference(any());
    }

    @Test
    void resolveActivityPubUserUnsupportedIdentifier() throws Exception
    {
        assertNull(this.defaultWebfingerService.resolveActivityPubUser("one.two.three"));
    }

    @Test
    void resolveActivityPubUserException() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        when(this.xWikiUserBridge.resolveUser("aaa")).thenReturn(userReference);
        when(this.xWikiUserBridge.isExistingUser(userReference)).thenReturn(true);
        when(this.actorHandler.getActor(userReference)).thenThrow(new ActivityPubException("Error"));
        WebfingerException res = assertThrows(WebfingerException.class,
            () -> this.defaultWebfingerService.resolveActivityPubUser("aaa"));
        assertEquals("Error while resolving username [aaa].", res.getMessage());
    }

    @Test
    void resolveXWikiUserUrlFromPerson() throws Exception
    {
        Person person = mock(Person.class);
        UserReference userReference = mock(UserReference.class);
        when(this.actorHandler.getXWikiUserReference(person)).thenReturn(userReference);
        when(this.xWikiUserBridge.getUserProfileURL(userReference)).thenReturn(new URL("http://user/profile"));
        assertEquals(URI.create("http://user/profile"), this.defaultWebfingerService.resolveXWikiUserUrl(person));
    }

    @Test
    void resolveXWikiUserUrlFromService() throws Exception
    {
        Service service = mock(Service.class);
        WikiReference wikiReference = new WikiReference("foo");
        DocumentReference documentReference = mock(DocumentReference.class);
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.actorHandler.getXWikiWikiReference(service)).thenReturn(wikiReference);
        when(this.wikiDescriptorManager.getById("foo")).thenReturn(wikiDescriptor);
        when(wikiDescriptor.getMainPageReference()).thenReturn(documentReference);
        when(this.documentAccessBridge.getDocumentInstance(documentReference)).thenReturn(xWikiDocument);
        when(xWikiDocument.getExternalURL("view", this.context)).thenReturn("http://service/profile");

        assertEquals(URI.create("http://service/profile"), this.defaultWebfingerService.resolveXWikiUserUrl(service));
    }
}