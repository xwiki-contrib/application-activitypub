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
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private DefaultWebfingerService client;

    @MockComponent
    private XWikiUserBridge xWikiUserBridge;

    @MockComponent
    private DefaultURLHandler defaultURLHandler;

    @Test
    void resolveActivityPubUserUrl() throws Exception
    {
        UserReference mock = mock(UserReference.class);
        when(this.xWikiUserBridge.resolveUser("aaa")).thenReturn(mock);
        when(this.xWikiUserBridge.getUserLogin(mock)).thenReturn("XWiki.aaa");
        URI mock1 = URI.create("testURI");
        when(this.defaultURLHandler.getAbsoluteURI(any())).thenReturn(mock1);
        URI res = this.client.resolveActivityPubUserUrl("aaa");
        assertEquals(mock1, res);
    }

    @Test
    void resolveActivityPubUserUrlException() throws Exception
    {
        UserReference mock = mock(UserReference.class);
        when(this.xWikiUserBridge.resolveUser("aaa")).thenReturn(mock);
        when(this.xWikiUserBridge.getUserLogin(mock)).thenReturn("XWiki.aaa");
        when(this.defaultURLHandler.getAbsoluteURI(any())).thenThrow(new URISyntaxException("", ""));
        WebfingerException res = assertThrows(WebfingerException.class, () -> this.client.resolveActivityPubUserUrl("aaa"));
        assertEquals("Error while serializing reference for user [aaa]", res.getMessage());
    }

    @Test
    void resolveXWikiUserUrl() throws Exception
    {
        UserReference mock = mock(UserReference.class);
        when(this.xWikiUserBridge.getUserProfileURL(mock)).thenReturn("user@domain");
        String actual = this.client.resolveXWikiUserUrl(mock);
        assertEquals("user@domain", actual);
    }
}