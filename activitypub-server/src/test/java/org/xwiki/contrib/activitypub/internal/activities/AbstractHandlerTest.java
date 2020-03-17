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
package org.xwiki.contrib.activitypub.internal.activities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubNotifier;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Helper for writing handler tests.
 *
 * @version $Id$
 */
public class AbstractHandlerTest
{
    @Mock
    protected HttpServletResponse servletResponse;

    @Mock
    protected HttpServletRequest servletRequest;

    @Mock
    protected ServletOutputStream responseOutput;

    @MockComponent
    protected ActivityPubJsonSerializer activityPubJsonSerializer;

    @MockComponent
    protected ActivityPubStorage activityPubStorage;

    @MockComponent
    protected ActivityPubNotifier notifier;

    @MockComponent
    protected ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @MockComponent
    protected ActorHandler actorHandler;

    @MockComponent
    protected ActivityPubClient activityPubClient;

    @MockComponent
    protected ActivityPubConfiguration activityPubConfiguration;

    @Mock
    protected PostMethod postMethod;

    protected void initMock() throws IOException, ActivityPubException
    {
        when(this.servletResponse.getOutputStream()).thenReturn(this.responseOutput);
        when(this.activityPubClient.postInbox(any(), any())).thenReturn(this.postMethod);
    }

    protected void verifyResponse(int code, String message) throws IOException
    {
        verify(servletResponse, times(1)).setStatus(code);
        verify(servletResponse, times(1)).setContentType("text/plain");
        verify(responseOutput, times(1)).write(message.getBytes(StandardCharsets.UTF_8));
    }

    protected void verifyResponse(ActivityPubObject entity) throws IOException, ActivityPubException
    {
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, entity);
    }
}
