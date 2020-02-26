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
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultActivityPubStorageTest
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
    private DefaultActivityPubStorage activityPubStorage;

    @MockComponent
    private ActivityPubJsonParser jsonParser;

    @MockComponent
    private ActivityPubJsonSerializer jsonSerializer;

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
    }

    @Test
    public void storeEntityFromAnotherInstance()
    {
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new ActivityPubObject().setId(new URI("http://anotherwiki.org/12")));
        });

        assertEquals("Entity [http://anotherwiki.org/12] won't be stored since it's not part of the current instance",
            activityPubException.getMessage());
    }

    @Test
    public void storeAndRetrieveEntityWithUID() throws ActivityPubException
    {
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity("", new ActivityPubObject());
        });

        assertEquals("The UID cannot be empty.", activityPubException.getMessage());

        activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity((String)null, new ActivityPubObject());
        });

        assertEquals("The UID cannot be empty.", activityPubException.getMessage());

        assertNull(this.activityPubStorage.retrieveEntity("foo"));

        ActivityPubObject object1 = new ActivityPubObject().setName("foo");
        ActivityPubObject object2 = new ActivityPubObject().setName("foobar");
        when(this.jsonSerializer.serialize(object1)).thenReturn("{foo}");
        when(this.jsonSerializer.serialize(object2)).thenReturn("{foobar}");
        when(this.jsonParser.parse("{foo}")).thenReturn(object1);
        when(this.jsonParser.parse("{foobar}")).thenReturn(object2);

        assertTrue(this.activityPubStorage.storeEntity("foo", object1));
        assertSame(object1, this.activityPubStorage.retrieveEntity("foo"));

        assertFalse(this.activityPubStorage.storeEntity("foo", object2));
        assertSame(object2, this.activityPubStorage.retrieveEntity("foo"));
    }
}
