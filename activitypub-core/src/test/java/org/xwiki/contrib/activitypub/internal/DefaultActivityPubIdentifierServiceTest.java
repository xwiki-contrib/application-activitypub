package org.xwiki.contrib.activitypub.internal;/*
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

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultActivityPubIdentifierService}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
class DefaultActivityPubIdentifierServiceTest
{
    @InjectMockComponents
    private DefaultActivityPubIdentifierService defaultActivityPubIdentifierService;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Test
    void getWebfingerIdentifierPerson() throws Exception
    {
        Person person = mock(Person.class);
        DocumentReference documentReference = mock(DocumentReference.class);

        when(person.toString()).thenReturn("Foo");
        String currentWikiId = "foobar";
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(currentWikiId);
        WikiReference wikiReference = new WikiReference(currentWikiId);
        when(documentReference.getWikiReference()).thenReturn(wikiReference);

        URI personId = URI.create("http://www.xwiki.org/person/foo");
        when(person.getId()).thenReturn(personId);
        when(person.getPreferredUsername()).thenReturn("xwiki:XWiki.Foo");
        assertEquals("Foo", this.defaultActivityPubIdentifierService.createIdentifier(person, "Foo",
            "foobar"));

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");
        assertEquals("Foo.foobar", this.defaultActivityPubIdentifierService.createIdentifier(person,
            "Foo", "foobar"));
    }

    @Test
    void getWebfingerIdentifierService() throws Exception
    {
        Service service = mock(Service.class);
        URI serviceId = URI.create("http://www.xwiki.org/service/foobar");
        String currentWikiId = "foobar";

        when(service.getId()).thenReturn(serviceId);
        when(service.toString()).thenReturn("Service Foo");
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn(currentWikiId);
        assertEquals("xwiki.xwiki", this.defaultActivityPubIdentifierService.createIdentifier(service,
            null, "xwiki"));

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("xwiki");
        assertEquals("foobar.xwiki", this.defaultActivityPubIdentifierService.createIdentifier(service,
            null, "foobar"));
    }
}