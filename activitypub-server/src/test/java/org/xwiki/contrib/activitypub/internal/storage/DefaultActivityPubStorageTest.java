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
package org.xwiki.contrib.activitypub.internal.storage;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
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
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonParser;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.ResourceType;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    @Named("relative")
    private ActivityPubJsonParser jsonParser;

    @MockComponent
    @Named("relative")
    private ActivityPubJsonSerializer jsonSerializer;

    @MockComponent
    private ActivityPubObjectReferenceResolver resolver;

    @MockComponent
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @MockComponent
    private Solr solrInstance;

    @MockComponent
    private DefaultURLHandler urlHandler;

    @MockComponent
    private WebfingerJsonParser webfingerJsonParser;

    @MockComponent
    private WebfingerJsonSerializer webfingerJsonSerializer;

    @Mock
    private SolrClient solrClient;

    @MockComponent
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> urlResolver;

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
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
    public void storeEntityWithoutID() throws Exception
    {
        // Test store of an entity without id.
        URI uri = new URI("http://mydomain.org/xwiki/activitypub/object/foo");
        ActivityPubObject object1 = new ActivityPubObject().setName("foo");
        when(this.jsonSerializer.serialize(object1)).thenReturn("{foo}");
        when(this.jsonParser.parse("{foo}")).thenReturn(object1);
        when(this.serializer.serialize(any())).thenReturn(uri);

        URI uid = this.activityPubStorage.storeEntity(object1);
        assertEquals(uid, uri);
        assertEquals(uri, object1.getId());
        ArgumentCaptor<ActivityPubResourceReference> argumentCaptor =
            ArgumentCaptor.forClass(ActivityPubResourceReference.class);
        verify(this.serializer).serialize(argumentCaptor.capture());
        verifySolrPutAndPrepareGet(argumentCaptor.getValue().getUuid(), "{foo}");

        when(this.urlHandler.belongsToCurrentInstance(uri)).thenReturn(true);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.urlHandler.getExtendedURL(uri)).thenReturn(extendedURL);
        when(this.urlResolver.resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(argumentCaptor.getValue());

        assertSame(object1, this.activityPubStorage.retrieveEntity(uid));
    }

    @Test
    public void storeEntityWithIDRemoteInstance() throws Exception
    {
        // Test store of an entity with ID
        URL object2URL = new URL("http://www.xwiki.org/xwiki/activitypub/object/42");
        ActivityPubObject object2 = mock(ActivityPubObject.class);
        when(object2.getId()).thenReturn(object2URL.toURI());
        String content = "{foobar}";
        when(this.jsonSerializer.serialize(object2)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(object2);
        when(this.urlHandler.belongsToCurrentInstance(object2URL.toURI())).thenReturn(false);

        // ID match server URL
        assertEquals(object2URL.toURI(), this.activityPubStorage.storeEntity(object2));
        verifySolrPutAndPrepareGet(object2URL.toString(), content);
        verify(object2).setId(null);
        verify(object2).setId(object2URL.toURI());

        assertSame(object2, this.activityPubStorage.retrieveEntity(object2URL.toURI()));
        verify(object2, times(2)).setId(object2URL.toURI());
    }

    @Test
    public void storeEntityWithIDLocalInstance() throws Exception
    {
        // Test store of an entity with ID
        URL object2URL = new URL("http://www.xwiki.org/xwiki/activitypub/object/42");
        ActivityPubObject object2 = mock(ActivityPubObject.class);
        when(object2.getId()).thenReturn(object2URL.toURI());
        String content = "{foobar}";
        when(this.jsonSerializer.serialize(object2)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(object2);
        when(this.urlHandler.belongsToCurrentInstance(object2URL.toURI())).thenReturn(true);
        ExtendedURL object2ExtendedURL = new ExtendedURL(Arrays.asList("activitypub", "object", "42"));
        when(this.urlHandler.getExtendedURL(object2URL.toURI())).thenReturn(object2ExtendedURL);
        when(this.urlResolver.resolve(object2ExtendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(new ActivityPubResourceReference("object", "42"));

        // ID match server URL
        assertEquals(object2URL.toURI(), this.activityPubStorage.storeEntity(object2));
        verifySolrPutAndPrepareGet("42", content);
        verify(object2).setId(null);
        verify(object2).setId(object2URL.toURI());
        assertSame(object2, this.activityPubStorage.retrieveEntity(object2URL.toURI()));
        verify(object2, times(2)).setId(object2URL.toURI());
    }

    @Test
    public void storeActor() throws Exception
    {
        // Test store of an Actor
        URI uid = new URI("http://domain.org/xwiki/activitypub/Person/xwiki%3AXWiki.FooBar");
        String content = "{user:FooBar}";
        AbstractActor actor = new Person()
            .setPreferredUsername("FooBar")
            .setXwikiReference("wiki:XWiki.FooBar");
        when(this.jsonSerializer.serialize(actor)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(actor);
        ActivityPubResourceReference resourceReference =
            new ActivityPubResourceReference("Person", "wiki:XWiki.FooBar");
        when(this.serializer.serialize(resourceReference)).thenReturn(uid);

        assertEquals(uid, this.activityPubStorage.storeEntity(actor));
        assertEquals(uid, actor.getId());
        verifySolrPutAndPrepareGet("wiki:XWiki.FooBar", content);

        when(this.urlHandler.belongsToCurrentInstance(uid)).thenReturn(true);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.urlHandler.getExtendedURL(uid)).thenReturn(extendedURL);
        when(this.urlResolver.resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(resourceReference);
        assertSame(actor, this.activityPubStorage.retrieveEntity(uid));
    }

    @Test
    public void storeBox() throws Exception
    {
        // Test store of an Inbox
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new Inbox());
        });

        assertEquals("Cannot store an inbox without owner.", activityPubException.getCause().getMessage());

        URI uid = new URI("http://domain.org/xwiki/activitypub/Inbox/xwiki%3AXWiki.FooBar-inbox");
        String content = "{inbox:FooBar}";
        Person actor = new Person()
            .setPreferredUsername("FooBar")
            .setXwikiReference("xwiki:XWiki.FooBar");
        ActivityPubObjectReference<AbstractActor> objectReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(actor);
        when(this.resolver.resolveReference(objectReference)).thenReturn(actor);
        Inbox inbox = new Inbox().setAttributedTo(Collections.singletonList(objectReference));
        when(this.jsonSerializer.serialize(inbox)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(inbox);
        ActivityPubResourceReference resourceReference =
            new ActivityPubResourceReference("Inbox", "xwiki:XWiki.FooBar-inbox");
        when(this.serializer.serialize(resourceReference)).thenReturn(uid);

        assertEquals(uid, this.activityPubStorage.storeEntity(inbox));
        assertEquals(uid, inbox.getId());
        verifySolrPutAndPrepareGet(resourceReference.getUuid(), content);

        when(this.urlHandler.belongsToCurrentInstance(uid)).thenReturn(true);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.urlHandler.getExtendedURL(uid)).thenReturn(extendedURL);
        when(this.urlResolver.resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(resourceReference);
        assertSame(inbox, this.activityPubStorage.retrieveEntity(uid));

        // Test store of an Outbox
        activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new Outbox());
        });
        assertEquals("Cannot store an outbox without owner.", activityPubException.getCause().getMessage());

        uid = new URI("http://domain.org/xwiki/activitypub/Outbox/xwiki%3AXWiki.FooBar-outbox");
        content = "{outbox:FooBar}";
        Outbox outbox = new Outbox().setAttributedTo(Collections.singletonList(objectReference));
        when(this.jsonSerializer.serialize(outbox)).thenReturn(content);
        when(this.jsonParser.parse(content)).thenReturn(outbox);
        resourceReference = new ActivityPubResourceReference("Outbox", "xwiki:XWiki.FooBar-outbox");
        when(this.serializer.serialize(resourceReference)).thenReturn(uid);

        assertEquals(uid, this.activityPubStorage.storeEntity(outbox));
        assertEquals(uid, outbox.getId());
        verifySolrPutAndPrepareGet(resourceReference.getUuid(), content, 2);
        when(this.urlHandler.belongsToCurrentInstance(uid)).thenReturn(true);
        when(this.urlHandler.getExtendedURL(uid)).thenReturn(extendedURL);
        when(this.urlResolver.resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(resourceReference);
        assertSame(outbox, this.activityPubStorage.retrieveEntity(uid));
    }

    @Test
    public void retrieveActor() throws Exception
    {
        Person person = mock(Person.class);
        SolrDocument solrDocument = mock(SolrDocument.class);
        URI uri = URI.create("http://www.xwiki.org/person/foo");
        when(this.urlHandler.belongsToCurrentInstance(uri)).thenReturn(true);
        ExtendedURL extendedURL = mock(ExtendedURL.class);
        when(this.urlHandler.getExtendedURL(uri)).thenReturn(extendedURL);
        ActivityPubResourceReference activityPubResourceReference = new ActivityPubResourceReference("person", "foo");
        when(this.urlResolver.resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(activityPubResourceReference);
        when(this.solrClient.getById("foo")).thenReturn(solrDocument);
        when(solrDocument.isEmpty()).thenReturn(false);
        when(solrDocument.getFieldValue("content")).thenReturn("{person:foo}");
        when(this.jsonParser.parse("{person:foo}")).thenReturn(person);

        // A person from same domain, recently updated is always retrieved.
        when(solrDocument.getFieldValue("id")).thenReturn(uri.toASCIIString());
        when(solrDocument.getFieldValue("type")).thenReturn("person");
        when(solrDocument.getFieldValue("updatedDate")).thenReturn(new Date());
        assertSame(person, this.activityPubStorage.retrieveEntity(uri));

        // If it comes from another domain but it's recent enough it's still retrieved.
        when(solrDocument.getFieldValue("id")).thenReturn("http://anotherdomain.fr/person/foo");
        assertSame(person, this.activityPubStorage.retrieveEntity(uri));

        // Now it comes from another domain but it's two days old: we don't retrieve it.
        Date oldDate = DateUtils.addDays(new Date(), -2);
        when(solrDocument.getFieldValue("updatedDate")).thenReturn(oldDate);
        assertNull(this.activityPubStorage.retrieveEntity(uri));

        // We consider it back on the same domain, even if two days old we retrieve it.
        when(solrDocument.getFieldValue("id")).thenReturn(uri.toASCIIString());
        assertSame(person, this.activityPubStorage.retrieveEntity(uri));

        // Now if it comes from another domain but it's not a person, we retrieve the data, even if it's old.
        when(solrDocument.getFieldValue("id")).thenReturn("http://anotherdomain.fr/person/foo");
        when(solrDocument.getFieldValue("type")).thenReturn("something");
        assertSame(person, this.activityPubStorage.retrieveEntity(uri));
    }

    @Test
    public void storeWebfinger() throws Exception
    {
        JSONResourceDescriptor jsonResourceDescriptor = mock(JSONResourceDescriptor.class);
        String subject = "acct:foo@xwiki.org";
        String content = "{webfinger:foo}";
        when(jsonResourceDescriptor.getSubject()).thenReturn(subject);
        when(this.webfingerJsonSerializer.serialize(jsonResourceDescriptor)).thenReturn(content);

        this.activityPubStorage.storeWebFinger(jsonResourceDescriptor);
        ArgumentCaptor<SolrInputDocument> argumentCaptor =
            ArgumentCaptor.forClass(SolrInputDocument.class);
        verify(this.solrClient).add(argumentCaptor.capture());
        assertEquals(subject, argumentCaptor.getValue().getFieldValue("id"));
        assertEquals(content, argumentCaptor.getValue().getFieldValue("content"));
        assertEquals("webfinger", argumentCaptor.getValue().getFieldValue("type"));
    }

    @Test
    public void searchWebfinger() throws Exception
    {
        String queryParam = "foo";
        int limit = 42;
        String queryString = "filter(type:webfinger) AND id:*foo*";
        SolrQuery solrQuery = new SolrQuery("*").addFilterQuery(queryString).setRows(limit);
        SolrDocumentList solrDocumentList = mock(SolrDocumentList.class);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(this.solrClient.query(any())).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        SolrDocument document1 = mock(SolrDocument.class);
        SolrDocument document2 = mock(SolrDocument.class);
        when(solrDocumentList.iterator()).thenReturn(Arrays.asList(document1, document2).iterator());
        String content1 = "{foo1}";
        String content2 = "{foo2}";
        when(document1.getFieldValue("content")).thenReturn(content1);
        when(document2.getFieldValue("content")).thenReturn(content2);
        JSONResourceDescriptor json1 = mock(JSONResourceDescriptor.class);
        JSONResourceDescriptor json2 = mock(JSONResourceDescriptor.class);
        when(this.webfingerJsonParser.parse(content1)).thenReturn(json1);
        when(this.webfingerJsonParser.parse(content2)).thenReturn(json2);
        assertEquals(Arrays.asList(json1, json2), this.activityPubStorage.searchWebFinger(queryParam, limit));
        ArgumentCaptor<SolrQuery> argumentCaptor = ArgumentCaptor.forClass(SolrQuery.class);
        verify(this.solrClient).query(argumentCaptor.capture());
        assertEquals(solrQuery.toString(), argumentCaptor.getValue().toString());
    }

    @Test
    public void query() throws Exception
    {
        String query = "wikiReference:\"xwiki:MySpace.MyDoc\"";
        int limit = 42;
        SolrQuery solrQuery = new SolrQuery("*")
            .addFilterQuery("filter(type:Document)")
            .addFilterQuery(query)
            .setRows(limit);
        Document document = mock(Document.class);
        SolrDocumentList solrDocumentList = mock(SolrDocumentList.class);
        QueryResponse queryResponse = mock(QueryResponse.class);
        when(this.solrClient.query(any())).thenReturn(queryResponse);
        when(queryResponse.getResults()).thenReturn(solrDocumentList);
        SolrDocument document1 = mock(SolrDocument.class);
        when(solrDocumentList.iterator()).thenReturn(Arrays.asList(document1).iterator());
        String content1 = "{doc1}";
        when(document1.getFieldValue("content")).thenReturn(content1);
        when(this.jsonParser.parse(content1)).thenReturn(document);
        assertEquals(Collections.singletonList(document), this.activityPubStorage.query(Document.class, query, limit));
        ArgumentCaptor<SolrQuery> argumentCaptor = ArgumentCaptor.forClass(SolrQuery.class);
        verify(this.solrClient).query(argumentCaptor.capture());
        assertEquals(solrQuery.toString(), argumentCaptor.getValue().toString());
    }
}
