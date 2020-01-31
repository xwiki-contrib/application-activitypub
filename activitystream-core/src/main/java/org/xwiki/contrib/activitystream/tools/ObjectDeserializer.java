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
package org.xwiki.contrib.activitystream.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xwiki.contrib.activitystream.entities.Object;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ObjectDeserializer extends JsonDeserializer<Object>
{
    private static final List<String> packageExtensions = new ArrayList<>();
    private static final String PACKAGE_FALLBACK = "org.xwiki.contrib.activitystream.entities";

    public static void registerPackageExtension(String packageExtension)
    {
        packageExtensions.add(packageExtension);
    }

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException, JsonProcessingException
    {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = (ObjectNode) mapper.readTree(jsonParser);

        JsonNode type = root.findValue("type");
        Class<? extends Object> instanceClass;
        if (type != null) {
            instanceClass = findClass(type.asText());
        } else {
            instanceClass = Object.class;
        }
        return mapper.treeToValue(root, instanceClass);
    }

    private Class<? extends Object> findClass(String type)
    {
        Class<? extends Object> classInPackage;
        for (String packageExtension : packageExtensions) {
            classInPackage = findClassInPackage(packageExtension, type);
            if (classInPackage != null) {
                return classInPackage;
            }
        }
        classInPackage = findClassInPackage(PACKAGE_FALLBACK, type);
        if (classInPackage == null) {
            classInPackage = Object.class;
        }
        return classInPackage;
    }

    private Class<? extends Object> findClassInPackage(String packageName, String type)
    {
        String fqn = String.format("%s.%s", packageName, type);
        try {
            return (Class<? extends Object>) getClass().getClassLoader().loadClass(fqn);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
