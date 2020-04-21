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
package org.xwiki.contrib.activitypub.internal.json.relative;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.json.AbstractActivityPubObjectReferenceSerializer;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * A custom version of {@link AbstractActivityPubObjectReferenceSerializer} that transforms local absolute URI to
 * keep them relative: this implementation is mainly used inside the storage to keep right information.
 *
 * @version $Id$
 * @since 1.2
 */
@Component(roles = RelativeActivityPubObjectReferenceSerializer.class)
@Singleton
public class RelativeActivityPubObjectReferenceSerializer extends AbstractActivityPubObjectReferenceSerializer
{
    private static final ResourceType ACTIVITYPUB_RESOURCETYPE = new ResourceType("activitypub");

    @Inject
    private DefaultURLHandler defaultURLHandler;

    @Inject
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @Override
    public URI transformURI(URI inputURI) throws ActivityPubException
    {
        URI result;
        if (this.defaultURLHandler.belongsToCurrentInstance(inputURI)) {
            try {
                ExtendedURL extendedURL = this.defaultURLHandler.getExtendedURL(inputURI);
                ActivityPubResourceReference reference = (ActivityPubResourceReference) this.resourceReferenceResolver
                    .resolve(extendedURL, ACTIVITYPUB_RESOURCETYPE, Collections.emptyMap());
                result = URI.create(String.format("%s/%s", reference.getEntityType(), reference.getUuid()));
            } catch (MalformedURLException | CreateResourceReferenceException
                | UnsupportedResourceReferenceException e) {
                throw new ActivityPubException(String.format("Error while transforming URI [%s]", inputURI), e);
            }
        } else {
            result = inputURI;
        }
        return result;
    }
}
