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
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.internal.json.ActivityPubObjectReferenceSerializer;
import org.xwiki.contrib.activitypub.internal.json.DefaultActivityPubJsonParser;
import org.xwiki.contrib.activitypub.internal.json.DefaultActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.internal.json.ObjectMapperConfiguration;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Utility class to use the actual ObjectMapperConfiguration and Parser/Serializer components to perform
 * Serialization/Parsing of the entities.
 */
@ComponentTest
@ComponentList({ObjectMapperConfiguration.class})
public class AbstractEntityTest
{
    @MockComponent
    protected ActivityPubObjectReferenceSerializer activityPubObjectReferenceSerializer;

    @InjectMockComponents
    protected DefaultActivityPubJsonParser parser;

    @InjectMockComponents
    protected DefaultActivityPubJsonSerializer serializer;

    protected String readResource(String entityPath) throws FileNotFoundException
    {
        File newPath = new File(new File("./src/test/resources/entities"), entityPath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(newPath));
        return bufferedReader.lines().collect(Collectors.joining("\n"));
    }
}
