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

import org.xwiki.component.phase.Initializable;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.JSONLDContext;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * This allows to configure a Jackson {@link ObjectMapper} ready to be used in the parser and serializer.
 * This abstract class only require to provide the concrete implementation of JsonSerializer and Deserializer for
 * {@link ActivityPubObjectReference} since various implementation might exist depending on the wanted URI
 * tranformation (see {@link AbstractActivityPubObjectReferenceSerializer}
 * and {@link AbstractActivityPubObjectReferenceDeserializer}).
 *
 * @version $Id$
 * @since 1.2
 */
public abstract class AbstractObjectMapperConfiguration implements ObjectMapperConfiguration, Initializable
{
    @Inject
    private JSONLDContextDeserializer jsonLDContextDeserializer;

    private ObjectMapper objectMapper;

    /**
     * @return the concrete implementation of serializer for {@link ActivityPubObjectReference}.
     */
    public abstract JsonSerializer<ActivityPubObjectReference> getObjectReferenceSerializer();

    /**
     * @return the concrete implementation of deserializer for {@link ActivityPubObjectReference}.
     */
    public abstract JsonDeserializer<ActivityPubObjectReference> getObjectReferenceDeserializer();

    @Override
    public void initialize()
    {
        SimpleModule module = new SimpleModule();

        // defines which serializer/deserializer to use.
        // Maybe we could have use Jackon Polymorphic deserialization, but so far we didn't manage to use it properly
        // cf https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization
        module.addSerializer(ActivityPubObjectReference.class, this.getObjectReferenceSerializer());
        module.addDeserializer(ActivityPubObjectReference.class, this.getObjectReferenceDeserializer());
        module.addDeserializer(JSONLDContext.class, this.jsonLDContextDeserializer);

        this.objectMapper = new ObjectMapper()
            // we don't want null values field to be serialized
            .setSerializationInclusion(NON_NULL)
            // if the property type is a list, it still accepts a single value: i.e. it doesn't need to be a JSON Array
            // it's very useful for cases where an attribute can have one or several values.
            // Compare for example, the attributedTo values in the examples given in
            // https://www.w3.org/TR/activitystreams-vocabulary/#dfn-attributedto and the example 2 from
            // https://www.w3.org/TR/activitypub/#Overview
            .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
            // We don't want the parsing to fail on unknown properties since we cannot guarantee that all properties
            // will be covered by the API: the specification is clear on the fact that anyone can extend it
            // as he/she wants
            .disable(FAIL_ON_UNKNOWN_PROPERTIES)
            // Order properties alphabetically: easier to test the result.
            .enable(SORT_PROPERTIES_ALPHABETICALLY)
            .enable(INDENT_OUTPUT)
            .registerModule(module);
    }

    /**
     * @return a configured object mapper ready to be used.
     */
    public ObjectMapper getObjectMapper()
    {
        return this.objectMapper;
    }
}
