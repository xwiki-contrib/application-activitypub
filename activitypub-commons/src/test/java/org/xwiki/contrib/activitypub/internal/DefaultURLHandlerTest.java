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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultURLHandler}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultURLHandlerTest
{
    private static final String SERVER_URL = "http://xwiki.org";

    @InjectMockComponents
    private DefaultURLHandler urlHandler;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext context;

    @BeforeEach
    public void setup() throws MalformedURLException
    {
        when(contextProvider.get()).thenReturn(context);
        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(context.getURLFactory()).thenReturn(urlFactory);
        when(urlFactory.getServerURL(context)).thenReturn(new URL(SERVER_URL));
    }

    @Test
    public void getServerURL() throws MalformedURLException
    {
        assertEquals(new URL(SERVER_URL), this.urlHandler.getServerUrl());
    }

    @Test
    public void getAbsoluteURI() throws URISyntaxException, MalformedURLException
    {
        URI uri = new URI("/xwiki/bin/view/XWiki/Admin");
        URI expectedURI = new URI("http://xwiki.org/xwiki/bin/view/XWiki/Admin");

        assertEquals(expectedURI, this.urlHandler.getAbsoluteURI(uri));
    }

    @Test
    public void belongsToCurrentInstance() throws Exception
    {
        assertTrue(this.urlHandler.belongsToCurrentInstance(URI.create("http://xwiki.org/foo/something")));
        assertFalse(this.urlHandler.belongsToCurrentInstance(URI.create("https://xwiki.org/foo/something")));
        assertFalse(this.urlHandler.belongsToCurrentInstance(URI.create("http://xwiki.org:4848/foo/something")));
        assertTrue(this.urlHandler.belongsToCurrentInstance(URI.create("http://xwiki.org")));
        assertFalse(this.urlHandler.belongsToCurrentInstance(URI.create("http://xwiki.com/foo/something")));
        assertFalse(this.urlHandler.belongsToCurrentInstance(URI.create("/foo/something")));
    }

    @Test
    public void getExtendedURL() throws Exception
    {
        URI absoluteURI = URI.create("http://xwiki.org/xwiki/activitypub/foo/something");
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("activitypub", "foo", "something"));
        XWikiRequest xWikiRequest = mock(XWikiRequest.class);
        when(context.getRequest()).thenReturn(xWikiRequest);
        when(xWikiRequest.getContextPath()).thenReturn("/xwiki");
        ExtendedURL obtainedURL = this.urlHandler.getExtendedURL(absoluteURI);
        assertEquals(extendedURL.getSegments(), obtainedURL.getSegments());
    }
}
