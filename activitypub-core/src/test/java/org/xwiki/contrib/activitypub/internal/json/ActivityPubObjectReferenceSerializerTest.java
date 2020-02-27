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
package org.xwiki.contrib.activitypub.internal.json;

import java.io.IOException;
import java.net.URI;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link ActivityPubObjectReferenceSerializer}.
 *
 * @since 1.0
 * @version $Id$
 */
@ComponentTest
public class ActivityPubObjectReferenceSerializerTest
{
    @InjectMockComponents
    private ActivityPubObjectReferenceSerializer activityPubObjectReferenceSerializer;

    @MockComponent
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> activityPubResourceReferenceSerializer;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @Test
    void serializeIsLink() throws IOException
    {
        ActivityPubObjectReference<ActivityPubObject> ref =
            new ActivityPubObjectReference<>().setLink(URI.create("http://mylink/"));
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider);
        verify(jsonGenerator).writeString("http://mylink/");
    }

    @Test
    void serializeIsObject() throws Exception
    {
        ActivityPubObjectReference<ActivityPubObject> ref =
            new ActivityPubObjectReference<>().setObject(new Accept());
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        when(this.componentManager.getInstance(ActivityPubStorage.class)).thenReturn(mock(ActivityPubStorage.class));
        when(this.activityPubResourceReferenceSerializer.serialize(any())).thenReturn(URI.create("http://newuri"));
        this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider);
        verify(jsonGenerator).writeString("http://newuri");
    }

    @Test
    void serializeSerializeResourceReferenceException() throws Exception
    {
        ActivityPubObjectReference<ActivityPubObject> ref =
            new ActivityPubObjectReference<>().setObject(new Accept());
        JsonGenerator jsonGenerator = mock(JsonGenerator.class);
        SerializerProvider serializeProvider = mock(SerializerProvider.class);
        when(this.componentManager.getInstance(ActivityPubStorage.class)).thenReturn(mock(ActivityPubStorage.class));
        when(this.activityPubResourceReferenceSerializer.serialize(any()))
            .thenThrow(new SerializeResourceReferenceException("ERR"));
        IOException e = assertThrows(IOException.class,
            () -> this.activityPubObjectReferenceSerializer.serialize(ref, jsonGenerator, serializeProvider));

        // TODO replace with assertEquals when a proper toString is available on AbstractActivity classes.
        assertTrue(e.getMessage().startsWith("Error when serializing ["));
    }
}
