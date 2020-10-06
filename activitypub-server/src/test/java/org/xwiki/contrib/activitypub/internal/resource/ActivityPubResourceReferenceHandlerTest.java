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
package org.xwiki.contrib.activitypub.internal.resource;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.inject.Provider;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.internal.filters.CollectionFilter;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

import static java.util.Collections.singletonList;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ActivityPubResourceReferenceHandler}.
 *
 * @version $Id$
 */
@ComponentTest
public class ActivityPubResourceReferenceHandlerTest
{
    @InjectMockComponents
    private ActivityPubResourceReferenceHandler handler;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private ActivityPubJsonSerializer activityPubJsonSerializer;

    @MockComponent
    private ActivityPubJsonParser activityPubJsonParser;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @MockComponent
    private XWikiUserBridge xWikiUserBridge;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private CollectionFilter<OrderedCollection<AbstractActivity>> publicActivityCollectionFilter;

    @MockComponent
    private WebfingerService webfingerService;

    @Mock
    private ResourceReferenceHandlerChain handlerChain;

    private HttpServletRequest servletRequest;

    private HttpServletResponse servletResponse;

    private ServletOutputStream responseOutput;

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        this.servletRequest = mock(HttpServletRequest.class);
        this.servletResponse = mock(HttpServletResponse.class);
        this.responseOutput = mock(ServletOutputStream.class);

        Container container = this.componentManager.registerMockComponent(Container.class);
        ServletRequest request = mock(ServletRequest.class);
        when(container.getRequest()).thenReturn(request);
        when(request.getHttpServletRequest()).thenReturn(this.servletRequest);

