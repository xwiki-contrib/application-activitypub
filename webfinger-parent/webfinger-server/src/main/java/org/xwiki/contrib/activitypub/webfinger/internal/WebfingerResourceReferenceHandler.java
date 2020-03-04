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
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.WebfingerResourceReference;
import org.xwiki.contrib.activitypub.webfinger.entities.LinkJRD;
import org.xwiki.contrib.activitypub.webfinger.entities.WebfingerJRD;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;

/**
 *
 * Webfinger (https://tools.ietf.org/html/rfc7033) resource handler.
 * 
 *
 * @since 1.1
 * @version $Id$
 */
@Component
@Named("webfinger")
@Singleton
public class WebfingerResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    private static final ResourceType TYPE = new ResourceType("webfinger");

    private static final String TEXTPLAIN_CONTENTTYPE = "text/plain";

    @Inject
    private Container container;

    @Inject
    private Logger logger;

    @Inject
    private WebfingerJsonSerializer webfingerJsonSerializer;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(TYPE);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        WebfingerResourceReference resourceReference = (WebfingerResourceReference) reference;
        HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        try {
            this.proceed(resourceReference, request, response);
        } catch (IOException | WebfingerException e) {
            try {
                this.handleException(response, e);
            } catch (IOException ex) {
                this.logger.error("Cannot handle exception properly", ex);
                this.logger.error("Root exception to handle", e);
            }
        }
    }

    private void proceed(WebfingerResourceReference reference, HttpServletRequest request,
        HttpServletResponse response) throws IOException, WebfingerException
    {
        String[] referenceSplit = reference.getResource().split("@");

        if (referenceSplit.length != 2) {
            throw new WebfingerException();
        }

        String username = referenceSplit[0];
        String domain = referenceSplit[1];
        String usernamePath = String.join("/", username.split("\\."));
        response.setContentType("application/activity+json; charset=utf-8");
        response.addHeader("Access-Control-Allow-Origin", "*");
        LinkJRD link1 = new LinkJRD().setRel("http://webfinger.net/rel/profile-page").setType("text/html")
                            .setHref(String.format("http://%s/xwiki/bin/view/%s", domain, usernamePath));
        LinkJRD link2 = new LinkJRD().setRel("self").setType("application/activity+json")
                            .setHref("http://" + domain + "/xwiki/activitypub/Person/" + username);
        WebfingerJRD object =
            new WebfingerJRD().setSubject("acc:" + reference.getResource()).setLinks(Arrays.asList(link1, link2));
        this.webfingerJsonSerializer.serialize(response.getOutputStream(), object);
    }

    /**
     *
     * Utility method to send an error message in case of exception.
     * @param response the servlet response to use
     * @param e the exception to handle.
     * @throws IOException in case of error during the HTTP response
     */
    private void handleException(HttpServletResponse response, Exception e) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType(TEXTPLAIN_CONTENTTYPE);
        e.printStackTrace(response.getWriter());
    }
}
