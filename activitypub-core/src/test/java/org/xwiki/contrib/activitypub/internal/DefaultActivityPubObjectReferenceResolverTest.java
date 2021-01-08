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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultActivityPubObjectReferenceResolver}.
 *
 * @since 1.0
 * @version $Id$
 */
@ComponentTest
class DefaultActivityPubObjectReferenceResolverTest
{
    @InjectMockComponents
    private DefaultActivityPubObjectReferenceResolver resolver;

    @MockComponent
    private DateProvider dateProvider;

    @MockComponent
    private ActivityPubJsonParser activityPubJsonParser;

    @MockComponent
    private Provider<ActivityPubClient> activityPubClientProvider;

    @Mock
    private ActivityPubClient activityPubClient;

    @MockComponent
    private Provider<ActivityPubStorage> activityPubStorageProvider;

    @Mock
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private DefaultURLHandler defaultURLHandler;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @BeforeEach
    public void setup() throws Exception
    {
        when(this.activityPubStorageProvider.get()).thenReturn(this.activityPubStorage);
        when(this.activityPubClientProvider.get()).thenReturn(this.activityPubClient);
    }

    @Test
    void resolveReferenceInvalidLink()
    {
        Executable executable =
            () -> this.resolver.resolveReference(new ActivityPubObjectReference<>());
        ActivityPubException e = assertThrows(ActivityPubException.class, executable);
        assertEquals("The reference property is null and does not have any ID to follow.", e.getMessage());
    }

