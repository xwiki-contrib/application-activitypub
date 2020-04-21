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

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.internal.json.AbstractActivityPubObjectReferenceDeserializer;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;

/**
 * A custom version of {@link AbstractActivityPubObjectReferenceDeserializer} that transforms local relative URI to
 * obtain the absolute ones: this implementation is mainly used inside the storage to retrieve right information.
 *
 * @version $Id$
 * @since 1.2
 */
@Component(roles = RelativeActivityPubObjectReferenceDeserializer.class)
@Singleton
public class RelativeActivityPubObjectReferenceDeserializer extends AbstractActivityPubObjectReferenceDeserializer
{
    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Override
    public URI transformURI(URI uri) throws ActivityPubException
    {
        URI result;
        if (!uri.isAbsolute()) {
            String[] splitted = uri.toASCIIString().split("/");
            if (splitted.length != 2) {
                throw new ActivityPubException(
                    String.format("The relative URL does not follow the scheme type/uid: [%s]", uri));
            }
            ActivityPubResourceReference resourceReference = new ActivityPubResourceReference(splitted[0], splitted[1]);
            try {
                result = this.serializer.serialize(resourceReference);
            } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
                throw new ActivityPubException(
                    String.format("Error when serializing the computed resource reference [%s] for URI [%s]",
                        resourceReference, uri));
            }
        } else {
            result = uri;
        }
        return result;
    }
}
