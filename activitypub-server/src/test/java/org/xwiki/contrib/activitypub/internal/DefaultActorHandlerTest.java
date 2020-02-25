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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ComponentTest
public class DefaultActorHandlerTest
{
    @InjectMockComponents
    private DefaultActorHandler actorHandler;

    private XWikiContext context;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @BeforeComponent
    public void setup(MockitoComponentManager componentManager) throws Exception
    {
        Type providerContextType = new DefaultParameterizedType(null, Provider.class, XWikiContext.class);
        Provider<XWikiContext> contextProvider = componentManager.registerMockComponent(providerContextType);
        this.context = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(context);
        Utils.setComponentManager(componentManager);
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    public void getCurrentActorWithGuest()
    {
        ActivityPubException activityPubException = assertThrows(ActivityPubException.class, () -> {
            actorHandler.getCurrentActor();
        });
        assertEquals("The context does not have any user reference, the request might be anonymous.",
            activityPubException.getMessage());
        verify(context, times(1)).getUserReference();
    }

    @Test
    public void getActorWithAlreadyExistingActor() throws Exception
    {
        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "Foo");
        XWikiDocument fooDoc = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance((EntityReference)userRef)).thenReturn(fooDoc);

        BaseObject userObject = mock(BaseObject.class);
        when(fooDoc.getXObject(any(EntityReference.class))).thenReturn(userObject);
        when(this.localEntityReferenceSerializer.serialize(userRef)).thenReturn("XWiki.Foo");

        AbstractActor expectedActor = new Person().setPreferredUsername("XWiki.Foo");
        when(this.activityPubStorage.retrieveEntity("actor", "XWiki.Foo")).thenReturn(expectedActor);

        assertSame(expectedActor, this.actorHandler.getActor(userRef));
    }

    @Test
    public void getActorWithExistingActor() throws Exception
    {
        DocumentReference userRef = new DocumentReference("xwiki", "XWiki", "Foo");
        XWikiDocument fooDoc = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getDocumentInstance((EntityReference)userRef)).thenReturn(fooDoc);

        BaseObject userObject = mock(BaseObject.class);
        when(fooDoc.getXObject(any(EntityReference.class))).thenReturn(userObject);
        when(this.localEntityReferenceSerializer.serialize(userRef)).thenReturn("XWiki.Foo");
        when(userObject.getStringValue("first_name")).thenReturn("Foo");
        when(userObject.getStringValue("last_name")).thenReturn("Foo");

        AbstractActor expectedActor = new Person()
            .setPreferredUsername("XWiki.Foo")
            .setInbox(new ActivityPubObjectReference<Inbox>().setObject(new Inbox()))
            .setOutbox(new ActivityPubObjectReference<Outbox>().setObject(new Outbox()))
            .setFollowers(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setFollowing(
                new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(new OrderedCollection<>()))
            .setName("Foo Foo");

        assertEquals(expectedActor, this.actorHandler.getActor(userRef));
    }
}
