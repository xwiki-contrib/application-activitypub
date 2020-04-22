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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.math3.analysis.function.Abs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
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
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
public class DefaultActivityPubObjectReferenceResolverTest
{
    @InjectMockComponents
    private DefaultActivityPubObjectReferenceResolver defaultActivityPubObjectReferenceResolver;

    @MockComponent
    private ActivityPubJsonParser activityPubJsonParser;

    @MockComponent
    private ActivityPubClient activityPubClient;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @BeforeEach
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        DefaultParameterizedType storageProviderType = new DefaultParameterizedType(null, Provider.class,
            ActivityPubStorage.class);
        Provider<ActivityPubStorage> storageProvider = componentManager.registerMockComponent(storageProviderType);
        when(storageProvider.get()).thenReturn(this.activityPubStorage);

        DefaultParameterizedType clientProviderType = new DefaultParameterizedType(null, Provider.class,
            ActivityPubClient.class);
        Provider<ActivityPubClient> clientProvider = componentManager.registerMockComponent(clientProviderType);
        when(clientProvider.get()).thenReturn(this.activityPubClient);
    }

    @Test
    public void resolveReferenceInvalidLink()
    {
        Executable executable =
            () -> this.defaultActivityPubObjectReferenceResolver.resolveReference(new ActivityPubObjectReference<>());
        ActivityPubException e = assertThrows(ActivityPubException.class, executable);
        assertEquals("The reference property is null and does not have any ID to follow.", e.getMessage());
    }

    @Test
    public void resolveReferenceWithObject() throws Exception
    {
        ActivityPubObject object = mock(ActivityPubObject.class);
        assertSame(object, this.defaultActivityPubObjectReferenceResolver
            .resolveReference(new ActivityPubObjectReference<>().setObject(object)));
        verify(this.activityPubClient, never()).get(any());
        verify(this.activityPubStorage, never()).retrieveEntity(any());
    }

    @Test
    public void resolveReferenceObjectNullNotStored() throws Exception
    {
        Accept t = new Accept();
        HttpMethod hm = mock(HttpMethod.class);
        when(hm.getResponseBodyAsString()).thenReturn("{accept}");
        URI uri = URI.create("http://test/create/1");
        ActivityPubObjectReference<ActivityPubObject> reference = new ActivityPubObjectReference<>().setLink(uri);
        when(this.activityPubClient.get(uri)).thenReturn(hm);
        when(this.activityPubJsonParser.parse("{accept}")).thenReturn(t);
        assertSame(t, this.defaultActivityPubObjectReferenceResolver.resolveReference(reference));
        assertSame(t, reference.getObject());
        verify(this.activityPubStorage).retrieveEntity(uri);
    }

    @Test
    public void resolveReferenceObjectNullStored() throws Exception
    {
        Accept t = new Accept();
        URI uri = URI.create("http://test/create/1");
        ActivityPubObjectReference<ActivityPubObject> reference = new ActivityPubObjectReference<>().setLink(uri);
        when(this.activityPubStorage.retrieveEntity(uri)).thenReturn(t);
        assertSame(t, this.defaultActivityPubObjectReferenceResolver.resolveReference(reference));
        assertSame(t, reference.getObject());
        verify(this.activityPubClient, never()).get(uri);
    }

    @Test
    public void resolveReferenceNetworkError() throws Exception
    {
        when(this.activityPubClient.get(any())).thenThrow(new IOException(""));
        ActivityPubObjectReference<ActivityPubObject> reference =
            new ActivityPubObjectReference<>().setLink(URI.create("http://test/create/1"));
        ActivityPubException e = assertThrows(ActivityPubException.class,
            () -> this.defaultActivityPubObjectReferenceResolver.resolveReference(reference));
        assertEquals("Error when retrieving the ActivityPub information from [http://test/create/1]", e.getMessage());
    }

    @Test
    public void resolveTargetsWithComputedTargets()
    {
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        Set<AbstractActor> result = Collections.singleton(mock(AbstractActor.class));
        when(activityPubObject.getComputedTargets()).thenReturn(result);
        assertEquals(result, this.defaultActivityPubObjectReferenceResolver.resolveTargets(activityPubObject));
    }

    @Test
    public void resolveTargetsNotComputed() throws Exception
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
        assertEquals(expectedSet, this.defaultActivityPubObjectReferenceResolver.resolveTargets(activityPubObject));
    }

    @Test
    public void resolveTargetsEmpty()
    {
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        when(activityPubObject.getComputedTargets()).thenReturn(null);
        assertEquals(Collections.emptySet(),
            this.defaultActivityPubObjectReferenceResolver.resolveTargets(activityPubObject));
    }
}
