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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.activitypub.entities.Object;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.entities.ObjectReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@Component
@Singleton
public class DefaultActivityPubJsonSerializer implements ActivityPubJsonSerializer, Initializable
{
    @Inject
    private ObjectReferenceSerializer objectReferenceSerializer;

    private ObjectMapper objectMapper;

    @Override
    public void initialize() throws InitializationException
    {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ObjectReference.class, objectReferenceSerializer);
        objectMapper = new ObjectMapper().registerModule(module);
    }

    @Override
    public <T extends Object> String serialize(T object) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        this.serialize(byteArrayOutputStream, object);
        return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public <T extends Object> void serialize(OutputStream stream, T object) throws IOException
    {
        objectMapper.writeValue(stream, object);
    }
}
