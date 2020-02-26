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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultActivityPubClientTest
{
    private static final String CLIENT_CONTENT_TYPE =
        "application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"";

    private static final String TEST_URL = "http://www.xwiki.org/xwiki/activitypub/object/42";

    @InjectMockComponents
    private DefaultActivityPubClient activityPubClient;

    @Mock
    private HttpClient httpClient;

    @MockComponent
    private ActivityPubJsonSerializer activityPubJsonSerializer;

    private URI uri;

    @BeforeEach
    public void setup() throws URISyntaxException
    {
        this.activityPubClient.setHttpClient(httpClient);
        this.uri = new URI(TEST_URL);
    }

    @Test
    public void get() throws IOException
    {
        HttpMethod httpMethod = this.activityPubClient.get(this.uri);
        verify(this.httpClient, times(1)).executeMethod(httpMethod);

        assertTrue(httpMethod instanceof GetMethod);
        assertEquals(new org.apache.commons.httpclient.URI(TEST_URL, false), httpMethod.getURI());
        assertEquals(CLIENT_CONTENT_TYPE, httpMethod.getRequestHeader("Accept").getValue());
    }

    @Test
    public void post() throws Exception
    {
        Create create = new Create();
        when(this.activityPubJsonSerializer.serialize(create)).thenReturn("{activity:create}");

        HttpMethod httpMethod = this.activityPubClient.post(this.uri, create);
        verify(this.httpClient, times(1)).executeMethod(httpMethod);

        assertTrue(httpMethod instanceof PostMethod);
        PostMethod postMethod = (PostMethod) httpMethod;
        assertEquals(new org.apache.commons.httpclient.URI(TEST_URL, false), httpMethod.getURI());
        assertTrue(postMethod.getRequestEntity() instanceof StringRequestEntity);
        StringRequestEntity retrievedRequestEntity = (StringRequestEntity) postMethod.getRequestEntity();
        assertEquals("{activity:create}", retrievedRequestEntity.getContent());
        assertEquals("UTF-8", retrievedRequestEntity.getCharset());
        assertEquals(CLIENT_CONTENT_TYPE +"; charset=UTF-8", retrievedRequestEntity.getContentType());
    }

    @Test
    public void postInbox() throws Exception
    {
        Inbox inbox = new Inbox().setId(this.uri);
        Person person = new Person().setInbox(new ActivityPubObjectReference<Inbox>().setObject(inbox));
        Create create = new Create();
        when(this.activityPubJsonSerializer.serialize(create)).thenReturn("{activity:create}");

        HttpMethod httpMethod = this.activityPubClient.postInbox(person, create);
        verify(this.httpClient, times(1)).executeMethod(httpMethod);

        assertTrue(httpMethod instanceof PostMethod);
        PostMethod postMethod = (PostMethod) httpMethod;
        assertEquals(new org.apache.commons.httpclient.URI(TEST_URL, false), httpMethod.getURI());
        assertTrue(postMethod.getRequestEntity() instanceof StringRequestEntity);
        StringRequestEntity retrievedRequestEntity = (StringRequestEntity) postMethod.getRequestEntity();
        assertEquals("{activity:create}", retrievedRequestEntity.getContent());
        assertEquals("UTF-8", retrievedRequestEntity.getCharset());
        assertEquals(CLIENT_CONTENT_TYPE +"; charset=UTF-8", retrievedRequestEntity.getContentType());
    }

    @Test
    public void postInboxRef() throws Exception
    {
        Person person = new Person().setInbox(new ActivityPubObjectReference<Inbox>().setLink(this.uri));
        Create create = new Create();
        when(this.activityPubJsonSerializer.serialize(create)).thenReturn("{activity:create}");

        HttpMethod httpMethod = this.activityPubClient.postInbox(person, create);
        verify(this.httpClient, times(1)).executeMethod(httpMethod);

        assertTrue(httpMethod instanceof PostMethod);
        PostMethod postMethod = (PostMethod) httpMethod;
        assertEquals(new org.apache.commons.httpclient.URI(TEST_URL, false), httpMethod.getURI());
        assertTrue(postMethod.getRequestEntity() instanceof StringRequestEntity);
        StringRequestEntity retrievedRequestEntity = (StringRequestEntity) postMethod.getRequestEntity();
        assertEquals("{activity:create}", retrievedRequestEntity.getContent());
        assertEquals("UTF-8", retrievedRequestEntity.getCharset());
        assertEquals(CLIENT_CONTENT_TYPE +"; charset=UTF-8", retrievedRequestEntity.getContentType());
    }

    @Test
    public void postOutbox() throws Exception
    {
        Outbox outbox = new Outbox().setId(this.uri);
        Person person = new Person().setOutbox(new ActivityPubObjectReference<Outbox>().setObject(outbox));
        Create create = new Create();
        when(this.activityPubJsonSerializer.serialize(create)).thenReturn("{activity:create}");

        HttpMethod httpMethod = this.activityPubClient.postOutbox(person, create);
        verify(this.httpClient, times(1)).executeMethod(httpMethod);

        assertTrue(httpMethod instanceof PostMethod);
        PostMethod postMethod = (PostMethod) httpMethod;
        assertEquals(new org.apache.commons.httpclient.URI(TEST_URL, false), httpMethod.getURI());
        assertTrue(postMethod.getRequestEntity() instanceof StringRequestEntity);
        StringRequestEntity retrievedRequestEntity = (StringRequestEntity) postMethod.getRequestEntity();
        assertEquals("{activity:create}", retrievedRequestEntity.getContent());
        assertEquals("UTF-8", retrievedRequestEntity.getCharset());
        assertEquals(CLIENT_CONTENT_TYPE +"; charset=UTF-8", retrievedRequestEntity.getContentType());
    }

    @Test
    public void postOutboxRef() throws Exception
    {
        Person person = new Person().setOutbox(new ActivityPubObjectReference<Outbox>().setLink(this.uri));
        Create create = new Create();
        when(this.activityPubJsonSerializer.serialize(create)).thenReturn("{activity:create}");

        HttpMethod httpMethod = this.activityPubClient.postOutbox(person, create);
        verify(this.httpClient, times(1)).executeMethod(httpMethod);

        assertTrue(httpMethod instanceof PostMethod);
        PostMethod postMethod = (PostMethod) httpMethod;
        assertEquals(new org.apache.commons.httpclient.URI(TEST_URL, false), httpMethod.getURI());
        assertTrue(postMethod.getRequestEntity() instanceof StringRequestEntity);
        StringRequestEntity retrievedRequestEntity = (StringRequestEntity) postMethod.getRequestEntity();
        assertEquals("{activity:create}", retrievedRequestEntity.getContent());
        assertEquals("UTF-8", retrievedRequestEntity.getCharset());
        assertEquals(CLIENT_CONTENT_TYPE +"; charset=UTF-8", retrievedRequestEntity.getContentType());
    }

    @Test
    public void checkAsnwer() throws ActivityPubException
    {
        HttpMethod method = mock(GetMethod.class);
        when(method.isRequestSent()).thenReturn(false);
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubClient.checkAnswer(method);
        });
        assertEquals("The request has not been sent.", activityPubException.getMessage());

        when(method.isRequestSent()).thenReturn(true);
        when(method.getStatusCode()).thenReturn(404);
        activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubClient.checkAnswer(method);
        });
        assertEquals("200 status code expected, got [404] instead", activityPubException.getMessage());

        when(method.getStatusCode()).thenReturn(200);
        activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubClient.checkAnswer(method);
        });
        assertEquals("Content-Type header should return 'application/ld+json; "
            + "profile=\"https://www.w3.org/ns/activitystreams\"' and got [null] instead",
            activityPubException.getMessage());

        when(method.getResponseHeader("Content-Type")).thenReturn(new Header("Content-Type", CLIENT_CONTENT_TYPE));
        this.activityPubClient.checkAnswer(method);
        verify(method, times(3)).getResponseHeader("Content-Type");
    }
}
