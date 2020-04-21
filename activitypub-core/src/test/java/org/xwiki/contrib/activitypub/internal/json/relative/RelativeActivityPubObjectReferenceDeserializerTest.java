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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.internal.json.absolute.DefaultActivityPubObjectReferenceDeserializer;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link RelativeActivityPubObjectReferenceDeserializer}.
 *
 * @version $Id$
 */
@ComponentTest
public class RelativeActivityPubObjectReferenceDeserializerTest
{
    @InjectMockComponents
    private RelativeActivityPubObjectReferenceDeserializer objectReferenceDeserializer;

    @MockComponent
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Mock
    private JsonParser jsonParser;

    @Mock
    private DeserializationContext deserializationContext;

    @Mock
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setup()
    {
        when(jsonParser.getCodec()).thenReturn(objectMapper);
    }

    @Test
    public void deserializeAbsoluteURI() throws Exception
    {
        URI serializedUri = URI.create("http://xwiki.org/foo");
        ActivityPubObjectReference<ActivityPubObject> expectedReference = new ActivityPubObjectReference<>()
            .setLink(serializedUri);

        when(this.jsonParser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
        when(this.objectMapper.readValue(this.jsonParser, URI.class)).thenReturn(serializedUri);

        ActivityPubObjectReference<ActivityPubObject> obtained =
            objectReferenceDeserializer.deserialize(this.jsonParser, this.deserializationContext);
        assertEquals(expectedReference, obtained);
    }

    @Test
    public void deserializeRelativeURI() throws Exception
    {
        URI serializedUri = URI.create("foo/bar");
        URI absoluteURI = URI.create("http://xwiki.org/foo/bar");
        ActivityPubObjectReference<ActivityPubObject> expectedReference = new ActivityPubObjectReference<>()
            .setLink(absoluteURI);

        when(this.jsonParser.currentToken()).thenReturn(JsonToken.VALUE_STRING);
        when(this.objectMapper.readValue(this.jsonParser, URI.class)).thenReturn(serializedUri);
        when(this.serializer.serialize(new ActivityPubResourceReference("foo", "bar"))).thenReturn(absoluteURI);

        ActivityPubObjectReference<ActivityPubObject> obtained =
            objectReferenceDeserializer.deserialize(this.jsonParser, this.deserializationContext);
        assertEquals(expectedReference, obtained);
    }

    @Test
    public void deserializeObject() throws Exception
    {
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        ActivityPubObjectReference<ActivityPubObject> expectedReference = new ActivityPubObjectReference<>()
            .setObject(activityPubObject);

        when(this.jsonParser.currentToken()).thenReturn(JsonToken.START_OBJECT);
        when(this.objectMapper.readValue(this.jsonParser, ActivityPubObject.class)).thenReturn(activityPubObject);

        ActivityPubObjectReference<ActivityPubObject> obtained =
            objectReferenceDeserializer.deserialize(this.jsonParser, this.deserializationContext);
        assertEquals(expectedReference, obtained);
    }
}
