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
package org.xwiki.contrib.activitypub.internal.resource;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
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

        try {
            // Add the resource type segment.
            segments.add("activitypub");
            segments.add(resource.getEntityType());
            segments.add(URLEncoder.encode(resource.getUuid(), "UTF-8"));

            // Add all optional parameters
            ExtendedURL extendedURL = new ExtendedURL(segments, resource.getParameters());

            extendedURL = this.extendedURLNormalizer.normalize(extendedURL);
            return this.urlHandler.getAbsoluteURI(URI.create(extendedURL.toString()));
        } catch (MalformedURLException | URISyntaxException | UnsupportedEncodingException e) {
            throw new SerializeResourceReferenceException(
                String.format("Error while serializing [%s] to URI.", resource), e);
        }
    }
}
