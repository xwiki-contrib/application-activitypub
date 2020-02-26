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

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ActivityPubResourceReferenceSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
public class ActivityPubResourceReferenceSerializerTest
{
    private static URL DEFAULT_URL;

    static {
        try {
            DEFAULT_URL = new URL("http://www.xwiki.org");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @InjectMockComponents
    private ActivityPubResourceReferenceSerializer serializer;

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        Type providerContextType = new DefaultParameterizedType(null, Provider.class, XWikiContext.class);
        Provider<XWikiContext> contextProvider = componentManager.registerMockComponent(providerContextType);
        XWikiContext context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);

        XWikiURLFactory xWikiURLFactory = mock(XWikiURLFactory.class);
        when(context.getURLFactory()).thenReturn(xWikiURLFactory);

        when(xWikiURLFactory.getServerURL(context)).thenReturn(DEFAULT_URL);

        Type urlNormalizerType = new DefaultParameterizedType(null, URLNormalizer.class, ExtendedURL.class);
        URLNormalizer<ExtendedURL> urlNormalizer =
            componentManager.registerMockComponent(urlNormalizerType, "contextpath");

        when(urlNormalizer.normalize(any())).then(invocationOnMock -> invocationOnMock.getArguments()[0]);
    }

    @Test
    public void serialize() throws Exception
    {
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("foobar", "myuid");
        resourceReference.addParameter("param1", "bazbuz");
        resourceReference.addParameter("param2", 42);

        URI expectedURI = new URI("http://www.xwiki.org/activitypub/foobar/myuid?param1=bazbuz&param2=42");
        assertEquals(expectedURI, this.serializer.serialize(resourceReference));
    }
}
