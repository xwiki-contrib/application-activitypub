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
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.webfinger.WebfingerClient;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonParser;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;

/**
 * Default implementation of {@link WebfingerClient}.
 *
 * @since 1.1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWebfingerClient implements WebfingerClient
{
    private static final Pattern ACTOR_REGEX = Pattern.compile("^@?(?<username>[^@]+)@(?<domain>[^@]+)$");

    private HttpClient httpClient;

    @Inject
    private WebfingerJsonParser parser;

    /**
     * Default constructor for {@link DefaultWebfingerClient}.
     */
    public DefaultWebfingerClient()
    {
        // The 1000 value is an arbitrarily large value, see https://jira.xwiki.org/browse/XAP-25
        MultiThreadedHttpConnectionManager httpConnectionManager = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams params = httpConnectionManager.getParams();
        params.setMaxTotalConnections(10000);
        params.setDefaultMaxConnectionsPerHost(10000);
        this.httpClient = new HttpClient(httpConnectionManager);
    }

    @Override
    public JSONResourceDescriptor get(String webfingerResource) throws WebfingerException
    {
        if (webfingerResource == null) {
            throw new WebfingerException("Invalid agument, webfingerResource is null.", null);
        }
        
        Matcher match = ACTOR_REGEX.matcher(webfingerResource);
        boolean matches = match.matches();
        if (matches) {
            String username = match.group("username");
            String domain = match.group("domain");
            GetMethod get = null;
            try {
                String resource = URLEncoder.encode(
                    String.format("%s@%s", username, domain), "UTF-8");
                String query = String.format("http://%s/.well-known/webfinger?resource=%s", domain, resource);
                get = new GetMethod(query);
                this.httpClient.executeMethod(get);

                InputStream responseBodyAsStream = get.getResponseBodyAsStream();
                return this.parser.parse(responseBodyAsStream);
            } catch (IOException e) {
                throw new WebfingerException(
                    String.format("Error while querying the webfinger resource for %s", webfingerResource), e);
            } finally {
                if (get != null) {
                    get.releaseConnection();
                }
            }
        } else {
            throw new WebfingerException(String.format("[%s] is not a valid webfinger resource", webfingerResource),
                null);
        }
    }

    @Override
    public boolean testWebFingerConfiguration(String domain) throws WebfingerException
    {
        String query = String.format("http://%s/.well-known/webfinger", domain);
        GetMethod get = new GetMethod(query);
        try {
            this.httpClient.executeMethod(get);
            if (get.getStatusCode() == 400) {
                String response = get.getResponseBodyAsString();
                if (WebfingerResourceReferenceHandler.DEFAULT_ERROR_ANSWER_NO_RESOURCE.equals(response)) {
                    return true;
                }
            }
        } catch (IOException e) {
            throw new WebfingerException(
                String.format("Error while testing WebFinger configuration on domain [%s].", domain), e);
        }
        return false;
    }

    /**
     * Replace the http client.
     * @param httpClient An externaly defined http client.
     */
    public void setHttpClient(HttpClient httpClient)
    {
        this.httpClient = httpClient;
    }
}
