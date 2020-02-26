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
import java.util.Collections;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

import liquibase.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultActivityPubStorage}.
 *
 * @version $Id$
 */
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

    @MockComponent
    private ActivityPubObjectReferenceResolver resolver;

    @MockComponent
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> urlResolver;

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

    @Test
    public void storeEntityWithoutID() throws Exception
    {
        // Test store of an entity without id.
        ActivityPubObject object1 = new ActivityPubObject().setName("foo");
        when(this.jsonSerializer.serialize(object1)).thenReturn("{foo}");
        when(this.jsonParser.parse("{foo}")).thenReturn(object1);

        String uid = this.activityPubStorage.storeEntity(object1);
        assertFalse(StringUtils.isEmpty(uid));
        assertSame(object1, this.activityPubStorage.retrieveEntity(uid));
    }

    @Test
    public void storeEntityWithID() throws Exception
    {
        // Test store of an entity with ID
        URL object2URL = new URL("http://www.xwiki.org/xwiki/activitypub/object/42");
        ActivityPubObject object2 = new ActivityPubObject()
            .setName("foobar")
            .setId(object2URL.toURI());
        when(this.jsonSerializer.serialize(object2)).thenReturn("{foobar}");
        when(this.jsonParser.parse("{foobar}")).thenReturn(object2);
        ExtendedURL object2ExtendedURL = new ExtendedURL(object2URL, "xwiki/activitypub");
        when(this.urlResolver.resolve(object2ExtendedURL, new ResourceType("activitypub"), Collections.emptyMap()))
            .thenReturn(new ActivityPubResourceReference("object", "42"));

        // ID does not match server URL
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new ActivityPubObject().setId(new URI("http://anotherwiki.org/12")));
        });
        assertEquals("Entity [http://anotherwiki.org/12] won't be stored since it's not part of the current instance",
            activityPubException.getMessage());

        // ID match server URL
        assertEquals("42", this.activityPubStorage.storeEntity(object2));
        assertSame(object2, this.activityPubStorage.retrieveEntity("42"));
    }

    @Test
    public void storeActor() throws Exception
    {
        // Test store of an Actor
        AbstractActor actor = new Person().setPreferredUsername("FooBar");
        when(this.jsonSerializer.serialize(actor)).thenReturn("{user:FooBar}");
        when(this.jsonParser.parse("{user:FooBar}")).thenReturn(actor);

        assertEquals("FooBar", this.activityPubStorage.storeEntity(actor));
        assertSame(actor, this.activityPubStorage.retrieveEntity("FooBar"));
    }

    @Test
    public void storeBox() throws Exception
    {
        // Test store of an Inbox
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new Inbox());
        });

        assertEquals("Cannot store an inbox without owner.", activityPubException.getMessage());

        Person actor = new Person().setPreferredUsername("FooBar");
        ActivityPubObjectReference<AbstractActor> objectReference =
            new ActivityPubObjectReference<AbstractActor>().setObject(actor);
        when(this.resolver.resolveReference(objectReference)).thenReturn(actor);
        Inbox inbox = new Inbox().setAttributedTo(Collections.singletonList(objectReference));
        when(this.jsonSerializer.serialize(inbox)).thenReturn("{inbox:FooBar}");
        when(this.jsonParser.parse("{inbox:FooBar}")).thenReturn(inbox);

        assertEquals("FooBar-inbox", this.activityPubStorage.storeEntity(inbox));
        assertSame(inbox, this.activityPubStorage.retrieveEntity("FooBar-inbox"));

        // Test store of an Outbox
        activityPubException = assertThrows(ActivityPubException.class, () -> {
            this.activityPubStorage.storeEntity(new Outbox());
        });
        assertEquals("Cannot store an outbox without owner.", activityPubException.getMessage());

        Outbox outbox = new Outbox().setAttributedTo(Collections.singletonList(objectReference));
        when(this.jsonSerializer.serialize(outbox)).thenReturn("{outbox:FooBar}");
        when(this.jsonParser.parse("{outbox:FooBar}")).thenReturn(outbox);
        assertEquals("FooBar-outbox", this.activityPubStorage.storeEntity(outbox));
        assertSame(outbox, this.activityPubStorage.retrieveEntity("FooBar-outbox"));
    }
}
