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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;

/**
 * Default implementation of {@link ActivityPubJsonSerializer}.
 * This implementation relies on {@link ObjectMapperConfiguration}.
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubJsonSerializer implements ActivityPubJsonSerializer
{
    @Inject
    private ObjectMapperConfiguration objectMapperConfiguration;

    @Override
    public <T extends ActivityPubObject> String serialize(T object) throws ActivityPubException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        this.serialize(byteArrayOutputStream, object);
        return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public <T extends ActivityPubObject> void serialize(OutputStream stream, T object) throws ActivityPubException
    {
        try {
            this.objectMapperConfiguration.getObjectMapper().writeValue(stream, object);
        } catch (IOException e) {
            throw new ActivityPubException(
                String.format("Error while serializing the stream to type [%s]", object.getClass()), e);
        }
    }
}
