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
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Default implementation of {@link ActivityPubJsonParser}.
 * This implementation directly use {@link ObjectMapperConfiguration}.
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubJsonParser implements ActivityPubJsonParser
{
    private static final String ERROR_MSG = "Error while parsing request with type [%s].";
    @Inject
    private ObjectMapperConfiguration objectMapperConfiguration;
    
    @Inject
    private Logger logger;

    @Override
    public <T extends ActivityPubObject> T parse(String requestBody) throws ActivityPubException
    {
        return (T) parse(requestBody, ActivityPubObject.class);
    }

    @Override
    public <T extends ActivityPubObject> T parse(String requestBody, Class<T> type) throws ActivityPubException
    {
        try {
            return this.objectMapperConfiguration.getObjectMapper().readValue(requestBody, type);
        } catch (JsonProcessingException e) {
            throw new ActivityPubException(String.format(ERROR_MSG, type), e);
        }
    }

    @Override
    public <T extends ActivityPubObject> T parse(Reader requestBodyReader) throws ActivityPubException
    {
        return (T) parse(requestBodyReader, ActivityPubObject.class);
    }

    @Override
    public <T extends ActivityPubObject> T parse(Reader requestBodyReader, Class<T> type) throws ActivityPubException
    {
        try {
            return this.objectMapperConfiguration.getObjectMapper().readValue(requestBodyReader, type);
        } catch (IOException e) {
            throw new ActivityPubException(String.format(ERROR_MSG, type), e);
        }
    }

    @Override
    public <T extends ActivityPubObject> T parse(InputStream requestBodyInputStream) throws ActivityPubException
    {
        return (T) parse(requestBodyInputStream, ActivityPubObject.class);
    }

    @Override
    public <T extends ActivityPubObject> T parse(InputStream requestBodyInputStream, Class<T> type)
        throws ActivityPubException
    {
        try {
            return this.objectMapperConfiguration.getObjectMapper().readValue(requestBodyInputStream, type);
        } catch (IOException e) {
            throw new ActivityPubException(String.format(ERROR_MSG, type), e);
        }
    }
}
