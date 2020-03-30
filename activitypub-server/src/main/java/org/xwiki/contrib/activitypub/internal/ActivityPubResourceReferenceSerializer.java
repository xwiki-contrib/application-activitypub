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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.http.client.utils.URIBuilder;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

/**
 * Serialize an {@link ActivityPubResourceReference} to an {@link URI}.
 * This is particularly useful to set the ID of some {@link org.xwiki.contrib.activitypub.entities.ActivityPubObject}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class ActivityPubResourceReferenceSerializer implements
    ResourceReferenceSerializer<ActivityPubResourceReference, URI>
{
    @Inject
    private DefaultURLHandler urlHandler;

    @Inject
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> extendedURLNormalizer;

    @Override
    public URI serialize(ActivityPubResourceReference resource)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        List<String> segments = new ArrayList<>();

        // Add the resource type segment.
        segments.add("activitypub");
        segments.add(resource.getEntityType());
        segments.add(resource.getUuid());

        // Add all optional parameters
        ExtendedURL extendedURL = new ExtendedURL(segments, resource.getParameters());

        extendedURL = this.extendedURLNormalizer.normalize(extendedURL);

        // The following is a nasty hack, we should rely on URLURLNormarlizer once
        // https://jira.xwiki.org/browse/XWIKI-17023 is fixed.
        try {
            URIBuilder uriBuilder = new URIBuilder(this.urlHandler.getServerUrl().toURI());
            uriBuilder.setPathSegments(extendedURL.getSegments());
            for (Map.Entry<String, List<String>> parameter : extendedURL.getParameters().entrySet()) {
                String paramKey = parameter.getKey();
                if (parameter.getValue().isEmpty()) {
                    uriBuilder.addParameter(paramKey, null);
                } else {
                    for (String paramValue : parameter.getValue()) {
                        uriBuilder.addParameter(paramKey, paramValue);
                    }
                }
            }
            return uriBuilder.build();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new SerializeResourceReferenceException(
                String.format("Error while serializing [%s] to URI.", resource), e);
        }
    }
}
