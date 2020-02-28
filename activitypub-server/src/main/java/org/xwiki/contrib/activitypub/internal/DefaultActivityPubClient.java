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
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.AbstractActor;

/**
 * Default implementation of the {@link ActivityPubClient}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubClient implements ActivityPubClient
{
    private static final String CONTENT_TYPE_HEADER_NAME = "Content-Type";

    private HttpClient httpClient;

    @Inject
    private ActivityPubJsonSerializer activityPubJsonSerializer;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public DefaultActivityPubClient()
    {
        this.httpClient = new HttpClient();
    }

    /**
     * A setter for httpclient for testing purpose.
     * @param client the {@link HttpClient} implementation to use.
     */
    protected void setHttpClient(HttpClient client)
    {
        this.httpClient = client;
    }

    @Override
    public HttpMethod postInbox(AbstractActor actor, AbstractActivity activity) throws ActivityPubException, IOException
    {
        return post(getURIFromObjectReference(actor.getInbox()), activity);
    }

    // FIXME: Credentials must be provided to post in an outbox.
    // See: https://www.w3.org/TR/activitypub/#client-to-server-interactions
    @Override
    public HttpMethod postOutbox(AbstractActor actor, AbstractActivity activity)
        throws ActivityPubException, IOException
    {
        return post(getURIFromObjectReference(actor.getOutbox()), activity);
    }

    private URI getURIFromObjectReference(ActivityPubObjectReference<? extends ActivityPubObject> objectReference)
    {
        if (objectReference.isLink()) {
            return objectReference.getLink();
        } else {
            return objectReference.getObject().getId();
        }
    }

    @Override
    public HttpMethod post(URI uri, AbstractActivity activity) throws ActivityPubException, IOException
    {
        RequestEntity bodyRequest =
            new StringRequestEntity(this.activityPubJsonSerializer.serialize(activity), CONTENT_TYPE, "UTF-8");
        PostMethod postMethod = new PostMethod(uri.toASCIIString());
        postMethod.setRequestEntity(bodyRequest);
        this.httpClient.executeMethod(postMethod);
        return postMethod;
    }

    @Override
    public HttpMethod get(URI uri) throws IOException
    {
        GetMethod getMethod = new GetMethod(uri.toASCIIString());
        getMethod.addRequestHeader("Accept", CONTENT_TYPE);
        this.httpClient.executeMethod(getMethod);
        return getMethod;
    }

    private boolean checkContentTypeHeader(Header contentTypeHeader)
    {
        return  (contentTypeHeader != null && contentTypeHeader.getValue().contains(CONTENT_TYPE));
    }

    @Override
    public void checkAnswer(HttpMethod method) throws ActivityPubException
    {
        String exceptionMessage = null;
        if (!method.isRequestSent()) {
            exceptionMessage = "The request has not been sent.";
        } else if (method.getStatusCode() != 200) {
            String responseBody = null;
            try {
                responseBody = method.getResponseBodyAsString();
            } catch (IOException e) {
                logger.error("Cannot retrieve response body of a request.", e);
            }
            exceptionMessage = String.format("200 status code expected, got [%s] instead with body: [%s].",
                method.getStatusCode(), responseBody);
        } else if (!checkContentTypeHeader(method.getResponseHeader(CONTENT_TYPE_HEADER_NAME))) {
            exceptionMessage = String.format("Content-Type header should return '%s' and got [%s] instead.",
                CONTENT_TYPE, method.getResponseHeader(CONTENT_TYPE_HEADER_NAME));
        }

        if (exceptionMessage != null) {
            String baseMessage = null;
            try {
                baseMessage = String.format("Error when performing [%s] on [%s]: ",
                    method.getName(), method.getURI());
            } catch (URIException e) {
                logger.error("Cannot retrieve URI from HttpMethod.", e);
            }
            throw new ActivityPubException(baseMessage + exceptionMessage);
        }
    }
}
