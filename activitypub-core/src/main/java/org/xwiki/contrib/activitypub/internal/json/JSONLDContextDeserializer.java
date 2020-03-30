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
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.entities.JSONLDContext;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * A custom json deserializer for {@link JSONLDContext} fields.
 * @since 1.1
 * @version $Id$
 */
@Component(roles = JSONLDContextDeserializer.class)
@Singleton
public class JSONLDContextDeserializer extends JsonDeserializer<JSONLDContext>
{
    @Inject
    private Logger logger;

    @Override
    public JSONLDContext deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException
    {
        JSONLDContext ret = new JSONLDContext();
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();

        if (jsonParser.currentToken() == JsonToken.START_ARRAY) {
            ArrayNode lst = mapper.readValue(jsonParser, ArrayNode.class);
            for (JsonNode e : lst) {
                if (e instanceof TextNode) {
                    ret.add(URI.create(e.asText()));
                } else {
                    this.logger.info("The JsonNode [{}] has been ignored.", e);
                }
            }
        } else {
            ret.add(mapper.readValue(jsonParser, URI.class));
        }
        return ret;
    }
}
