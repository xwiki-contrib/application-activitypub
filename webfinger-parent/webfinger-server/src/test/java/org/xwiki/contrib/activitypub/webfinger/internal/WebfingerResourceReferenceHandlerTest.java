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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.WebfingerResourceReference;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.contrib.activitypub.webfinger.entities.LinkJRD;
import org.xwiki.contrib.activitypub.webfinger.entities.WebfingerJRD;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.SerializeResourceReferenceException;
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
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        when(this.webfingerService.isExistingUser(Mockito.nullable(DocumentReference.class))).thenReturn(true);
        SerializeResourceReferenceException expt = mock(SerializeResourceReferenceException.class);
        when(expt.getMessage()).thenReturn("error message");
        when(this.webfingerService.resolveActivityPubUserUrl(null)).thenThrow(expt);
        when(this.httpServletResponse.getOutputStream()).thenReturn(mock(ServletOutputStream.class));
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
        when(this.webfingerService.isExistingUser(Mockito.nullable(DocumentReference.class))).thenReturn(true);
        URI uri = new URI("acct://host1.net");
        when(this.webfingerService.resolveActivityPubUserUrl(null)).thenReturn(uri);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse).setStatus(404);
        verify(chain).handleNext(reference);
    }

    @Test
    void handleResourceValid() throws Exception
    {
        ResourceReference reference = mock(WebfingerResourceReference.class);
        String invalidResource = "acct:XWiki.Admin@xwiki.tst";
        when(reference.getParameterValues("resource")).thenReturn(singletonList(invalidResource));
        when(reference.getParameterValue("resource")).thenReturn(invalidResource);
        when(reference.getParameterValues("rel")).thenReturn(null);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        when(this.webfingerService.isExistingUser(Mockito.nullable(DocumentReference.class))).thenReturn(true);
        URI uri = new URI("https://xwiki.tst");
        when(this.webfingerService.resolveActivityPubUserUrl("XWiki.Admin")).thenReturn(uri);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse, times(0)).setStatus(anyInt());
        verify(this.httpServletResponse).setContentType("application/activity+json; charset=utf-8");
        LinkJRD link1 =
            new LinkJRD().setRel("http://webfinger.net/rel/profile-page").setType("text/html").setHref(null);
        LinkJRD link2 = new LinkJRD().setRel("self").setType("application/activity+json").setHref("https://xwiki.tst");
        WebfingerJRD expectedWebfingerJRD = new WebfingerJRD()
                                                .setSubject("acct:acct:XWiki.Admin@xwiki.tst")
                                                .setLinks(asList(link1, link2));
        verify(this.webfingerJsonSerializer)
            .serialize(ArgumentMatchers.nullable(OutputStream.class), eq(expectedWebfingerJRD));
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
        when(this.webfingerService.isExistingUser(Mockito.nullable(DocumentReference.class))).thenReturn(true);
        URI uri = new URI("https://xwiki.tst");
        when(this.webfingerService.resolveActivityPubUserUrl("XWiki.Admin")).thenReturn(uri);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse, times(0)).setStatus(anyInt());
        verify(this.httpServletResponse).setContentType("application/activity+json; charset=utf-8");
        LinkJRD link = new LinkJRD().setRel("self").setType("application/activity+json").setHref("https://xwiki.tst");
        WebfingerJRD expectedWebfingerJRD = new WebfingerJRD()
                                                .setSubject("acct:acct:XWiki.Admin@xwiki.tst")
                                                .setLinks(singletonList(link));
        verify(this.webfingerJsonSerializer)
            .serialize(ArgumentMatchers.nullable(OutputStream.class), eq(expectedWebfingerJRD));
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
        when(this.webfingerService.isExistingUser(Mockito.nullable(DocumentReference.class))).thenReturn(true);
        URI uri = new URI("https://xwiki.tst");
        when(this.webfingerService.resolveActivityPubUserUrl("XWiki.Admin")).thenReturn(uri);
        this.handler.handle(reference, chain);
        verify(this.httpServletResponse, times(0)).setStatus(anyInt());
        verify(this.httpServletResponse).setContentType("application/activity+json; charset=utf-8");
        LinkJRD link = new LinkJRD().setRel("http://webfinger.net/rel/profile-page").setType("text/html").setHref(null);
        WebfingerJRD expectedWebfingerJRD = new WebfingerJRD()
                                                .setSubject("acct:acct:XWiki.Admin@xwiki.tst")
                                                .setLinks(singletonList(link));
        verify(this.webfingerJsonSerializer)
            .serialize(ArgumentMatchers.nullable(OutputStream.class), eq(expectedWebfingerJRD));
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
        when(this.webfingerService.isExistingUser(Mockito.nullable(DocumentReference.class))).thenReturn(true);
        URI uri = new URI("https://xwiki.tst");
        when(this.webfingerService.resolveActivityPubUserUrl("XWiki.Admin")).thenReturn(uri);
        this.handler.handle(reference, chain);
        assertEquals(1, this.logCapture.size());
        assertTrue(this.logCapture.getMessage(0).endsWith("] contains unknown parameters [unknown]"));
    }
}