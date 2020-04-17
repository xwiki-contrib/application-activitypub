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

import java.net.URL;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link XWikiUserBridge}.
 *
 * @version $Id$
 * @since 1.1
 */
@ComponentTest
public class XWikiUserBridgeTest
{
    @InjectMockComponents
    private XWikiUserBridge xWikiUserBridge;

    @MockComponent
    private UserManager userManager;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userFromDocumentReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DocumentAccessBridge documentAccess;

    @Mock
    private UserReference userReference;

    @Test
    public void isExistingUser()
    {
        when(this.userReferenceResolver.resolve("foo")).thenReturn(userReference);
        when(this.userManager.exists(userReference)).thenReturn(true);
        assertTrue(this.xWikiUserBridge.isExistingUser("foo"));
        assertTrue(this.xWikiUserBridge.isExistingUser(userReference));

        when(this.userManager.exists(userReference)).thenReturn(false);
        assertFalse(this.xWikiUserBridge.isExistingUser("foo"));
        assertFalse(this.xWikiUserBridge.isExistingUser(userReference));
    }

    @Test
    public void getUserLogin()
    {
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("Foo");

        assertEquals("Foo", this.xWikiUserBridge.getUserLogin(userReference));
    }

    @Test
    public void resolveUserReference()
    {
        when(this.userReferenceResolver.resolve("foo")).thenReturn(userReference);
        assertSame(this.userReference, this.xWikiUserBridge.resolveUser("foo"));
    }

    @Test
    public void resolveUserReferenceWithWiki()
    {
        WikiReference wikiReference = new WikiReference("subwiki");
        when(this.userReferenceResolver.resolve("foo", wikiReference)).thenReturn(userReference);
        assertSame(this.userReference, this.xWikiUserBridge.resolveUser("foo", wikiReference));
    }

    @Test
    public void resolveUser()
    {
        when(this.userManager.exists(userReference)).thenReturn(false);
        assertNull(this.xWikiUserBridge.resolveUser(this.userReference));

        when(this.userManager.exists(userReference)).thenReturn(true);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        assertSame(userProperties, this.xWikiUserBridge.resolveUser(userReference));
    }

    @Test
    public void resolveDocumentReference()
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.userFromDocumentReferenceResolver.resolve(documentReference)).thenReturn(this.userReference);
        assertSame(this.userReference, this.xWikiUserBridge.resolveDocumentReference(documentReference));
    }

    @Test
    public void getUserProfileURL() throws Exception
    {
        when(this.userReferenceSerializer.serialize(userReference)).thenReturn("foo");
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "Foo");
        when(this.documentReferenceResolver.resolve("foo")).thenReturn(documentReference);

        XWikiContext xWikiContext = mock(XWikiContext.class);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);

        when(this.contextProvider.get()).thenReturn(xWikiContext);
        when(this.documentAccess.getDocumentInstance(documentReference)).thenReturn(xWikiDocument);
        when(xWikiDocument.getExternalURL("view", xWikiContext)).thenReturn("http://foo");
        assertEquals(new URL("http://foo"), this.xWikiUserBridge.getUserProfileURL(userReference));
    }

    @Test
    public void getCurrentUserReference() throws Exception
    {
        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(xWikiContext.getUserReference()).thenReturn(documentReference);
        UserReference userReference = mock(UserReference.class);
        when(this.userFromDocumentReferenceResolver.resolve(documentReference)).thenReturn(userReference);
        assertSame(userReference, this.xWikiUserBridge.getCurrentUserReference());
    }
}
