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

import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An abstract Jackson Deserializer to deserialize references {@link ActivityPubObjectReference}.
 * This deserializer looks on the property value and deserialize the object if the value starts with a "{"
 * or else deserialize the value as an URI and set the link of the reference with it.
 * The URI might be transformed from/to an absolute or relative URI depending on the concrete implementation.
 *
 * @version $Id$
 * @since 1.2
 */
public abstract class AbstractActivityPubObjectReferenceDeserializer
    // it is expected that the ActivityPubObjectReference is the raw type, using it with <?> breaks Jackson typing
    extends JsonDeserializer<ActivityPubObjectReference>
{
    @Override
    public ActivityPubObjectReference<ActivityPubObject> deserialize(JsonParser jsonParser,
        DeserializationContext deserializationContext) throws IOException, JsonProcessingException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();

        ActivityPubObjectReference<ActivityPubObject> objectReference = new ActivityPubObjectReference<>();
        if (jsonParser.currentToken() == JsonToken.START_OBJECT) {
            objectReference.setObject(mapper.readValue(jsonParser, ActivityPubObject.class));
        } else {
            try {
                objectReference.setLink(this.transformURI(mapper.readValue(jsonParser, URI.class)));
            } catch (ActivityPubException e) {
                throw new IOException("Error when deserializing link", e);
            }
        }
        return objectReference;
    }

    /**
     * Transform the provided URI before the deserialization. This can be useful in particular to resolve relative URI,
     * or in contrary to relativize absolute URI.
     *
     * @param inputURI the URI to transform.
     * @return a transformed URI.
     * @throws ActivityPubException in case of problem during the transformation.
     */
    public abstract URI transformURI(URI inputURI) throws ActivityPubException;
}
