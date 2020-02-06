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
import java.io.InputStream;
import java.io.Reader;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;

import com.fasterxml.jackson.core.JsonProcessingException;

@Component
@Singleton
public class DefaultActivityPubJsonParser implements ActivityPubJsonParser
{
    @Inject
    private ObjectMapperConfiguration objectMapperConfiguration;
    
    @Inject
    private Logger logger;

    @Override
    public <T extends ActivityPubObject> T parseRequest(String requestBody)
    {
        return (T) parseRequest(requestBody, ActivityPubObject.class);
    }

    @Override
    public <T extends ActivityPubObject> T parseRequest(String requestBody, Class<T> type)
    {
        try {
            return this.objectMapperConfiguration.getObjectMapper().readValue(requestBody, type);
        } catch (JsonProcessingException e) {
            this.logger.error("Error while parsing request with type [{}].", type, e);
            return null;
        }
    }

    @Override
    public <T extends ActivityPubObject> T parseRequest(Reader requestBodyReader)
    {
        return (T) parseRequest(requestBodyReader, ActivityPubObject.class);
    }

    @Override
    public <T extends ActivityPubObject> T parseRequest(Reader requestBodyReader, Class<T> type)
    {
        try {
            return this.objectMapperConfiguration.getObjectMapper().readValue(requestBodyReader, type);
        } catch (IOException e) {
            this.logger.error("Error while parsing request with type [{}].", type, e);
            return null;
        }
    }

    @Override
    public <T extends ActivityPubObject> T parseRequest(InputStream requestBodyInputStream)
    {
        return (T) parseRequest(requestBodyInputStream, ActivityPubObject.class);
    }

    @Override
    public <T extends ActivityPubObject> T parseRequest(InputStream requestBodyInputStream, Class<T> type)
    {
        try {
            return this.objectMapperConfiguration.getObjectMapper().readValue(requestBodyInputStream, type);
        } catch (IOException e) {
            this.logger.error("Error while parsing request with type [{}].", type, e);
            return null;
        }
    }
}
