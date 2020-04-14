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
package org.xwiki.contrib.activitypub.webfinger.internal;

import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.WebfingerResourceReference;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.contrib.activitypub.webfinger.entities.Link;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Test of {@link WebfingerResourceReferenceHandler}.
 *
 * @since 1.1
 * @version $Id$
 */
@ComponentTest
public class WebfingerResourceReferenceHandlerTest
{
    @InjectMockComponents
    private WebfingerResourceReferenceHandler handler;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private WebfingerService webfingerService;

    @MockComponent
    private WebfingerJsonSerializer webfingerJsonSerializer;

    @MockComponent
    private XWikiUserBridge xWikiUserBridge;

    private HttpServletResponse httpServletResponse;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        Container container = this.componentManager.registerMockComponent(Container.class);
        ServletResponse servletResponse = mock(ServletResponse.class);
        when(container.getResponse()).thenReturn(servletResponse);
        this.httpServletResponse = mock(HttpServletResponse.class);
        when(servletResponse.getHttpServletResponse()).thenReturn(this.httpServletResponse);
        when(this.httpServletResponse.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
    }

    @BeforeEach
    public void beforeEach()
    {
        when(this.xWikiUserBridge.isExistingUser(any(String.class))).thenReturn(true);
    }

    @Test
    void handleResourceEmpty() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse).setStatus(400);
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceMissing() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        when(reference.getParameterValue("resource")).thenReturn(null);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse).setStatus(400);
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceMalformed() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String invalidResource = "aaa";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse).setStatus(404);
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResolutionFails() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String invalidResource = "aaa";
        when(this.xWikiUserBridge.isExistingUser((String)null)).thenReturn(true);
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        WebfingerException expt = mock(WebfingerException.class);
        when(expt.getMessage()).thenReturn("error message");
        when(expt.getErrorCode()).thenReturn(500);
        when(this.webfingerService.resolveActivityPubUser(null)).thenThrow(expt);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse).setStatus(500);
        verify(this.httpServletResponse).setContentType("text/plain");
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceInvalidHostname() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String invalidResource = "aaa";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        URI uri = new URI("acct://host1.net");
        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getId()).thenReturn(uri);
        when(this.webfingerService.resolveActivityPubUser(null)).thenReturn(abstractActor);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse).setStatus(404);
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceValid() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String resource = "XWiki.Admin@xwiki.tst";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(resource));
        when(reference.getParameterValue("resource")).thenReturn(resource);
        when(reference.getParameterValues("rel")).thenReturn(null);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        URI uri = new URI("https://xwiki.tst");
        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getId()).thenReturn(uri);
        when(this.webfingerService.resolveActivityPubUser("XWiki.Admin")).thenReturn(abstractActor);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse, times(0)).setStatus(anyInt());
        verify(this.httpServletResponse).setContentType("application/jrd+json");
        Link link1 =
            new Link().setRel("http://webfinger.net/rel/profile-page").setType("text/html").setHref(null);
        Link link2 = new Link().setRel("self").setType("application/activity+json")
            .setHref(URI.create("https://xwiki.tst"));
        JSONResourceDescriptor expectedJSONResourceDescriptor = new JSONResourceDescriptor()
                                                .setSubject("acct:XWiki.Admin@xwiki.tst")
                                                .setLinks(asList(link1, link2));
        verify(this.webfingerJsonSerializer)
            .serialize(ArgumentMatchers.nullable(OutputStream.class), eq(expectedJSONResourceDescriptor));
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceValidWithAcct() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String resource = "acct:XWiki.Admin@xwiki.tst";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(resource));
        when(reference.getParameterValue("resource")).thenReturn(resource);
        when(reference.getParameterValues("rel")).thenReturn(null);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        URI uri = new URI("https://xwiki.tst");
        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getId()).thenReturn(uri);
        when(this.webfingerService.resolveActivityPubUser("XWiki.Admin")).thenReturn(abstractActor);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse, times(0)).setStatus(anyInt());
        verify(this.httpServletResponse).setContentType("application/jrd+json");
        Link link1 =
            new Link().setRel("http://webfinger.net/rel/profile-page").setType("text/html").setHref(null);
        Link link2 = new Link().setRel("self").setType("application/activity+json")
            .setHref(URI.create("https://xwiki.tst"));
        JSONResourceDescriptor expectedJSONResourceDescriptor = new JSONResourceDescriptor()
                                                                    .setSubject("acct:XWiki.Admin@xwiki.tst")
                                                                    .setLinks(asList(link1, link2));
        verify(this.webfingerJsonSerializer)
            .serialize(ArgumentMatchers.nullable(OutputStream.class), eq(expectedJSONResourceDescriptor));
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceValidWithRelSelf() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String invalidResource = "acct:XWiki.Admin@xwiki.tst";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        when(reference.getParameterValues("rel")).thenReturn(singletonList("self"));
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        URI uri = new URI("https://xwiki.tst");
        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getId()).thenReturn(uri);
        when(this.webfingerService.resolveActivityPubUser("XWiki.Admin")).thenReturn(abstractActor);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse, times(0)).setStatus(anyInt());
        verify(this.httpServletResponse).setContentType("application/jrd+json");
        Link link = new Link().setRel("self").setType("application/activity+json")
            .setHref(URI.create("https://xwiki.tst"));
        JSONResourceDescriptor expectedJSONResourceDescriptor = new JSONResourceDescriptor()
                                                .setSubject("acct:XWiki.Admin@xwiki.tst")
                                                .setLinks(singletonList(link));
        verify(this.webfingerJsonSerializer)
            .serialize(ArgumentMatchers.nullable(OutputStream.class), eq(expectedJSONResourceDescriptor));
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceValidWithRelProfilePage() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String invalidResource = "acct:XWiki.Admin@xwiki.tst";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        when(reference.getParameterValues("rel")).thenReturn(singletonList("http://webfinger.net/rel/profile-page"));
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        URI uri = new URI("https://xwiki.tst");
        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getId()).thenReturn(uri);
        when(this.webfingerService.resolveActivityPubUser("XWiki.Admin")).thenReturn(abstractActor);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse, times(0)).setStatus(anyInt());
        verify(this.httpServletResponse).setContentType("application/jrd+json");
        Link link = new Link().setRel("http://webfinger.net/rel/profile-page").setType("text/html").setHref(null);
        JSONResourceDescriptor expectedJSONResourceDescriptor = new JSONResourceDescriptor()
                                                .setSubject("acct:XWiki.Admin@xwiki.tst")
                                                .setLinks(singletonList(link));
        verify(this.webfingerJsonSerializer)
            .serialize(ArgumentMatchers.nullable(OutputStream.class), eq(expectedJSONResourceDescriptor));
        verify(chain).handleNext(reference);
    }

    @Test
    void handleUnknownParameters() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        HashMap<String, List<String>> value = new HashMap<>();
        value.put("unknown", asList("a", "b"));
        when(reference.getParameters()).thenReturn(value);
        String invalidResource = "acct:XWiki.Admin@xwiki.tst";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        when(reference.getParameterValues("rel")).thenReturn(singletonList("http://webfinger.net/rel/profile-page"));
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        URI uri = new URI("https://xwiki.tst");
        AbstractActor abstractActor = mock(AbstractActor.class);
        when(abstractActor.getId()).thenReturn(uri);
        when(this.webfingerService.resolveActivityPubUser("XWiki.Admin")).thenReturn(abstractActor);
        this.handler.handle(reference, chain);
        assertEquals(1, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0).endsWith("] contains unknown parameters [unknown]"));
    }
}