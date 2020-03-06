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
package org.xwiki.contrib.activitypub.webfinger.entities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.webfinger.internal.json.DefaultWebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.internal.json.ObjectMapperConfiguration;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

/**
 *
 * Test of {@link WebfingerJRD} serialization.
 *
 * @since 1.1
 * @version $Id$
 */
@ComponentTest
@ComponentList({ ObjectMapperConfiguration.class })
class WebfingerJRDTest
{
    @InjectMockComponents
    private DefaultWebfingerJsonSerializer serializer;

    @Test
    void serialize() throws Exception
    {

        LinkJRD l1 = new LinkJRD().setRel("http://link1rel.tst").setHref("http://link1.txt").setType("self");
        LinkJRD l2 = new LinkJRD().setRel("http://link2rel.tst").setHref("http://link2.txt").setType("self");
        WebfingerJRD webfinger = new WebfingerJRD()
                                     .setSubject("acct:test@xwiki.tst")
                                     .setLinks(Arrays.asList(l1, l2));
        String actual = this.serializer.serialize(webfinger);
        String excepted = this.readResource("webfinger.json");
        Assertions.assertEquals(excepted, actual);
    }

    protected String readResource(String entityPath) throws FileNotFoundException
    {
        File newPath = new File(new File("./src/test/resources"), entityPath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(newPath));
        return bufferedReader.lines().collect(Collectors.joining("\n"));
    }
}