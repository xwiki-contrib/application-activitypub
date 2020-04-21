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
package org.xwiki.contrib.activitypub.internal.json.absolute;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.internal.json.AbstractObjectMapperConfiguration;

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default implementation of the {@link org.xwiki.contrib.activitypub.internal.json.ObjectMapperConfiguration}.
 * This implementation defines an {@link ObjectMapper} using default implementations of JsonSerializer and Deserializer
 * for {@link ActivityPubObjectReference} which keep absolute URI.
 *
 * @since 1.2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultObjectMapperConfiguration extends AbstractObjectMapperConfiguration
{
    @Inject
    private DefaultActivityPubObjectReferenceSerializer objectReferenceSerializer;

    @Inject
    private DefaultActivityPubObjectReferenceDeserializer objectReferenceDeserializer;

    @Override
    public JsonSerializer<ActivityPubObjectReference> getObjectReferenceSerializer()
    {
        return this.objectReferenceSerializer;
    }

    @Override
    public JsonDeserializer<ActivityPubObjectReference> getObjectReferenceDeserializer()
    {
        return this.objectReferenceDeserializer;
    }
}
