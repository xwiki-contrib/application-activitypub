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

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ActivityPubResourceReferenceSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
public class ActivityPubResourceReferenceSerializerTest
{
    private static final String DEFAULT_URL = "http://www.xwiki.org";

    @InjectMockComponents
    private ActivityPubResourceReferenceSerializer serializer;

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        DefaultURLHandler urlHandler = componentManager.registerMockComponent(DefaultURLHandler.class);
        when(urlHandler.getServerUrl()).thenReturn(new URL(DEFAULT_URL));

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
