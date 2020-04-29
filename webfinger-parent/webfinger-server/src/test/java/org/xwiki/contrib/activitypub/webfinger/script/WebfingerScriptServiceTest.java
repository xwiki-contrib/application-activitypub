package org.xwiki.contrib.activitypub.webfinger.script;/*
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

import java.net.URI;
import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.webfinger.WebfingerClient;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link WebfingerScriptService}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
class WebfingerScriptServiceTest
{
    @InjectMockComponents
    private WebfingerScriptService webfingerScriptService;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private WebfingerClient webfingerClient;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void isWebfingerConfiguredAlreadyDone() throws Exception
    {
        XWikiContext xWikiContext = mock(XWikiContext.class);
        XWikiURLFactory mock = mock(XWikiURLFactory.class);
        when(mock.getServerURL(xWikiContext)).thenReturn(new URL("http://domain.tld"));
        when(xWikiContext.getURLFactory()).thenReturn(mock);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        when(this.webfingerClient.testWebFingerConfiguration("domain.tld")).thenReturn(true);

        boolean actual1 = this.webfingerScriptService.isWebfingerConfigured();
        assertTrue(actual1);
        // if webfinger has already been configured once, nothing is tested again.
        boolean actual2 = this.webfingerScriptService.isWebfingerConfigured();
        assertTrue(actual2);

        verify(this.contextProvider).get();
        verify(this.webfingerClient).testWebFingerConfiguration("domain.tld");
    }

    @Test
    void getWebfingerId()
    {
        AbstractActor actor = new Person()
            .setPreferredUsername("username")
            .setId(URI.create("http://wiki.tld/person/1"));

        String actual = this.webfingerScriptService.getWebfingerId(actor);
        
        assertEquals("username@wiki.tld", actual);
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void getWebfingerMalformedURL()
    {
        AbstractActor actor = new Person()
            .setPreferredUsername("username")
            .setId(URI.create("abcd://wiki.tld/person/1"));

        String actual = this.webfingerScriptService.getWebfingerId(actor);

        assertNull(actual);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while getting WebFinger id", this.logCapture.getMessage(0));
    }

    @Test
    void getWebfingerNull()
    {
        String actual = this.webfingerScriptService.getWebfingerId(null);

        assertNull(actual);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("Error while getting WebFinger id. The actor is null.", this.logCapture.getMessage(0));
    }
}