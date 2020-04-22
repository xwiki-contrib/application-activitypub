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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.ResourceType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.SerializerProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link InternalURINormalizer}.
 *
 * @version $Id$
 */
@ComponentTest
public class InternalURINormalizerTest
{
    @InjectMockComponents
    private InternalURINormalizer internalURINormalizer;

    @MockComponent
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @MockComponent
    private DefaultURLHandler defaultURLHandler;

    @MockComponent
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @Test
    public void retrieveRelativeURI()
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("foo", "bar");
        assertEquals(URI.create("foo/bar"), this.internalURINormalizer.retrieveRelativeURI(resourceReference));
    }

    @Test
    public void relativizeRemoteLink() throws Exception
    {
        URI uri = URI.create("http://mylink/");
        when(this.defaultURLHandler.belongsToCurrentInstance(uri)).thenReturn(false);
        assertEquals(uri, this.internalURINormalizer.relativizeURI(uri));
    }

    @Test
    public void relativizeLocalLink() throws Exception
    {
        URI uri = URI.create("http://xwiki.org/foo/bar");
        when(this.defaultURLHandler.belongsToCurrentInstance(uri)).thenReturn(true);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.defaultURLHandler.getExtendedURL(uri)).thenReturn(extendedURL);
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("foo", "bar");
        when(this.resourceReferenceResolver
            .resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(resourceReference);
        assertEquals(URI.create("foo/bar"), this.internalURINormalizer.relativizeURI(uri));
    }

    @Test
    public void retrieveAboluteAbsoluteURI() throws Exception
    {
        URI remoteURI = URI.create("http://xwiki.org/foo");
        assertEquals(remoteURI, this.internalURINormalizer.retrieveAbsoluteURI(remoteURI));
    }

    @Test
    public void retrieveAboluteRelativeURI() throws Exception
    {
        URI serializedUri = URI.create("foo/bar");
        URI absoluteURI = URI.create("http://xwiki.org/foo/bar");
        when(this.serializer.serialize(new ActivityPubResourceReference("foo", "bar"))).thenReturn(absoluteURI);

        assertEquals(absoluteURI, this.internalURINormalizer.retrieveAbsoluteURI(serializedUri));
    }
}
