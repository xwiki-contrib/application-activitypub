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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import javax.inject.Provider;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
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
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.description;
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

    @Mock
    private ResourceReferenceHandlerChain handlerChain;

    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    private ServletOutputStream responseOutput;
    private XWikiContext xWikiContext;

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        this.servletRequest = mock(HttpServletRequest.class);
        this.servletResponse = mock(HttpServletResponse.class);
        this.responseOutput = mock(ServletOutputStream.class);

        Container container = componentManager.registerMockComponent(Container.class);
        ServletRequest request = mock(ServletRequest.class);
        when(container.getRequest()).thenReturn(request);
        when(request.getHttpServletRequest()).thenReturn(this.servletRequest);

        ServletResponse response = mock(ServletResponse.class);
        when(container.getResponse()).thenReturn(response);
        when(response.getHttpServletResponse()).thenReturn(this.servletResponse);
        when(this.servletResponse.getOutputStream()).thenReturn(this.responseOutput);
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);

        this.xWikiContext = mock(XWikiContext.class);
        Provider<XWikiContext> contextProvider = componentManager
            .registerMockComponent(new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        when(contextProvider.get()).thenReturn(this.xWikiContext);
    }

    private void verifyResponse(int code, String message) throws IOException
    {
        verify(servletResponse, times(1)).setStatus(code);
        verify(servletResponse, times(1)).setContentType("text/plain");
        verify(responseOutput, times(1)).write(message.getBytes(StandardCharsets.UTF_8));
    }

    private void verifyResponse(ActivityPubObject entity) throws IOException, ActivityPubException
    {
        verify(this.servletResponse, times(1)).setStatus(200);
        verify(this.servletResponse, times(1)).setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
        verify(this.servletResponse, times(1)).setCharacterEncoding(StandardCharsets.UTF_8.toString());
        verify(this.activityPubJsonSerializer, times(1)).serialize(this.responseOutput, entity);
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
        verify(handlerChain, times(1)).handleNext(resourceReference);
        verify(servletRequest, never()).getMethod();
    }

    @Test
    void handleNotStoredUnexistingActor() throws Exception
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("actor", "Foo");
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(404, "The entity of type [actor] and uid [Foo] cannot be found.");
        verify(handlerChain, times(1)).handleNext(resourceReference);
        verify(servletRequest, never()).getMethod();
    }

    @Test
    void handleGetNotStoredExistingActor() throws Exception
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("actor", "Foo");
        when(actorHandler.isExistingUser("Foo")).thenReturn(true);
        Person person = new Person().setPreferredUsername("Foo");
        when(actorHandler.getLocalActor("Foo")).thenReturn(person);
        when(servletRequest.getMethod()).thenReturn("GET");
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(person);
        verify(handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handletGetStoredEntity() throws Exception
    {
        Create create = new Create().setName("Create 42");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("create", "42");
        when(this.activityPubStorage.retrieveEntity("42")).thenReturn(create);
        when(servletRequest.getMethod()).thenReturn("GET");
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(create);
        verify(handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostOutsideBox() throws Exception
    {
        Create create = new Create().setName("Create 42");
        when(servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("create", "42");
        when(this.activityPubStorage.retrieveEntity("42")).thenReturn(create);
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(400, "POST requests are only allowed on inbox or outbox.");
        verify(handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostInUnattributedBox() throws Exception
    {
        Inbox inbox = new Inbox();
        when(servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("inbox", "42");
        when(this.activityPubStorage.retrieveEntity("42")).thenReturn(inbox);
        this.handler.handle(resourceReference, this.handlerChain);
        this.verifyResponse(500,
            "This box is not attributed. Please report the error to the administrator.");
        verify(handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostInbox() throws Exception
    {
        Person person = new Person().setPreferredUsername("Foo");
        ActivityPubObjectReference<AbstractActor> actorReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(person);
        when(this.objectReferenceResolver.resolveReference(actorReference)).thenReturn(person);
        Inbox inbox = new Inbox().setAttributedTo(Collections.singletonList(actorReference));
        when(servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("inbox", "42");
        when(this.activityPubStorage.retrieveEntity("42")).thenReturn(inbox);

        Create create = new Create().setName("Create 42");
        when(servletRequest.getReader()).thenReturn(new BufferedReader(new StringReader("{create:42}")));
        when(activityPubJsonParser.parse("{create:42}")).thenReturn(create);
        ActivityHandler<Create> activityHandler = this.componentManager
            .registerMockComponent(new DefaultParameterizedType(null, ActivityHandler.class, Create.class));

        this.handler.handle(resourceReference, this.handlerChain);

        verify(activityHandler, times(1))
            .handleInboxRequest(new ActivityRequest<>(person, create, servletRequest, servletResponse));
        verify(handlerChain, times(1)).handleNext(resourceReference);
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
        Outbox outbox = new Outbox().setAttributedTo(Collections.singletonList(actorReference));
        when(servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("outbox", "42");
        when(this.activityPubStorage.retrieveEntity("42")).thenReturn(outbox);

        Create create = new Create().setName("Create 42");
        when(servletRequest.getReader()).thenReturn(new BufferedReader(new StringReader("{create:42}")));
        when(activityPubJsonParser.parse("{create:42}")).thenReturn(create);
        ActivityHandler<Create> activityHandler = this.componentManager
            .registerMockComponent(new DefaultParameterizedType(null, ActivityHandler.class, Create.class));

        this.handler.handle(resourceReference, this.handlerChain);

        verify(activityHandler, never())
            .handleOutboxRequest(new ActivityRequest<>(person, create, servletRequest, servletResponse));
        verifyResponse(403, "The session user [null] cannot post to [FooUser] outbox.");
        verify(handlerChain, times(1)).handleNext(resourceReference);
    }

    @Test
    void handlePostOutboxAuthorized() throws Exception
    {
        Person person = new Person().setPreferredUsername("Foo");
        UserReference fooUserReference = mock(UserReference.class);
        when(this.actorHandler.getXWikiUserReference(person)).thenReturn(fooUserReference);

        DocumentReference userDocumentReference = mock(DocumentReference.class);
        when(this.xWikiUserBridge.resolveDocumentReference(userDocumentReference)).thenReturn(fooUserReference);
        when(this.xWikiContext.getUserReference()).thenReturn(userDocumentReference);
        ActivityPubObjectReference<AbstractActor> actorReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(person);
        when(this.objectReferenceResolver.resolveReference(actorReference)).thenReturn(person);
        Outbox outbox = new Outbox().setAttributedTo(Collections.singletonList(actorReference));
        when(servletRequest.getMethod()).thenReturn("POST");
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("outbox", "42");
        when(this.activityPubStorage.retrieveEntity("42")).thenReturn(outbox);

        Create create = new Create().setName("Create 42");
        when(servletRequest.getReader()).thenReturn(new BufferedReader(new StringReader("{create:42}")));
        when(activityPubJsonParser.parse("{create:42}")).thenReturn(create);
        ActivityHandler<Create> activityHandler = this.componentManager
            .registerMockComponent(new DefaultParameterizedType(null, ActivityHandler.class, Create.class));

        this.handler.handle(resourceReference, this.handlerChain);

        verify(activityHandler, times(1))
            .handleOutboxRequest(new ActivityRequest<>(person, create, servletRequest, servletResponse));
        verify(handlerChain, times(1)).handleNext(resourceReference);
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
}
