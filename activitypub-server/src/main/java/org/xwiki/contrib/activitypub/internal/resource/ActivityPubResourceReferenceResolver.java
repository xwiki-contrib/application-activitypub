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
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Resolve an {@link ExtendedURL} to an {@link ActivityPubResourceReference}.
 *
 * @version $Id$
 */
@Component
@Named("activitypub")
@Singleton
public class ActivityPubResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    private static final String DEFAULT_LOCALE = "UTF-8";

    @Override
    public ActivityPubResourceReference resolve(ExtendedURL extendedURL, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ActivityPubResourceReference reference;
        List<String> segments = extendedURL.getSegments();
        try {
            if (segments.size() == 2) {
                String type = segments.get(0);
                String uuid = URLDecoder.decode(segments.get(1), DEFAULT_LOCALE);
                reference = new ActivityPubResourceReference(type, uuid);
                copyParameters(extendedURL, reference);
            } else if (segments.size() == 3 && "activitypub".equals(segments.get(0))) {
                String type = segments.get(1);
                String uuid = URLDecoder.decode(segments.get(2), DEFAULT_LOCALE);
                reference = new ActivityPubResourceReference(type, uuid);
                copyParameters(extendedURL, reference);
            } else {
                throw new CreateResourceReferenceException(String.format("Invalid ActivityPub URL format [%s]",
                    extendedURL.toString()));
            }
        } catch (UnsupportedEncodingException e) {
            throw new CreateResourceReferenceException("Error while decoding the UID", e);
        }
        return reference;
    }
}
