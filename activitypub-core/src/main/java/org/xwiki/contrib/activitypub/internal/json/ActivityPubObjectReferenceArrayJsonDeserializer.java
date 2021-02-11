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
import java.util.ArrayList;
import java.util.List;

import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Deserialize an array of {@link ActivityPubObjectReference} while ignoring the objects with types currently not
 * handled by our implementation.
 *
 * @version $Id$
 * @since 1.7.1
 */
public class ActivityPubObjectReferenceArrayJsonDeserializer extends JsonDeserializer<List<ActivityPubObjectReference>>
{
    @Override
    public List<ActivityPubObjectReference> deserialize(JsonParser jsonParser, DeserializationContext ctxt)
        throws IOException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        TreeNode treeNode = mapper.readTree(jsonParser);

        if (treeNode instanceof ArrayNode) {
            List<ActivityPubObjectReference> activityPubObjectReferences = new ArrayList<>();
            ArrayNode node = (ArrayNode) treeNode;
            for (JsonNode jsonNode : node) {
                ActivityPubObjectReference activityPubObjectReference =
                    mapper.treeToValue(jsonNode, ActivityPubObjectReference.class);
                if (activityPubObjectReference != null) {
                    activityPubObjectReferences.add(activityPubObjectReference);
                }
            }
            if (activityPubObjectReferences.isEmpty()) {
                return null;
            } else {
                return activityPubObjectReferences;
            }
        } else {
            return null;
        }
    }
}
