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
package org.xwiki.contrib.activitypub.entities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.stream.Collectors;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.internal.json.ActivityPubObjectReferenceSerializer;
import org.xwiki.contrib.activitypub.internal.json.DefaultActivityPubJsonParser;
import org.xwiki.contrib.activitypub.internal.json.DefaultActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.internal.json.ObjectMapperConfiguration;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Utility class to use the actual ObjectMapperConfiguration and Parser/Serializer components to perform
 * Serialization/Parsing of the entities.
 *
 *
 * @since 1.0
 * @version $Id$
 */
@ComponentTest
@ComponentList({ ObjectMapperConfiguration.class, ActivityPubObjectReferenceSerializer.class })
public class AbstractEntityTest
{
    @InjectMockComponents
    protected DefaultActivityPubJsonParser parser;

    @InjectMockComponents
    protected DefaultActivityPubJsonSerializer serializer;

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerMockComponent(ActivityPubStorage.class);
        ResourceReferenceSerializer<ActivityPubResourceReference, URI> activityPubResourceReferenceSerializerMock =
            componentManager.registerMockComponent(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
                ActivityPubResourceReference.class, URI.class));

        when(activityPubResourceReferenceSerializerMock.serialize(any())).thenAnswer(invocationOnMock -> {
            ActivityPubResourceReference resourceReference = invocationOnMock.getArgument(0);
            String serialized = String.format("http://www.xwiki.org/xwiki/%s/%s",
                resourceReference.getEntityType(), resourceReference.getUuid());
            return new URI(serialized);
        });
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    protected String readResource(String entityPath) throws FileNotFoundException
    {
        File newPath = new File(new File("./src/test/resources/entities"), entityPath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(newPath));
        return bufferedReader.lines().collect(Collectors.joining("\n"));
    }
}
