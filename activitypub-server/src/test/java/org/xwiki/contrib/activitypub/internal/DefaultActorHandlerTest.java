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

import java.net.URI;
import java.net.URL;
import java.util.Arrays;

import javax.inject.Provider;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubIdentifierService;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.SignatureService;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.PublicKey;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.webfinger.WebfingerClient;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.contrib.activitypub.webfinger.entities.Link;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.XWikiContext;

import ch.qos.logback.classic.Level;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultActorHandler}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultActorHandlerTest
{
    private static final String GENERIC_ACTOR_ID = "http://xwiki.org/actor/foo";

    @InjectMockComponents
    private DefaultActorHandler actorHandler;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private ActivityPubClient activityPubClient;

    @MockComponent
    private ActivityPubJsonParser jsonParser;

    @MockComponent
    private XWikiUserBridge xWikiUserBridge;

    @MockComponent
    private SignatureService signatureService;

    @MockComponent
    private WebfingerClient webfingerClient;

    @MockComponent
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private GroupManager groupManager;

    @MockComponent
    private ActivityPubConfiguration activityPubConfiguration;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private DefaultURLHandler defaultURLHandler;

    @MockComponent
    private ActivityPubIdentifierService activityPubIdentifierService;

    @MockComponent
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Mock
    private UserReference fooUserReference;

    @Mock
    private UserReference barUserReference;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private URI fooUserURI;

    @BeforeEach
    void setup() throws Exception
    {
        // Foo is an existing user, named Foo Foo.
        when(this.xWikiUserBridge.resolveUser("xwiki:XWiki.Foo")).thenReturn(this.fooUserReference);
        when(this.xWikiUserBridge.resolveUser("XWiki.Foo")).thenReturn(this.fooUserReference);
        when(this.xWikiUserBridge.resolveUser("Foo")).thenReturn(this.fooUserReference);
        when(this.xWikiUserBridge.isExistingUser("XWiki.Foo")).thenReturn(true);
        when(this.xWikiUserBridge.isExistingUser("Foo")).thenReturn(true);
        when(this.xWikiUserBridge.isExistingUser(this.fooUserReference)).thenReturn(true);
        UserProperties fooUser = mock(UserProperties.class);
        when(this.xWikiUserBridge.resolveUser(this.fooUserReference)).thenReturn(fooUser);
        when(fooUser.getFirstName()).thenReturn("Foo");
        when(fooUser.getLastName()).thenReturn("Foo");
        when(this.xWikiUserBridge.getUserLogin(this.fooUserReference)).thenReturn("XWiki.Foo");
        this.fooUserURI = new URI("http://domain.org/xwiki/activitypub/Person/xwiki%3AXWiki.Foo");
        when(this.serializer.serialize(new ActivityPubResourceReference("Person", "xwiki:XWiki.Foo")))
            .thenReturn(this.fooUserURI);
        when(this.userReferenceSerializer.serialize(this.fooUserReference)).thenReturn("xwiki:XWiki.Foo");

        // Bar does not exist.
        when(this.xWikiUserBridge.resolveUser("XWiki.Bar")).thenReturn(this.barUserReference);
        when(this.xWikiUserBridge.isExistingUser("XWiki.Bar")).thenReturn(false);
        when(this.xWikiUserBridge.isExistingUser(this.barUserReference)).thenReturn(false);
        when(this.xWikiUserBridge.resolveUser("Bar")).thenReturn(this.barUserReference);
        when(this.xWikiUserBridge.isExistingUser("Bar")).thenReturn(false);

        when(this.activityPubStorage.storeEntity(any(AbstractActor.class))).thenAnswer(invocationOnMock -> {
            AbstractActor actor = (AbstractActor) invocationOnMock.getArguments()[0];
            actor.setId(new URI(GENERIC_ACTOR_ID));
            return null;
        });
    }

    @Test
    void getActorWithStoredPerson() throws Exception
    {
        AbstractActor expectedActor = new Person().setPreferredUsername("XWiki.Foo");
        when(this.activityPubStorage.retrieveEntity(this.fooUserURI)).thenReturn(expectedActor);

        verify(this.activityPubStorage, never()).storeEntity(any());
        assertSame(expectedActor, this.actorHandler.getActor(this.fooUserReference));
    }

    @Test
    void getActorWithNotStoredButExistingPerson() throws Exception
    {
        when(this.signatureService.getPublicKeyPEM(any())).thenReturn("...");
        when(this.xWikiUserBridge.getDocumentReference(this.fooUserReference))
            .thenReturn(new DocumentReference("xwiki", "XWiki", "Foo"));
        when(this.activityPubIdentifierService
            .createIdentifier(eq(new Person().setName("Foo Foo")), eq("Foo"), eq("xwiki")))
            .thenReturn("Foo");

        PublicKey publicKey = new PublicKey().setPublicKeyPem("...")
            .setId(GENERIC_ACTOR_ID + "#main-key")
            .setOwner(GENERIC_ACTOR_ID);
        AbstractActor expectedActor = new Person();
        expectedActor.setPreferredUsername("Foo")
            .setPublicKey(publicKey)
            .setInbox(new ActivityPubObjectReference<Inbox>().setObject(
                new Inbox().setAttributedTo(singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(expectedActor)))))
            .setOutbox(new ActivityPubObjectReference<Outbox>().setObject(
                new Outbox().setAttributedTo(singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(expectedActor)))))
            .setFollowers(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setFollowing(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setName("Foo Foo")
            .setId(new URI(GENERIC_ACTOR_ID))
            .setXwikiReference("xwiki:XWiki.Foo");

        AbstractActor obtainedActor = this.actorHandler.getActor(this.fooUserReference);
        assertEquals(expectedActor, obtainedActor);

        // Ensure the store is called the right number of times
        verify(this.activityPubStorage, times(1)).storeEntity(any(Outbox.class));
        verify(this.activityPubStorage, times(1)).storeEntity(any(Inbox.class));
        // Twice for followers & followings, but Inbox & Outbox are also OrderedCollection.
        verify(this.activityPubStorage, times(4)).storeEntity(any(OrderedCollection.class));
        // Twice: we need to call it first to get ID and then to store keys
        verify(this.activityPubStorage, times(2)).storeEntity(any(Person.class));

        // Total number of call
        verify(this.activityPubStorage, times(6)).storeEntity(any());
    }

    @Test
    void getActorWithStoredService() throws Exception
    {
        WikiReference wikiReference = new WikiReference("FooWiki");
        when(this.entityReferenceSerializer.serialize(wikiReference)).thenReturn("wiki:FooWiki");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("Service", "wiki:FooWiki");
        URI uri = URI.create("http://foowiki");
        when(this.serializer.serialize(resourceReference)).thenReturn(uri);

        Service actor = mock(Service.class);
        when(this.activityPubStorage.retrieveEntity(uri)).thenReturn(actor);
        assertSame(actor, this.actorHandler.getActor(wikiReference));
    }

    @Test
    void getActorWithNotStoredButExistingService() throws Exception
    {
        WikiReference wikiReference = new WikiReference("FooWiki");
        when(this.signatureService.getPublicKeyPEM(any())).thenReturn("...");
        when(this.activityPubIdentifierService
            .createIdentifier(eq(new Service().setName("Wiki FooWiki")), eq(null), eq("FooWiki")))
            .thenReturn("FooWiki.xwiki");

        PublicKey publicKey = new PublicKey().setPublicKeyPem("...")
            .setId(GENERIC_ACTOR_ID + "#main-key")
            .setOwner(GENERIC_ACTOR_ID);
        AbstractActor expectedActor = new Service();
        expectedActor.setPreferredUsername("FooWiki.xwiki")
            .setPublicKey(publicKey)
            .setInbox(new ActivityPubObjectReference<Inbox>().setObject(
                new Inbox().setAttributedTo(singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(expectedActor)))))
            .setOutbox(new ActivityPubObjectReference<Outbox>().setObject(
                new Outbox().setAttributedTo(singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(expectedActor)))))
            .setFollowers(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setFollowing(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setName("Wiki FooWiki")
            .setId(new URI(GENERIC_ACTOR_ID));

        assertEquals(expectedActor, this.actorHandler.getActor(wikiReference));
    }

    @Test
    void getPersonNotExisting() throws Exception
    {
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.actorHandler.getActor(this.barUserReference);
        });
        assertEquals("Cannot find any user with reference [barUserReference]", activityPubException.getMessage());
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void isExistingUser()
    {
        assertTrue(this.actorHandler.isExistingUser("Foo"));
        assertTrue(this.actorHandler.isExistingUser("XWiki.Foo"));

        assertFalse(this.actorHandler.isExistingUser("Bar"));
        assertFalse(this.actorHandler.isExistingUser("XWiki.Bar"));
    }

    @Test
    void getXWikiUserReference() throws Exception
    {
        assertSame(this.fooUserReference,
            this.actorHandler.getXWikiUserReference(new Person().setPreferredUsername("Foo")));
        assertSame(this.fooUserReference,
            this.actorHandler.getXWikiUserReference(new Person().setPreferredUsername("XWiki.Foo")));

        assertNull(this.actorHandler.getXWikiUserReference(new Person().setPreferredUsername("Bar")));
        assertNull(this.actorHandler.getXWikiUserReference(new Person().setPreferredUsername("XWiki.Bar")));
    }

    @Test
    void isLocalActor() throws Exception
    {
        assertTrue(this.actorHandler.isLocalActor(new Person().setPreferredUsername("Foo")));
        assertTrue(this.actorHandler.isLocalActor(new Person().setPreferredUsername("XWiki.Foo")));

        assertFalse(this.actorHandler.isLocalActor(new Person().setPreferredUsername("Bar")));
        assertFalse(this.actorHandler.isLocalActor(new Person().setPreferredUsername("XWiki.Bar")));

        URI actorId = new URI("http://myId");
        when(this.defaultURLHandler.belongsToCurrentInstance(actorId)).thenReturn(true);
        assertTrue(this.actorHandler.isLocalActor(new Person().setId(actorId)));
        verify(this.defaultURLHandler).belongsToCurrentInstance(actorId);
    }

    @Test
    void getLocalActor() throws Exception
    {
        when(this.signatureService.getPublicKeyPEM(any())).thenReturn("...");
        when(this.xWikiUserBridge.getDocumentReference(this.fooUserReference))
            .thenReturn(new DocumentReference("xwiki", "XWiki", "Foo"));
        when(this.activityPubIdentifierService
            .createIdentifier(eq(new Person().setName("Foo Foo")), eq("Foo"), eq("xwiki")))
            .thenReturn("Foo");

        PublicKey publicKey = new PublicKey().setPublicKeyPem("...")
            .setId(GENERIC_ACTOR_ID + "#main-key")
            .setOwner(GENERIC_ACTOR_ID);

        AbstractActor expectedActor = new Person();
        expectedActor.setPreferredUsername("Foo")
            .setPublicKey(publicKey)
            .setInbox(new ActivityPubObjectReference<Inbox>().setObject(
                new Inbox().setAttributedTo(singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(expectedActor)))))
            .setOutbox(new ActivityPubObjectReference<Outbox>().setObject(
                new Outbox().setAttributedTo(singletonList(
                    new ActivityPubObjectReference<AbstractActor>().setObject(expectedActor)))))
            .setFollowers(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setFollowing(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setName("Foo Foo")
            .setId(new URI(GENERIC_ACTOR_ID))
            .setXwikiReference("xwiki:XWiki.Foo");

        when(this.webfingerClient.get("xwiki:XWiki.Foo")).thenThrow(new WebfingerException(""));
        when(this.webfingerClient.get("xwiki:XWiki.Bar")).thenThrow(new WebfingerException(""));

        assertEquals(expectedActor, this.actorHandler.getActor("Foo"));
        assertEquals(expectedActor, this.actorHandler.getActor("XWiki.Foo"));
        assertEquals(expectedActor, this.actorHandler.getActor("xwiki:XWiki.Foo"));
        assertNull(this.actorHandler.getActor("xwiki:XWiki.Bar"));
        assertNull(this.actorHandler.getActor("XWiki.Bar"));
        assertNull(this.actorHandler.getActor("Bar"));

        assertEquals(3, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(1).getLevel());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(2).getLevel());
        assertEquals("Cannot find the asked user [xwiki:XWiki.Bar].", this.logCapture.getMessage(0));
        assertEquals("Cannot find the asked user [XWiki.Bar].", this.logCapture.getMessage(1));
        assertEquals("Cannot find the asked user [Bar].", this.logCapture.getMessage(2));
    }

    @Test
    void getRemoteActor() throws Exception
    {
        when(this.webfingerClient.get(any())).thenThrow(new WebfingerException("Cannot find resource", 404));
        Person person = new Person().setPreferredUsername("Foobar");
        String remoteActorUrl = "http://www.xwiki.org/xwiki/activitypub/Person/Foobar";
        GetMethod getMethod = mock(GetMethod.class);
        when(this.activityPubClient.get(new URI(remoteActorUrl))).thenReturn(getMethod);
        when(this.jsonParser.parse(getMethod.getResponseBodyAsStream())).thenReturn(person);
        assertSame(person, this.actorHandler.getActor(remoteActorUrl));
    }

    @Test
    void getRemoteActorWebfinger() throws Exception
    {
        JSONResourceDescriptor jrd = mock(JSONResourceDescriptor.class);
        when(jrd.getLinks())
            .thenReturn(singletonList(new Link().setRel("self").setHref(URI.create("http://server.com/user/test"))));
        when(this.activityPubClient.get(any())).thenReturn(mock(HttpMethod.class));
        when(this.webfingerClient.get("test@server.com")).thenReturn(jrd);
        this.actorHandler.getActor("test@server.com");
        verify(this.webfingerClient).get(eq("test@server.com"));
        verify(this.activityPubClient).get(new URI("http://server.com/user/test"));
    }

    @Test
    void getRemoteActorXWikiProfile() throws Exception
    {
        Person person = new Person().setPreferredUsername("Foobar");
        String remoteProfileUrl = "http://www.xwiki.org/xwiki/view/bin/XWiki/Foobar";
        String remoteActorUrl = "http://www.xwiki.org/xwiki/activitypub/Person/XWiki.Foobar";

        when(this.webfingerClient.get(any())).thenThrow(new WebfingerException("Cannot find resource", 404));
        GetMethod getMethodProfile = mock(GetMethod.class);
        when(this.activityPubClient.get(new URI(remoteProfileUrl))).thenReturn(getMethodProfile);
        doThrow(new ActivityPubException("Check issue")).when(this.activityPubClient).checkAnswer(getMethodProfile);

        HttpConnection jsoupConnection = mock(HttpConnection.class);
        this.actorHandler.setJsoupConnection(jsoupConnection);
        when(jsoupConnection.url(new URL(remoteProfileUrl))).thenReturn(jsoupConnection);
        Document document = mock(Document.class);
        when(jsoupConnection.get()).thenReturn(document);
        Element element = mock(Element.class);
        when(document.selectFirst("html")).thenReturn(element);
        when(element.attr("data-xwiki-document")).thenReturn("XWiki.Foobar");

        GetMethod getMethodActor = mock(GetMethod.class);
        when(this.activityPubClient.get(new URI(remoteActorUrl))).thenReturn(getMethodActor);
        when(this.jsonParser.parse(getMethodActor.getResponseBodyAsStream())).thenReturn(person);
        assertSame(person, this.actorHandler.getActor(remoteProfileUrl));
        assertSame(person, this.actorHandler.getActor(remoteActorUrl));
    }

    @Test
    void getCurrentActor() throws Exception
    {
        when(this.xWikiUserBridge.isExistingUser(CurrentUserReference.INSTANCE)).thenReturn(true);
        when(this.userReferenceSerializer.serialize(CurrentUserReference.INSTANCE)).thenReturn("xwiki:XWiki.Foo");
        AbstractActor expectedActor = new Person().setPreferredUsername("XWiki.Foo").setPreferredUsername("FooWiki");
        when(this.activityPubStorage.retrieveEntity(this.fooUserURI)).thenReturn(expectedActor);

        assertSame(expectedActor, this.actorHandler.getCurrentActor());
    }

    @Test
    void isAuthorizedToActForWithPerson() throws Exception
    {
        assertFalse(this.actorHandler.isAuthorizedToActFor(null, mock(AbstractActor.class)));
        assertFalse(this.actorHandler.isAuthorizedToActFor(null, mock(Person.class)));

        Person actor = mock(Person.class);
        when(actor.getPreferredUsername()).thenReturn("Foo");
        assertTrue(this.actorHandler.isAuthorizedToActFor(this.fooUserReference, actor));
        assertFalse(this.actorHandler.isAuthorizedToActFor(mock(UserReference.class), actor));
    }

    @Test
    void isAuthorizedToActForWithService() throws Exception
    {
        Service actor = mock(Service.class);
        assertFalse(this.actorHandler.isAuthorizedToActFor(null, actor));

        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        WikiReference wikiReference = mock(WikiReference.class);
        when(xWikiContext.getWikiReference()).thenReturn(wikiReference);
        DocumentReference groupReference = mock(DocumentReference.class);
        when(this.activityPubConfiguration.getWikiGroup()).thenReturn(groupReference);
        DocumentReference userDocumentReference = mock(DocumentReference.class);
        when(this.xWikiUserBridge.getDocumentReference(this.fooUserReference)).thenReturn(userDocumentReference);
        when(userDocumentReference.toString()).thenReturn("Foo");

        when(this.groupManager.getGroups(userDocumentReference, wikiReference, true))
            .thenReturn(Arrays.asList(mock(DocumentReference.class), groupReference, mock(DocumentReference.class)));
        assertTrue(this.actorHandler.isAuthorizedToActFor(this.fooUserReference, actor));

        when(this.groupManager.getGroups(userDocumentReference, wikiReference, true))
            .thenReturn(emptyList());
        assertFalse(this.actorHandler.isAuthorizedToActFor(this.fooUserReference, actor));

        when(this.groupManager.getGroups(userDocumentReference, wikiReference, true))
            .thenReturn(singletonList(mock(DocumentReference.class)));
        assertFalse(this.actorHandler.isAuthorizedToActFor(this.fooUserReference, actor));

        when(this.groupManager.getGroups(userDocumentReference, wikiReference, true))
            .thenThrow(new GroupException("error"));
        ActivityPubException exception = assertThrows(ActivityPubException.class,
            () -> this.actorHandler.isAuthorizedToActFor(this.fooUserReference, actor));
        assertEquals("Error while looking for groups for [Foo].", exception.getMessage());
    }

    @Test
    void getNotificationTarget() throws Exception
    {
        Person fooUser = mock(Person.class);
        when(fooUser.getPreferredUsername()).thenReturn("Foo");
        when(this.userReferenceSerializer.serialize(this.fooUserReference)).thenReturn("XWiki.Foo");
        assertEquals("XWiki.Foo", this.actorHandler.getNotificationTarget(fooUser));

        DocumentReference groupReference = mock(DocumentReference.class);
        when(this.activityPubConfiguration.getWikiGroup()).thenReturn(groupReference);
        when(this.entityReferenceSerializer.serialize(groupReference)).thenReturn("XWiki.MyGroup");
        assertEquals("XWiki.MyGroup", this.actorHandler.getNotificationTarget(mock(Service.class)));

        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getType()).thenReturn("Unknown type");
        ActivityPubException exception =
            assertThrows(ActivityPubException.class, () -> this.actorHandler.getNotificationTarget(abstractActor));
        assertEquals("This type of actor is not supported yet [Unknown type]", exception.getMessage());
    }

    @Test
    void getStoreDocument() throws Exception
    {
        Person fooUser = mock(Person.class);
        when(fooUser.getPreferredUsername()).thenReturn("Foo");
        DocumentReference userDocumentReference = mock(DocumentReference.class);
        when(this.xWikiUserBridge.getDocumentReference(this.fooUserReference)).thenReturn(userDocumentReference);
        assertSame(userDocumentReference, this.actorHandler.getStoreDocument(fooUser));

        Service serviceUser = mock(Service.class);
        when(serviceUser.getPreferredUsername()).thenReturn("fooWiki");
        DocumentReference expectedDocument =
            new DocumentReference("fooWiki", Arrays.asList("ActivityPub", "ServiceActors"), "fooWiki");
        assertEquals(expectedDocument, this.actorHandler.getStoreDocument(serviceUser));

        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getType()).thenReturn("Unknown type");
        ActivityPubException exception =
            assertThrows(ActivityPubException.class, () -> this.actorHandler.getStoreDocument(abstractActor));
        assertEquals("This type of actor is not supported yet [Unknown type]", exception.getMessage());
    }

    @Test
    void getActorFromResourceReference() throws Exception
    {
        Person expectedPerson = new Person().setPreferredUsername("XWiki.Foo");
        when(this.activityPubStorage.retrieveEntity(this.fooUserURI)).thenReturn(expectedPerson);
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("Person", "xwiki:XWiki.Foo");
        assertSame(expectedPerson, this.actorHandler.getActor(resourceReference));

        resourceReference = new ActivityPubResourceReference("Service", "wiki:SubWiki");
        WikiReference wikiReference = new WikiReference("SubWiki");
        when(this.entityReferenceResolver.resolve("wiki:SubWiki", EntityType.WIKI)).thenReturn(wikiReference);
        when(this.entityReferenceSerializer.serialize(wikiReference)).thenReturn("wiki:SubWiki");
        URI subwikiURI = URI.create("http://subwiki");
        when(this.serializer.serialize(resourceReference)).thenReturn(subwikiURI);
        Service expectedService = mock(Service.class);
        when(this.activityPubStorage.retrieveEntity(subwikiURI)).thenReturn(expectedService);
        assertSame(expectedService, this.actorHandler.getActor(resourceReference));
    }
}
