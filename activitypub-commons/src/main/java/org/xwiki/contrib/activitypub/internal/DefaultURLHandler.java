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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;

/**
 * Default component in charge of handling Servlet URLs.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = DefaultURLHandler.class)
@Singleton
public class DefaultURLHandler
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    private URL serverUrl;

    /**
     * @return the URL of the current instance based on the current context.
     * @throws MalformedURLException in case of problem when retrieving the URL.
     */
    public URL getServerUrl() throws MalformedURLException
    {
        if (this.serverUrl == null) {
            XWikiContext context = this.contextProvider.get();
            this.serverUrl = context.getURLFactory().getServerURL(context);
        }
        return this.serverUrl;
    }

    /**
     * Resolve an URI against the current {@link #getServerUrl()}.
     * @param instanceResource the URI to resolve.
     * @return an URI resolved against the server URL which should be absolute.
     * @throws MalformedURLException in case of problem to get the server URL.
     * @throws URISyntaxException in case of problem to retrieve an URI.
     */
    public URI getAbsoluteURI(URI instanceResource) throws MalformedURLException, URISyntaxException
    {
        return getServerUrl().toURI().resolve(instanceResource);
    }
}
