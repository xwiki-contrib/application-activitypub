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
package org.xwiki.contrib.activitypub.internal.json;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.Object;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;

@Component
@Singleton
public class DefaultActivityPubJsonParser implements ActivityPubJsonParser, Initializable
{
    private ObjectMapper objectMapper;
    private HttpClient httpClient;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        objectMapper = new ObjectMapper().configure(ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        this.httpClient = new HttpClient();
    }

    @Override
    public <T extends Object> T parseRequest(String requestBody)
    {
        return (T) parseRequest(requestBody, Object.class);
    }

    @Override
    public <T extends Object> T parseRequest(String requestBody, Class<T> type)
    {
        try {
            return objectMapper.readValue(requestBody, type);
        } catch (JsonProcessingException e) {
            this.logger.error("Error while parsing request with type [{}].", type, e);
            return null;
        }
    }

    private boolean isAcceptedContentType(String contentType)
    {
        return "application/activity+json".equals(contentType);
    }

    private boolean isResponseOK(GetMethod method)
    {
        if (method.getStatusCode() != 200) {
            return false;
        } else {
            String contentType = method.getResponseHeader("content-type").getValue();
            if (!isAcceptedContentType(contentType)) {
                return false;
            }
            if (method.getResponseContentLength() == 0) {
                return false;
            }
            return true;
        }
    }

    @Override
    public <T extends Object> T resolveObject(URI uri)
    {
        try {
            GetMethod getMethod = new GetMethod(uri.toString());
            this.httpClient.executeMethod(getMethod);
            if (isResponseOK(getMethod)) {
                return this.parseRequest(getMethod.getResponseBodyAsString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