    @Test
    void resolveReferenceActivityPubException() throws Exception
    {
        when(this.defaultURLHandler.belongsToCurrentInstance(URI.create("http://person/1"))).thenReturn(false);
        HttpMethod httpMethod = mock(HttpMethod.class);
        when(this.activityPubClient.get(URI.create("http://person/1"))).thenReturn(httpMethod);
        doThrow(new ActivityPubException("")).when(this.activityPubClient).checkAnswer(httpMethod);

        Person person = new Person().setId(URI.create("http://person/1")).setLastUpdated(new Date());
        when(this.dateProvider.isElapsed(any(), any(), anyInt())).thenReturn(true);

        ActivityPubObject actual = this.resolver.resolveReference(new ActivityPubObjectReference<>().setObject(person));

        assertEquals(actual, person);
        verify(httpMethod).releaseConnection();
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void resolveReferenceWithObject() throws Exception
    {
        ActivityPubObject object = mock(ActivityPubObject.class);
        assertSame(object, this.resolver
            .resolveReference(new ActivityPubObjectReference<>().setObject(object)));
        verify(this.activityPubClient, never()).get(any());
        verify(this.activityPubStorage, never()).retrieveEntity(any());
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void resolveReferenceObjectNullNotStored() throws Exception
    {
        Accept t = new Accept();
        HttpMethod hm = mock(HttpMethod.class);
        when(hm.getResponseBodyAsString()).thenReturn("{accept}");
        URI uri = URI.create("http://test/create/1");
        ActivityPubObjectReference<ActivityPubObject> reference = new ActivityPubObjectReference<>().setLink(uri);
        when(this.activityPubClient.get(uri)).thenReturn(hm);
        when(this.activityPubJsonParser.parse("{accept}")).thenReturn(t);
        assertSame(t, this.resolver.resolveReference(reference));
        assertSame(t, reference.getObject());
        verify(this.activityPubStorage).retrieveEntity(uri);
        verify(this.activityPubStorage).storeEntity(t);
    }

    @Test
    void resolveReferenceObjectNullStored() throws Exception
    {
        Accept t = new Accept();
        URI uri = URI.create("http://test/create/1");
        ActivityPubObjectReference<ActivityPubObject> reference = new ActivityPubObjectReference<>().setLink(uri);
        when(this.activityPubStorage.retrieveEntity(uri)).thenReturn(t);
        assertSame(t, this.resolver.resolveReference(reference));
        assertSame(t, reference.getObject());
        verify(this.activityPubClient, never()).get(uri);
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void resolveReferenceNetworkError() throws Exception
    {
        when(this.activityPubClient.get(any())).thenThrow(new IOException(""));
        ActivityPubObjectReference<ActivityPubObject> reference =
            new ActivityPubObjectReference<>().setLink(URI.create("http://test/create/1"));
        ActivityPubException e = assertThrows(ActivityPubException.class,
            () -> this.resolver.resolveReference(reference));
        assertEquals("Error when retrieving the ActivityPub information from [http://test/create/1]", e.getMessage());
    }

    @Test
    void resolveTargetsWithComputedTargets() throws Exception
    {
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        Set<AbstractActor> result = Collections.singleton(mock(AbstractActor.class));
        when(activityPubObject.getComputedTargets()).thenReturn(result);
        assertEquals(result, this.resolver.resolveTargets(activityPubObject));
        verify(this.activityPubClient, never()).get(any());
        verify(this.activityPubStorage, never()).retrieveEntity(any());
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void resolveTargetsNotComputed() throws Exception
    {
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        when(activityPubObject.getComputedTargets()).thenReturn(null);

        URI followersURI = URI.create("http://foo/followers");
        URI actorURI1 = URI.create("http://actor1");
        URI actorURI2 = URI.create("http://actor2");
        URI actorURI3 = URI.create("http://actor3");
        AbstractActor actor1 = mock(AbstractActor.class);
        AbstractActor actor2 = mock(AbstractActor.class);
        AbstractActor actor3 = mock(AbstractActor.class);
        when(actor1.getReference()).thenReturn(new ActivityPubObjectReference<>().setLink(actorURI1));
        when(actor2.getReference()).thenReturn(new ActivityPubObjectReference<>().setLink(actorURI2));
        when(actor3.getReference()).thenReturn(new ActivityPubObjectReference<>().setLink(actorURI3));
        when(this.activityPubStorage.retrieveEntity(actorURI1)).thenReturn(actor1);
        when(this.activityPubStorage.retrieveEntity(actorURI2)).thenReturn(actor2);
        when(this.activityPubStorage.retrieveEntity(actorURI3)).thenReturn(actor3);

        OrderedCollection<AbstractActor> followers = mock(OrderedCollection.class);
        when(this.activityPubStorage.retrieveEntity(followersURI)).thenReturn(followers);
        ActivityPubObjectReference<AbstractActor> reference2 = actor2.getReference();
        ActivityPubObjectReference<AbstractActor> reference3 = actor3.getReference();
        when(followers.getAllItems()).thenReturn(Arrays.asList(reference2, reference3));

        when(activityPubObject.getTo()).thenReturn(Arrays.asList(
            ProxyActor.getPublicActor(),
            new ProxyActor(actorURI1),
            new ProxyActor(followersURI),
            new ProxyActor(actorURI2)
        ));
        Set<AbstractActor> expectedSet = new HashSet<>();
        expectedSet.add(actor1);
        expectedSet.add(actor2);
        expectedSet.add(actor3);
        assertEquals(expectedSet, this.resolver.resolveTargets(activityPubObject));
    }

    @Test
    void resolveTargetsEmpty()
    {
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        when(activityPubObject.getComputedTargets()).thenReturn(null);
        assertEquals(Collections.emptySet(),
            this.resolver.resolveTargets(activityPubObject));
    }

    @Test
    void shouldBeRefreshed()
    {
        Person person = new Person();
        assertFalse(this.resolver.shouldBeRefreshed(person));

        when(this.dateProvider.isElapsed(any(), any(), anyInt())).thenReturn(true);
        URI currentUri = URI.create("http://foo");
        person.setId(currentUri);
        when(this.defaultURLHandler.belongsToCurrentInstance(currentUri)).thenReturn(false);
        Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
        person.setLastUpdated(twoDaysAgo);
        assertTrue(this.resolver.shouldBeRefreshed(person));

        when(this.defaultURLHandler.belongsToCurrentInstance(currentUri)).thenReturn(true);
        assertFalse(this.resolver.shouldBeRefreshed(person));

        when(this.dateProvider.isElapsed(any(), any(), anyInt())).thenReturn(false);
        when(this.defaultURLHandler.belongsToCurrentInstance(currentUri)).thenReturn(false);
        person.setLastUpdated(new Date());
        assertFalse(this.resolver.shouldBeRefreshed(person));
    }

    @Test
    void resolveReferenceObjectNullStoredButNeedRefresh() throws Exception
    {
        URI uri = URI.create("http://test/person/1");
        Person person = new Person();
        person.setId(uri);
        Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
        person.setLastUpdated(twoDaysAgo);

        HttpMethod hm = mock(HttpMethod.class);
        when(hm.getResponseBodyAsString()).thenReturn("{1}");

        ActivityPubObjectReference<ActivityPubObject> reference = new ActivityPubObjectReference<>().setLink(uri);
        when(this.activityPubStorage.retrieveEntity(uri)).thenReturn(person);
        when(this.activityPubClient.get(uri)).thenReturn(hm);
        when(this.activityPubJsonParser.parse("{1}")).thenReturn(person);
        when(this.dateProvider.isElapsed(any(), any(), anyInt())).thenReturn(true);
        assertSame(person, this.resolver.resolveReference(reference));
        assertSame(person, reference.getObject());
        verify(this.activityPubStorage).retrieveEntity(uri);
        verify(this.activityPubClient).get(uri);
        verify(this.activityPubStorage).storeEntity(person);
    }

    @Test
    void resolveReferenceObjectNullStoredNeedRefreshButNetworkError() throws Exception
    {
        URI uri = URI.create("http://test/person/1");
        Person person = new Person();
        person.setId(uri);
        Date twoDaysAgo = DateUtils.addDays(new Date(), -2);
        person.setLastUpdated(twoDaysAgo);

        HttpMethod hm = mock(HttpMethod.class);
        when(hm.getResponseBodyAsString()).thenReturn("{1}");

        ActivityPubObjectReference<ActivityPubObject> reference = new ActivityPubObjectReference<>().setLink(uri);
        when(this.activityPubStorage.retrieveEntity(uri)).thenReturn(person);
        when(this.activityPubClient.get(uri)).thenThrow(new IOException("error"));
        when(this.activityPubJsonParser.parse("{1}")).thenReturn(person);
        when(this.dateProvider.isElapsed(any(), any(), anyInt())).thenReturn(true);
        assertSame(person, this.resolver.resolveReference(reference));
        assertSame(person, reference.getObject());
        verify(this.activityPubStorage).retrieveEntity(uri);
        verify(this.activityPubClient).get(uri);
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void resolveDocumentReferenceAlreadyStored() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("mywiki", "Foo", "Bar");
        when(this.serializer.serialize(documentReference)).thenReturn("mywiki:Foo.Bar");
        when(this.activityPubStorage.escapeQueryChars("mywiki:Foo.Bar")).thenReturn("mywiki\\:Foo.Bar");
        Page expectedPage = mock(Page.class);
        when(this.activityPubStorage.query(Page.class, "filter(xwikiReference:mywiki\\:Foo.Bar)", 1))
            .thenReturn(Collections.singletonList(expectedPage));

        assertEquals(expectedPage, this.resolver.resolveDocumentReference(documentReference));
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void resolveDocumentReferenceNotStored() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("mywiki", "Foo", "Bar");
        when(this.serializer.serialize(documentReference)).thenReturn("mywiki:Foo.Bar");
        when(this.activityPubStorage.escapeQueryChars("mywiki:Foo.Bar")).thenReturn("mywiki\\:Foo.Bar");
        Page expectedPage = new Page().setXwikiReference("mywiki:Foo.Bar");
        when(this.activityPubStorage.query(Page.class, "filter(xwikiReference:mywiki\\:Foo.Bar)", 1))
            .thenReturn(Collections.emptyList());

        assertEquals(expectedPage, this.resolver.resolveDocumentReference(documentReference));
        verify(this.activityPubStorage).storeEntity(expectedPage);
        verify(this.activityPubStorage).query(Page.class, "filter(xwikiReference:mywiki\\:Foo.Bar)", 1);
    }
}
