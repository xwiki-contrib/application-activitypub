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
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.internal.json.DefaultWebfingerJsonParser;
import org.xwiki.contrib.activitypub.webfinger.internal.json.DefaultWebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.internal.json.ObjectMapperConfiguration;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * Test of {@link JSONResourceDescriptor} serialization.
 *
 * @since 1.1
 * @version $Id$
 */
@ComponentTest
@ComponentList({ ObjectMapperConfiguration.class })
class JSONResourceDescriptorTest
{
    @InjectMockComponents
    private DefaultWebfingerJsonSerializer serializer;

    @InjectMockComponents
    private DefaultWebfingerJsonParser parser;

    @Test
    void parseStringEmptyValid() throws Exception
    {
        JSONResourceDescriptor actual = this.parser.parse("{}");
        assertEquals(new JSONResourceDescriptor(), actual);
    }

    @Test
    void parseStringValid() throws Exception
    {
        JSONResourceDescriptor actual = this.parser.parse(this.readResource("webfinger.json"));
        assertEquals(new JSONResourceDescriptor().setSubject("acct:test@xwiki.tst").setLinks(Arrays.asList(
            new Link().setHref(URI.create("http://link1.txt")).setRel("http://link1rel.tst").setType("self"),
            new Link().setHref(URI.create("http://link2.txt")).setRel("http://link2rel.tst").setType("self"))), actual);
    }

    @Test
    void parseStringValidUnknownField() throws Exception
    {
        JSONResourceDescriptor actual = this.parser.parse(this.readResource("webfinger_unknown_field.json"));
        assertEquals(new JSONResourceDescriptor().setSubject("acct:test@xwiki.tst").setLinks(Arrays.asList(
            new Link().setHref(URI.create("http://link1.txt")).setRel("http://link1rel.tst").setType("self"),
            new Link().setHref(URI.create("http://link2.txt")).setRel("http://link2rel.tst").setType("self"))), actual);
    }

    @Test
    void parseStringInvalid() throws Exception
    {
        WebfingerException expt = assertThrows(WebfingerException.class, () -> this.parser.parse("{"));
        assertEquals("Error while parsing a JSONResourceDescriptor", expt.getMessage());
    }

    @Test
    void parseReaderInvalid() throws Exception
    {
        WebfingerException expt =
            assertThrows(WebfingerException.class, () -> this.parser.parse(new StringReader("{")));
        assertEquals("Error while parsing a JSONResourceDescriptor", expt.getMessage());
    }

    @Test
    void parseReaderEmptyValid() throws Exception
    {
        JSONResourceDescriptor actual = this.parser.parse(new StringReader("{}"));
        assertEquals(new JSONResourceDescriptor(), actual);
    }

    @Test
    void parseReaderValid() throws Exception
    {
        JSONResourceDescriptor actual = this.parser.parse(new StringReader(this.readResource("webfinger.json")));
        assertEquals(new JSONResourceDescriptor().setSubject("acct:test@xwiki.tst").setLinks(Arrays.asList(
            new Link().setHref(URI.create("http://link1.txt")).setRel("http://link1rel.tst").setType("self"),
            new Link().setHref(URI.create("http://link2.txt")).setRel("http://link2rel.tst").setType("self"))), actual);
    }

    @Test
    void parseInputStreamInvalid() throws Exception
    {
        InputStream inputStream = IOUtils.toInputStream("{", "UTF-8");
        WebfingerException expt =
            assertThrows(WebfingerException.class, () -> this.parser.parse(inputStream));
        assertEquals("Error while parsing a JSONResourceDescriptor", expt.getMessage());
    }

    @Test
    void parseInputStreamEmptyValid() throws Exception
    {
        InputStream inputStream = IOUtils.toInputStream("{}", "UTF-8");
        JSONResourceDescriptor actual = this.parser.parse(inputStream);
        assertEquals(new JSONResourceDescriptor(), actual);
    }

    @Test
    void parseInputStreamValid() throws Exception
    {
        InputStream inputStream = IOUtils.toInputStream(this.readResource("webfinger.json"), "UTF-8");
        JSONResourceDescriptor actual = this.parser.parse(inputStream);
        assertEquals(new JSONResourceDescriptor().setSubject("acct:test@xwiki.tst").setLinks(Arrays.asList(
            new Link().setHref(URI.create("http://link1.txt")).setRel("http://link1rel.tst").setType("self"),
            new Link().setHref(URI.create("http://link2.txt")).setRel("http://link2rel.tst").setType("self"))), actual);
    }

    @Test
    void serialize() throws Exception
    {

        Link l1 = new Link().setRel("http://link1rel.tst").setHref(URI.create("http://link1.txt")).setType("self");
        Link l2 = new Link().setRel("http://link2rel.tst").setHref(URI.create("http://link2.txt")).setType("self");
        JSONResourceDescriptor webfinger = new JSONResourceDescriptor()
                                               .setSubject("acct:test@xwiki.tst")
                                               .setLinks(Arrays.asList(l1, l2));
        String actual = this.serializer.serialize(webfinger);
        String excepted = this.readResource("webfinger.json");
        assertEquals(excepted, actual);
    }

    private String readResource(String entityPath) throws FileNotFoundException
    {
        File newPath = new File(new File("./src/test/resources"), entityPath);
        BufferedReader bufferedReader = new BufferedReader(new FileReader(newPath));
        return bufferedReader.lines().collect(Collectors.joining("\n"));
    }
}