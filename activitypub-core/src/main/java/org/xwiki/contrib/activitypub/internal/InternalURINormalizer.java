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
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

/**
 * A component that aims at providing a standard way to transform internal URI for storage.
 *
 * @since 1.2
 * @version $Id$
 */
@Component(roles = InternalURINormalizer.class)
@Singleton
public class InternalURINormalizer
{
    private static final ResourceType ACTIVITYPUB_RESOURCETYPE = new ResourceType("activitypub");

    @Inject
    private DefaultURLHandler defaultURLHandler;

    @Inject
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Inject
    private Logger logger;

    /**
     * Transform a local absolute activitypub URI to a relative URI based on the transformation of the given URI to
     * obtain a {@link ActivityPubResourceReference} and then on
     * {@link #retrieveRelativeURI(ActivityPubResourceReference)}. Note that in case the given URI does not belong to
     * the current instance, or if it is not an activitypub URI or in case of any issue during the transformation the
     * URI given as input is immediately returned.
     *
     * @param inputURI an URI to transform to a local relative URI.
     * @return a relative URI complying with {@link #retrieveRelativeURI(ActivityPubResourceReference)} or the input URI
     */
    public URI relativizeURI(URI inputURI)
    {
        URI result;
        if (this.defaultURLHandler.belongsToCurrentInstance(inputURI)) {
            try {
                ExtendedURL extendedURL = this.defaultURLHandler.getExtendedURL(inputURI);
                ActivityPubResourceReference reference = (ActivityPubResourceReference) this.resourceReferenceResolver
                    .resolve(extendedURL, ACTIVITYPUB_RESOURCETYPE, Collections.emptyMap());

                if (reference != null) {
                    result = this.retrieveRelativeURI(reference);
                } else {
                    result = inputURI;
                }
            } catch (MalformedURLException | CreateResourceReferenceException
                | UnsupportedResourceReferenceException e) {
                this.logger.warn("Error while trying to relativize [{}]", inputURI, e);
                result = inputURI;
            }
        } else {
            result = inputURI;
        }
        return result;
    }

    /**
     * Compute back an absolute URI from a relative URI given by
     * {@link #retrieveRelativeURI(ActivityPubResourceReference)} or {@link #relativizeURI(URI)}.
     * If the URI is absolute then the URI given in argument is immediately returned.
     * If it is relative, but does not comply with the format for
     * {@link #retrieveRelativeURI(ActivityPubResourceReference)} then an exception is thrown.
     *
     * @param relativeURI a relative URI respecting the format of
     *          {@link #retrieveRelativeURI(ActivityPubResourceReference)} or an absolute URI
     * @return an absolute URI
     * @throws ActivityPubException if the relative URI does not respect the format of
     *      {@link #retrieveRelativeURI(ActivityPubResourceReference)} or in case of problem when serializing the URL.
     */
    public URI retrieveAbsoluteURI(URI relativeURI) throws ActivityPubException
    {
        URI result;
        if (!relativeURI.isAbsolute()) {
            String[] splitted = relativeURI.toASCIIString().split("/");
            if (splitted.length != 2) {
                throw new ActivityPubException(
                    String.format("The relative URL does not follow the scheme type/uid: [%s]", relativeURI));
            }
            ActivityPubResourceReference resourceReference = new ActivityPubResourceReference(splitted[0], splitted[1]);
            try {
                result = this.serializer.serialize(resourceReference);
            } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
                throw new ActivityPubException(
                    String.format("Error when serializing the computed resource reference [%s] for URI [%s]",
                        resourceReference, relativeURI));
            }
        } else {
            result = relativeURI;
        }
        return result;
    }

    /**
     * Return a relative URI for the current instance corresponding to the format: {@code "type/uid"}, where type is
     * given by {@link ActivityPubResourceReference#getType()} and uid is given by
     * {@link ActivityPubResourceReference#getUuid()}.
     *
     * @param reference the reference to transform to a relative URI.
     * @return a relative URI.
     */
    public URI retrieveRelativeURI(ActivityPubResourceReference reference)
    {
        return URI.create(String.format("%s/%s", reference.getEntityType(), reference.getUuid()));
    }
}
