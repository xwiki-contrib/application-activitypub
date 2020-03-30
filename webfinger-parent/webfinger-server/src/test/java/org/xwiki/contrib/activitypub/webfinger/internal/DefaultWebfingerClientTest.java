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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonParser;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Test of {@link DefaultWebfingerClient}.
 *
 * @since 1.1
 * @version $Id$
 */
@ComponentTest
class DefaultWebfingerClientTest
{
    @InjectMockComponents
    private DefaultWebfingerClient client;

    @Mock
    private HttpClient httpClient;

    @MockComponent
    private WebfingerJsonParser parser;

    @BeforeEach
    public void setup() throws URISyntaxException
    {
        this.client.setHttpClient(this.httpClient);
    }

    @Test
    void getQuery() throws Exception
    {
        this.client.get("user@test.org");
        verify(this.httpClient).executeMethod(ArgumentMatchers.argThat(
            argument -> {
                try {
                    return String.valueOf(argument.getURI())
                               .equals("http://test.org/.well-known/webfinger?resource=user%40test.org");
                } catch (URIException e) {
                    return false;
                }
            }));
    }
    
    @Test
    void getNull() throws Exception {
        WebfingerException actual = assertThrows(WebfingerException.class, () -> this.client.get(null));
        assertEquals("Invalid agument, webfingerResource is null.", actual.getMessage());
    }

    @Test
    void getMalformed() throws Exception
    {
        WebfingerException actual = assertThrows(WebfingerException.class, () -> this.client.get("hello"));
        assertEquals("[hello] is not a valid webfinger resource", actual.getMessage());
    }

    @Test
    void getWebfingerException() throws Exception
    {
        when(this.parser.parse(nullable(InputStream.class))).thenThrow(new WebfingerException("TEST", null));
        WebfingerException actual = assertThrows(WebfingerException.class, () -> this.client.get("user@test.org"));
        assertEquals("TEST", actual.getMessage());
    }

    @Test
    void getIOException() throws Exception
    {
        when(this.httpClient.executeMethod(any())).thenThrow(new IOException("TEST"));
        WebfingerException actual = assertThrows(WebfingerException.class, () -> this.client.get("user@test.org"));
        assertEquals("Error while querying the webfinger resource for user@test.org", actual.getMessage());
    }
}