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
package org.xwiki.contrib.activitypub.internal;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.internal.api.SolrInstance;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import liquibase.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultActivityPubStorage}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultActivityPubStorageTest
{
    private static final String DEFAULT_URL = "http://www.xwiki.org";

    @InjectMockComponents
    private DefaultActivityPubStorage activityPubStorage;

    @MockComponent
    private ActivityPubJsonParser jsonParser;

    @MockComponent
    private ActivityPubJsonSerializer jsonSerializer;

    @MockComponent
    private ActivityPubObjectReferenceResolver resolver;

    @MockComponent
    private Solr solrInstance;

    @Mock
    private SolrClient solrClient;

    @MockComponent
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> urlResolver;

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        DefaultURLHandler urlHandler = componentManager.registerMockComponent(DefaultURLHandler.class);
        when(urlHandler.getServerUrl()).thenReturn(new URL(DEFAULT_URL));
    }

    @BeforeEach
    public void beforeEach() throws SolrException
    {
        when(this.solrInstance.getClient("activitypub")).thenReturn(solrClient);
    }

    private void verifySolrPutAndPrepareGet(String uid, String content) throws IOException, SolrServerException
    {
        this.verifySolrPutAndPrepareGet(uid, content, 1);
    }

    private void verifySolrPutAndPrepareGet(String uid, String content, int timeNumber) throws IOException, SolrServerException
    {
        ArgumentCaptor<SolrInputDocument> argumentCaptor =
            ArgumentCaptor.forClass(SolrInputDocument.class);
        verify(this.solrClient, times(timeNumber)).add(argumentCaptor.capture());
        assertEquals(uid, argumentCaptor.getValue().getFieldValue("id"));
        assertEquals(content, argumentCaptor.getValue().getFieldValue("content"));
        verify(this.solrClient, times(timeNumber)).commit();

        Map<String, Object> fields = new HashMap<>();
        fields.put("id", uid);
        fields.put("content", content);
        SolrDocument solrDocument = new SolrDocument(fields);
        when(this.solrClient.getById(uid)).thenReturn(solrDocument);
    }

    @Test
    public void storeAndRetrieveEntityWithUID() throws Exception
    {
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity("", new ActivityPubObject());
        });

        assertEquals("The UID cannot be empty.", activityPubException.getMessage());

        activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity((String)null, new ActivityPubObject());
        });

        assertEquals("The UID cannot be empty.", activityPubException.getMessage());

        assertNull(this.activityPubStorage.retrieveEntity("foo"));

        ActivityPubObject object1 = new ActivityPubObject().setName("foo");
        ActivityPubObject object2 = new ActivityPubObject().setName("foobar");
        when(this.jsonSerializer.serialize(object1)).thenReturn("{foo}");
        when(this.jsonSerializer.serialize(object2)).thenReturn("{foobar}");
        when(this.jsonParser.parse("{foo}")).thenReturn(object1);
        when(this.jsonParser.parse("{foobar}")).thenReturn(object2);

        assertFalse(this.activityPubStorage.storeEntity("foo", object1));
        verifySolrPutAndPrepareGet("foo", "{foo}");
        assertSame(object1, this.activityPubStorage.retrieveEntity("foo"));

        assertTrue(this.activityPubStorage.storeEntity("foo", object2));
        verifySolrPutAndPrepareGet("foo", "{foobar}", 2);
        assertSame(object2, this.activityPubStorage.retrieveEntity("foo"));
    }

    @Test
    public void storeEntityWithoutID() throws Exception
    {
        // Test store of an entity without id.
        ActivityPubObject object1 = new ActivityPubObject().setName("foo");
        when(this.jsonSerializer.serialize(object1)).thenReturn("{foo}");
        when(this.jsonParser.parse("{foo}")).thenReturn(object1);

        String uid = this.activityPubStorage.storeEntity(object1);
        assertFalse(StringUtils.isEmpty(uid));
        verifySolrPutAndPrepareGet(uid, "{foo}");
        assertSame(object1, this.activityPubStorage.retrieveEntity(uid));
    }

    @Test
    public void storeEntityWithID() throws Exception
    {
        // Test store of an entity with ID
        URL object2URL = new URL("http://www.xwiki.org/xwiki/activitypub/object/42");
        ActivityPubObject object2 = new ActivityPubObject()
            .setName("foobar")
            .setId(object2URL.toURI());
        String uid = "42";
        String content = "{foobar}";
        when(this.jsonSerializer.serialize(object2)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(object2);
        ExtendedURL object2ExtendedURL = new ExtendedURL(object2URL, "xwiki/activitypub");
        when(this.urlResolver.resolve(object2ExtendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(new ActivityPubResourceReference("object", "42"));

        // ID does not match server URL
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new ActivityPubObject().setId(new URI("http://anotherwiki.org/12")));
        });
        assertEquals("Entity [http://anotherwiki.org/12] won't be stored since it's not part of the current instance",
            activityPubException.getMessage());

        // ID match server URL
        assertEquals(uid, this.activityPubStorage.storeEntity(object2));
        verifySolrPutAndPrepareGet(uid, content);
        assertSame(object2, this.activityPubStorage.retrieveEntity(uid));
    }

    @Test
    public void storeActor() throws Exception
    {
        // Test store of an Actor
        String uid = "FooBar";
        String content = "{user:FooBar}";
        AbstractActor actor = new Person().setPreferredUsername(uid);
        when(this.jsonSerializer.serialize(actor)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(actor);

        assertEquals(uid, this.activityPubStorage.storeEntity(actor));
        verifySolrPutAndPrepareGet(uid, content);
        assertSame(actor, this.activityPubStorage.retrieveEntity("FooBar"));
    }

    @Test
    public void storeBox() throws Exception
    {
        // Test store of an Inbox
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new Inbox());
        });

        assertEquals("Cannot store an inbox without owner.", activityPubException.getMessage());

        String uid = "FooBar-inbox";
        String content = "{inbox:FooBar}";
        Person actor = new Person().setPreferredUsername("FooBar");
        ActivityPubObjectReference<AbstractActor> objectReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(actor);
        when(this.resolver.resolveReference(objectReference)).thenReturn(actor);
        Inbox inbox = new Inbox().setAttributedTo(Collections.singletonList(objectReference));
        when(this.jsonSerializer.serialize(inbox)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(inbox);

        assertEquals(uid, this.activityPubStorage.storeEntity(inbox));
        verifySolrPutAndPrepareGet(uid, content);
        assertSame(inbox, this.activityPubStorage.retrieveEntity(uid));

        // Test store of an Outbox
        activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new Outbox());
        });
        assertEquals("Cannot store an outbox without owner.", activityPubException.getMessage());

        uid = "FooBar-outbox";
        content = "{outbox:FooBar}";
        Outbox outbox = new Outbox().setAttributedTo(Collections.singletonList(objectReference));
        when(this.jsonSerializer.serialize(outbox)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(outbox);
        assertEquals(uid, this.activityPubStorage.storeEntity(outbox));
        verifySolrPutAndPrepareGet(uid, content, 2);
        assertSame(outbox, this.activityPubStorage.retrieveEntity(uid));
    }
}
