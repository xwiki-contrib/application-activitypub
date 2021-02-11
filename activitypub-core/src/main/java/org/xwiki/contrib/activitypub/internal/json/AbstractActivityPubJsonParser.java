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

import org.slf4j.Logger;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Abstract implementation of {@link ActivityPubJsonParser}.
 * This class provides almost everything to for parsing, it just needs an {@link ObjectMapper} to work, which is
 * generally provided by {@link ObjectMapperConfiguration}.
 *
 * @version $Id$
 * @since 1.2
 */
public abstract class AbstractActivityPubJsonParser implements ActivityPubJsonParser
{
    /**
     * Logger injection key.
     */
    public static final String LOGGER_KEY = "logger";

    private static final String ERROR_MSG_KNOWN_TYPE = "Error while parsing request with type [%s].";

    private static final String ERROR_MSG_UNKNOWN_TYPE = "Error while parsing request with unknown type.";

    @Inject
    private Logger logger;

    /**
     * Retrieve an object mapper for the parsing operations: this is generally provided by
     * a {@link ObjectMapperConfiguration}.
     *
     * @return the object mapper to be used for the parsing.
     */
    public abstract ObjectMapper getObjectMapper();

    @Override
    public <T extends ActivityPubObject> T parse(String requestBody) throws ActivityPubException
    {
        return (T) parse(requestBody, ActivityPubObject.class);
    }

    @Override
    public <T extends ActivityPubObject> T parse(String requestBody, Class<T> type) throws ActivityPubException
    {
        try {
            return getInitializedReader().readValue(requestBody, type);
        } catch (IOException e) {
            throw new ActivityPubException(String.format(ERROR_MSG_KNOWN_TYPE, type), e);
        } catch (RuntimeException e) {
            throw new ActivityPubException(ERROR_MSG_UNKNOWN_TYPE, e);
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
            return getInitializedReader().readValue(requestBodyReader, type);
        } catch (IOException e) {
            throw new ActivityPubException(String.format(ERROR_MSG_KNOWN_TYPE, type), e);
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
            return getInitializedReader().readValue(requestBodyInputStream, type);
        } catch (IOException e) {
            throw new ActivityPubException(String.format(ERROR_MSG_KNOWN_TYPE, type), e);
        }
    }

    private ObjectMapper getInitializedReader()
    {
        ObjectMapper objectMapper = this.getObjectMapper();
        objectMapper.setInjectableValues(new InjectableValues.Std().addValue(LOGGER_KEY, this.logger));
        return objectMapper;
    }
}
