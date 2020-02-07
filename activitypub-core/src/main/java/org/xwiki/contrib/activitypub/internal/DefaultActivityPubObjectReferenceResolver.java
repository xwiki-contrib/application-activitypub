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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;

@Component
@Singleton
public class DefaultActivityPubObjectReferenceResolver implements ActivityPubObjectReferenceResolver, Initializable
{
    private HttpClient httpClient;

    @Inject
    private ActivityPubJsonParser activityPubJsonParser;

    @Override
    public void initialize() throws InitializationException
    {
        this.httpClient = new HttpClient();
    }

    private boolean isAcceptedContentType(String contentType)
    {
        return contentType != null && contentType.contains("application/activity+json");
    }

    private void checkResponse(GetMethod method) throws IOException
    {
        if (method.getStatusCode() != 200) {
            throw new IOException(String.format("Response code not 200: [%s]", method.getStatusCode()));
        } else {
            String contentType = method.getResponseHeader("content-type").getValue();
            if (!isAcceptedContentType(contentType)) {
                throw
                    new IOException(String.format("Content type response invalid for ActivityPub: [%s]", contentType));
            }
            if (method.getResponseContentLength() == 0) {
                throw new IOException("Body reponse is empty.");
            }
        }
    }

    @Override
    public <T extends ActivityPubObject> T resolveReference(ActivityPubObjectReference<T> reference)
        throws ActivityPubException
    {
        T result = reference.getObject();
        if (!reference.isLink() && result == null) {
            throw new ActivityPubException("The reference property is null and does not have any ID to follow.");
        }
        if (result == null) {
            try {
                GetMethod getMethod = new GetMethod(reference.getLink().toString());
                this.httpClient.executeMethod(getMethod);
                checkResponse(getMethod);
                result = this.activityPubJsonParser.parseRequest(getMethod.getResponseBodyAsString());
                reference.setObject(result);
            } catch (IOException e) {
                throw new ActivityPubException(
                    String.format("Error when retrieving the ActivityPub information from [%s]", reference.getLink()),
                    e);
            }
        }
        return result;
    }


}
