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
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.AbstractActor;

@Component
@Singleton
public class DefaultActivityPubClient implements ActivityPubClient
{
    private HttpClient httpClient;

    @Inject
    private ActivityPubJsonSerializer activityPubJsonSerializer;

    public DefaultActivityPubClient()
    {
        this.httpClient = new HttpClient();
    }

    @Override
    public HttpMethod postInbox(AbstractActor actor, AbstractActivity activity) throws ActivityPubException
    {
        return post(getURIFromObjectReference(actor.getInbox()), activity);
    }

    @Override
    public HttpMethod postOutbox(AbstractActor actor, AbstractActivity activity) throws ActivityPubException
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
    public HttpMethod post(URI uri, AbstractActivity activity) throws ActivityPubException
    {
        try {
            RequestEntity bodyRequest = new StringRequestEntity(this.activityPubJsonSerializer.serialize(activity),
                "application/activity+json",
                "UTF-8");
            PostMethod postMethod = new PostMethod(uri.toASCIIString());
            postMethod.setRequestEntity(bodyRequest);
            this.httpClient.executeMethod(postMethod);
            return postMethod;
        } catch (IOException e) {
            throw new ActivityPubException(String.format("Error when getting entity from [%s]", uri), e);
        }
    }

    @Override
    public HttpMethod get(URI uri) throws IOException
    {
        GetMethod getMethod = new GetMethod(uri.toASCIIString());
        this.httpClient.executeMethod(getMethod);
        return getMethod;
    }

    private boolean checkContentTypeHeader(Header contentTypeHeader)
    {
        return  (contentTypeHeader != null && contentTypeHeader.getValue().contains("application/activity+json"));
    }

    @Override
    public void checkAnswer(HttpMethod method) throws ActivityPubException
    {
        String exceptionMessage = null;
        if (!method.isRequestSent()) {
            exceptionMessage = "The request has not been sent yet.";
        } else if (method.getStatusCode() != 200) {
            exceptionMessage = String.format("200 status code expected, got [%s] instead", method.getStatusCode());
        } else if (!checkContentTypeHeader(method.getResponseHeader("Content-Type"))) {
            exceptionMessage = String.format("Content-Type header should return 'application/activity+json' "
                + "and got [%s] instead", method.getResponseHeader("Content-Type"));
        }

        if (exceptionMessage != null) {
            throw new ActivityPubException(exceptionMessage);
        }
    }
}
