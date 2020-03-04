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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.entities.WebfingerJRD;

/**
 * Defines the default serialization of {@link WebfingerJRD} to json.
 *
 *  @version $Id$
 *  @since 1.1
 */
@Component
@Singleton
public class DefaultWebfingerJsonSerializer implements WebfingerJsonSerializer
{
    @Inject
    private ObjectMapperConfiguration objectMapperConfiguration;

    @Override
    public String serialize(WebfingerJRD object) throws IOException
    {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        this.serialize(byteArrayOutputStream, object);
        return new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public void serialize(OutputStream stream, WebfingerJRD object) throws IOException
    {
        this.objectMapperConfiguration.getObjectMapper().writeValue(stream, object);
    }
}
