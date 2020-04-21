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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.json.absolute.DefaultActivityPubObjectReferenceSerializer;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class RelativeActivityPubObjectReferenceSerializerTest
{
    @InjectMockComponents
    private RelativeActivityPubObjectReferenceSerializer activityPubObjectReferenceSerializer;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @Mock
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private DefaultURLHandler defaultURLHandler;

    @MockComponent
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> resourceReferenceResolver;

    @BeforeEach
    public void setup() throws Exception
    {
        when(this.componentManager.getInstance(ActivityPubStorage.class)).thenReturn(this.activityPubStorage);
    }

    @Test
    void serializeIsRemoteLink() throws IOException
    {
        ActivityPubObjectReference<ActivityPubObject> ref =
            new ActivityPubObjectReference<>().setLink(URI.create("http://mylink/"));
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider);
        verify(jsonGenerator).writeString("http://mylink/");
    }

    @Test
    void serializeIsLocalLink() throws Exception
    {
        URI uri = URI.create("http://xwiki.org/foo/bar");
        ActivityPubObjectReference<ActivityPubObject> ref = new ActivityPubObjectReference<>().setLink(uri);
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        when(this.defaultURLHandler.belongsToCurrentInstance(uri)).thenReturn(true);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.defaultURLHandler.getExtendedURL(uri)).thenReturn(extendedURL);
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("foo", "bar");
        when(this.resourceReferenceResolver
            .resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(resourceReference);

        this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider);
        verify(jsonGenerator).writeString("foo/bar");
    }

    @Test
    void serializeIsRemoteObject() throws Exception
    {
        Accept object = new Accept();
        ActivityPubObjectReference<ActivityPubObject> ref = new ActivityPubObjectReference<>().setObject(object);
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        String uriString = "http://newuri";
        when(this.activityPubStorage.storeEntity(object)).thenReturn(URI.create(uriString));

        this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider);
        verify(jsonGenerator).writeString(uriString);
    }

    @Test
    void serializeIsLocalObject() throws Exception
    {
        Accept object = new Accept();
        ActivityPubObjectReference<ActivityPubObject> ref = new ActivityPubObjectReference<>().setObject(object);
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        URI uri = URI.create("http://myxwiki.org/foo/bar");
        when(this.activityPubStorage.storeEntity(object)).thenReturn(uri);
        when(this.defaultURLHandler.belongsToCurrentInstance(uri)).thenReturn(true);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.defaultURLHandler.getExtendedURL(uri)).thenReturn(extendedURL);
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("foo", "bar");
        when(this.resourceReferenceResolver
            .resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(resourceReference);

        this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider);
        verify(jsonGenerator).writeString("foo/bar");
    }

    @Test
    void serializeSerializeResourceReferenceException() throws Exception
    {
        ActivityPubObjectReference<ActivityPubObject> ref =
            new ActivityPubObjectReference<>().setObject(new Accept());
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        when(this.activityPubStorage.storeEntity(any())).thenThrow(new ActivityPubException("Error when storing"));
        IOException e = assertThrows(IOException.class,
            () -> this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider));

        // TODO replace with assertEquals when a proper toString is available on AbstractActivity classes.
        assertTrue(e.getMessage().startsWith("Error when serializing reference ["));
    }
}
