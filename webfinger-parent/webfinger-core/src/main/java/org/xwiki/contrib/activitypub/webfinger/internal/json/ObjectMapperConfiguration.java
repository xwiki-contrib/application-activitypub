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
package org.xwiki.contrib.activitypub.webfinger.internal.json;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Provides a default json object mapper configuration.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = ObjectMapperConfiguration.class)
@Singleton
public class ObjectMapperConfiguration implements Initializable
{
    private ObjectMapper objectMapper;

    @Override
    public void initialize()
    {
        this.objectMapper = new ObjectMapper()
                                .setSerializationInclusion(NON_EMPTY)
                                .enable(ACCEPT_SINGLE_VALUE_AS_ARRAY)
                                .enable(SORT_PROPERTIES_ALPHABETICALLY)
                                .enable(INDENT_OUTPUT)
                                .disable(FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     *
     * @return the default object mapper.
     */
    public ObjectMapper getObjectMapper()
    {
        return this.objectMapper;
    }
}