        ServletResponse response = mock(ServletResponse.class);
        when(container.getResponse()).thenReturn(response);
        when(response.getHttpServletResponse()).thenReturn(this.servletResponse);
        when(this.servletResponse.getOutputStream()).thenReturn(this.responseOutput);
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer());
        this.componentManager.registerComponent(ComponentManager.class, "context", this.componentManager);

        XWikiContext xWikiContext = mock(XWikiContext.class);
        Provider<XWikiContext> contextProvider = this.componentManager
            .registerMockComponent(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        when(contextProvider.get()).thenReturn(xWikiContext);
    }

    private void verifyResponse(int code, String message) throws IOException
    {
        verify(this.servletResponse, times(1)).setStatus(code);
        verify(this.servletResponse, times(1)).setContentType("text/plain");
        verify(this.responseOutput, times(1)).write(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Check that a resource which cannot be found returned a 404.
     */
    @Test
    void handleNotStoredEntity() throws Exception
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("create", "43");
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(404, "The entity of type [create] and uid [43] cannot be found.");
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
        verify(this.servletRequest, never()).getMethod();
    }

    @Test
    void handleNotStoredUnexistingActor() throws Exception
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("actor", "Foo");
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(404, "The entity of type [actor] and uid [Foo] cannot be found.");
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
        verify(this.servletRequest, never()).getMethod();
    }

    @Test
    void handleGetNotStoredExistingActorFromBrowser() throws Exception
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("person", "Foo");
        Person person = new Person().setPreferredUsername("Foo");
        when(this.actorHandler.getActor(resourceReference)).thenReturn(person);
        when(this.servletRequest.getMethod()).thenReturn("GET");
        when(this.servletRequest.getHeader("Accept")).thenReturn("...,text/html,...");
        URI webURI = URI.create("http://wiki.tld/web/view/1");
        when(this.webfingerService.resolveXWikiUserUrl(person)).thenReturn(webURI);
        this.handler.handle(resourceReference, this.handlerChain);
        verify(this.activityPubJsonSerializer, never()).serialize(this.responseOutput, person);
        verify(this.handlerChain).handleNext(resourceReference);
        verify(this.servletResponse).sendRedirect(webURI.toASCIIString());
    }

    @Test
    void handleGetNotStoredExistingActor() throws Exception
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("person", "Foo");
        Person person = new Person().setPreferredUsername("Foo");
        when(this.actorHandler.getActor(resourceReference)).thenReturn(person);
        when(this.servletRequest.getMethod()).thenReturn("GET");
        when(this.servletRequest.getHeader("Accept")).thenReturn("application/json");
        this.handler.handle(resourceReference, this.handlerChain);
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, person);
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
        verify(this.servletResponse, never()).sendRedirect(any());
    }

    @Test
    void handletGetStoredEntity() throws Exception
    {
        Create create = new Create().setName("Create 42");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("create", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/Create/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(create);
        when(this.servletRequest.getMethod()).thenReturn("GET");
        this.handler.handle(resourceReference, this.handlerChain);
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, create);
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostOutsideBox() throws Exception
    {
        Create create = new Create().setName("Create 42");
        when(this.servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("create", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/Create/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(create);
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(400, "POST requests are only allowed on inbox or outbox.");
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostInUnattributedBox() throws Exception
    {
        Inbox inbox = new Inbox();
        when(this.servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("inbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/Inbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(inbox);
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(500,
            "This box is not attributed. Please report the error to the administrator.");
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostInbox() throws Exception
    {
        Person person = new Person().setPreferredUsername("Foo");
        ActivityPubObjectReference<AbstractActor> actorReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(person);
        when(this.objectReferenceResolver.resolveReference(actorReference)).thenReturn(person);
        Inbox inbox = new Inbox().setAttributedTo(singletonList(actorReference));
        when(this.servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("inbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/Inbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(inbox);

        Create create = new Create().setName("Create 42");
        when(this.servletRequest.getInputStream()).thenReturn(mock(ServletInputStream.class));
        when(this.activityPubJsonParser.parse(any(Reader.class))).thenReturn(create);
        ActivityHandler<Create> activityHandler = this.componentManager
            .registerMockComponent(new DefaultParameterizedType(null, ActivityHandler.class, Create.class));

        this.handler.handle(resourceReference, this.handlerChain);

        verify(activityHandler, times(1))
            .handleInboxRequest(new ActivityRequest<>(person, create, this.servletRequest, this.servletResponse));
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostOutboxNotAuthorized() throws Exception
    {
        Person person = new Person().setPreferredUsername("Foo");
        UserReference fooUser = mock(UserReference.class);
        when(this.actorHandler.getXWikiUserReference(person)).thenReturn(fooUser);
        when(fooUser.toString()).thenReturn("FooUser");
        ActivityPubObjectReference<AbstractActor> actorReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(person);
        when(this.objectReferenceResolver.resolveReference(actorReference)).thenReturn(person);
        Outbox outbox = new Outbox().setAttributedTo(singletonList(actorReference));
        when(this.servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("outbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/Outbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(outbox);

        Create create = new Create().setName("Create 42");
        when(this.servletRequest.getInputStream()).thenReturn(mock(ServletInputStream.class));
        when(this.activityPubJsonParser.parse(any(Reader.class))).thenReturn(create);
        ActivityHandler<Create> activityHandler = this.componentManager
            .registerMockComponent(new DefaultParameterizedType(null, ActivityHandler.class, Create.class));

        when(this.actorHandler.isAuthorizedToActFor(fooUser, person)).thenReturn(false);
        this.handler.handle(resourceReference, this.handlerChain);

        verify(activityHandler, never())
            .handleOutboxRequest(new ActivityRequest<>(person, create, this.servletRequest, this.servletResponse));
        verifyResponse(403, "The session user [null] cannot post to [Foo] outbox.");
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostOutboxAuthorized() throws Exception
    {
        Person person = new Person().setPreferredUsername("Foo");
        UserReference fooUserReference = mock(UserReference.class);
        when(this.actorHandler.getXWikiUserReference(person)).thenReturn(fooUserReference);
        when(this.xWikiUserBridge.getCurrentUserReference()).thenReturn(fooUserReference);
        ActivityPubObjectReference<AbstractActor> actorReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(person);
        when(this.objectReferenceResolver.resolveReference(actorReference)).thenReturn(person);
        Outbox outbox = new Outbox().setAttributedTo(singletonList(actorReference));
        when(this.servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("outbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/Outbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(outbox);

        Create create = new Create().setName("Create 42");
        when(this.servletRequest.getInputStream()).thenReturn(mock(ServletInputStream.class));
        when(this.activityPubJsonParser.parse(any(Reader.class))).thenReturn(create);
        ActivityHandler<Create> activityHandler = this.componentManager
            .registerMockComponent(new DefaultParameterizedType(null, ActivityHandler.class, Create.class));

        when(this.actorHandler.isAuthorizedToActFor(fooUserReference, person)).thenReturn(true);
        this.handler.handle(resourceReference, this.handlerChain);

        verify(activityHandler, times(1))
            .handleOutboxRequest(new ActivityRequest<>(person, create, this.servletRequest, this.servletResponse));
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handleNetworkError() throws Exception
    {
        ActivityPubResourceReference reference = mock(ActivityPubResourceReference.class);
        ActivityPubException ape = mock(ActivityPubException.class);
        when(this.activityPubStorage.retrieveEntity(any())).thenThrow(ape);
        this.handler.handle(reference, this.handlerChain);
        verify(this.servletResponse).setStatus(SC_INTERNAL_SERVER_ERROR);
        verify(this.servletResponse).setContentType("text/plain");
        verify(ape).printStackTrace(nullable(PrintWriter.class));
    }

    @Test
    void handleGetInboxGuest() throws Exception
    {
        Inbox inbox = new Inbox()
            .setName("Inbox 42");
        inbox.setAttributedTo(Arrays.asList(mock(ActivityPubObjectReference.class)));
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("inbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/inbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(inbox);
        when(this.servletRequest.getMethod()).thenReturn("GET");
        when(this.publicActivityCollectionFilter.filter(inbox)).thenReturn(inbox);
        this.handler.handle(resourceReference, this.handlerChain);
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, inbox);
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
        verify(this.publicActivityCollectionFilter).filter(inbox);
    }

    @Test
    void handleGetOutboxGuest() throws Exception
    {
        Outbox outbox = new Outbox()
            .setName("Outbox 42");
        outbox.setAttributedTo(Arrays.asList(mock(ActivityPubObjectReference.class)));
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("outbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/outbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(outbox);
        when(this.servletRequest.getMethod()).thenReturn("GET");
        when(this.publicActivityCollectionFilter.filter(outbox)).thenReturn(outbox);
        this.handler.handle(resourceReference, this.handlerChain);
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, outbox);
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
        verify(this.publicActivityCollectionFilter).filter(outbox);
    }

    @Test
    void handleGetInboxOwner() throws Exception
    {
        Inbox inbox = new Inbox()
            .setName("Inbox 42");
        ActivityPubObjectReference reference = mock(ActivityPubObjectReference.class);
        inbox.setAttributedTo(singletonList(reference));
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("inbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/inbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(inbox);
        AbstractActor actor = mock(AbstractActor.class);
        UserReference userReference = mock(UserReference.class);
        when(this.xWikiUserBridge.getCurrentUserReference()).thenReturn(userReference);
        when(this.objectReferenceResolver.resolveReference(reference)).thenReturn(actor);
        when(this.actorHandler.isAuthorizedToActFor(userReference, actor)).thenReturn(true);
        when(this.servletRequest.getMethod()).thenReturn("GET");
        this.handler.handle(resourceReference, this.handlerChain);
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, inbox);
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
        verify(this.publicActivityCollectionFilter, never()).filter(inbox);
    }

    @Test
    void handleGetOutboxOwner() throws Exception
    {
        Outbox outbox = new Outbox()
            .setName("outbox 42");
        ActivityPubObjectReference reference = mock(ActivityPubObjectReference.class);
        outbox.setAttributedTo(Arrays.asList(reference));
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("outbox", "42");
        String requestURL = "http://domain.org/xwiki/activitypub/outbox/42";
        when(this.servletRequest.getRequestURL()).thenReturn(new StringBuffer(requestURL));
        when(this.activityPubStorage.retrieveEntity(new URI(requestURL))).thenReturn(outbox);
        AbstractActor actor = mock(AbstractActor.class);
        UserReference userReference = mock(UserReference.class);
        when(this.xWikiUserBridge.getCurrentUserReference()).thenReturn(userReference);
        when(this.objectReferenceResolver.resolveReference(reference)).thenReturn(actor);
        when(this.actorHandler.isAuthorizedToActFor(userReference, actor)).thenReturn(true);
        when(this.servletRequest.getMethod()).thenReturn("GET");
        this.handler.handle(resourceReference, this.handlerChain);
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, outbox);
        verify(this.handlerChain, times(1)).handleNext(resourceReference);
        verify(this.publicActivityCollectionFilter, never()).filter(outbox);
    }
}
