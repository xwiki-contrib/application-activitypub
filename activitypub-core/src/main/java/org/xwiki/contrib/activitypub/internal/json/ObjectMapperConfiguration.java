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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Component(roles = ObjectMapperConfiguration.class)
@Singleton
public class ObjectMapperConfiguration implements Initializable
{
    @Inject
    private ActivityPubObjectReferenceSerializer objectReferenceSerializer;

    private ObjectMapper objectMapper;

    @Override
    public void initialize() throws InitializationException
    {
        SimpleModule module = new SimpleModule();

        // defines which serializer/deserializer to use.
        // TODO: Check if we cannot use polymorphic serialization instead
        // cf https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization
        module
            .addSerializer(ActivityPubObjectReference.class, objectReferenceSerializer)
            .addDeserializer(ActivityPubObject.class, new ActivityPubObjectDeserializer())
            .addDeserializer(ActivityPubObjectReference.class, new ActivityPubObjectReferenceDeserializer());

        objectMapper = new ObjectMapper()
            // we don't want null values field to be serialized
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            // if the property type is a list, it still accepts a single value: i.e. it doesn't need to be a JSON Array
            // it's very useful for cases where an attribute can have one or several values.
            // Compare for example, the attributedTo values in the examples given in
            // https://www.w3.org/TR/activitystreams-vocabulary/#dfn-attributedto and the example 2 from
            // https://www.w3.org/TR/activitypub/#Overview
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            // We don't want the parsing to fail on unknown properties since we cannot guarantee that all properties
            // will be covered by the API: the specification is clear on the fact that anyone can extend it
            // as he/she wants
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Order properties alphabetically: easier to test the result.
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .registerModule(module);
    }

    public ObjectMapper getObjectMapper()
    {
        return this.objectMapper;
    }
}
