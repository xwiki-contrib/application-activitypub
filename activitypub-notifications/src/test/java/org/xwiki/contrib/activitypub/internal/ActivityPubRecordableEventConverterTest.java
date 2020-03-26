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

import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.FollowEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.activitypub.internal.ActivityPubRecordableEventConverter.ACTIVITY_PARAMETER_KEY;

/**
 * Test of {@link ActivityPubRecordableEventConverter}.
 * 
 * @since 1.0
 * @version $Id$
 */
@ComponentTest
public class ActivityPubRecordableEventConverterTest
{
    @InjectMockComponents
    private ActivityPubRecordableEventConverter activityPubRecordableEventConverter;

    @MockComponent
    private RecordableEventConverter recordableEventConverter;

    @MockComponent
    private ActivityPubJsonSerializer activityPubJsonSerializer;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @MockComponent
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @Test
    void testConvertWithUser() throws Exception
    {
        DefaultEvent e = new DefaultEvent();
        e.setUser(new DocumentReference("xwiki", "XWiki", "jdoe"));
        when(this.recordableEventConverter.convert(any(), any(), any())).thenReturn(e);
        when(this.activityPubJsonSerializer.serialize(any())).thenReturn("Serialized AP");
        when(this.stringDocumentReferenceResolver.resolve(any())).thenReturn(new DocumentReference("xwiki",
            "XWiki", "Foo"));
        Person person = new Person();
        person.setPreferredUsername("John Doe");
        when(this.objectReferenceResolver.resolveReference(any())).thenReturn(person);
        when(this.stringDocumentReferenceResolver.resolve(anyString()))
            .thenReturn(new DocumentReference("xwiki", "XWiki", "user"));

        RecordableEvent recordableEvent = new FollowEvent<>(new Accept(), null);
        String source = null;
        Object data = null;
        Event actual =
            this.activityPubRecordableEventConverter.convert(recordableEvent, source, data);
        assertNotNull(actual);
        assertEquals("Serialized AP", actual.getParameters().get(ACTIVITY_PARAMETER_KEY));
        assertEquals(FollowEvent.EVENT_TYPE, actual.getType());
        assertEquals("ActivityPub", actual.getUser().getName());
    }

    @Test
    void convertWithoutUser() throws Exception
    {
        when(this.recordableEventConverter.convert(any(), any(), any())).thenReturn(new DefaultEvent());
        when(this.activityPubJsonSerializer.serialize(any())).thenReturn("Serialized AP");
        when(this.stringDocumentReferenceResolver.resolve(any())).thenReturn(new DocumentReference("xwiki",
            "XWiki", "Foo"));
        Person person = new Person();
        person.setPreferredUsername("John Doe");
        when(this.objectReferenceResolver.resolveReference(any())).thenReturn(person);
        when(this.stringDocumentReferenceResolver.resolve(anyString()))
            .thenReturn(new DocumentReference("xwiki", "XWiki", "user"));

        RecordableEvent recordableEvent = new FollowEvent<>(new Accept(), null);
        String source = null;
        Object data = null;
        Event actual =
            this.activityPubRecordableEventConverter.convert(recordableEvent, source, data);
        assertNotNull(actual);
        assertEquals("Serialized AP", actual.getParameters().get(ACTIVITY_PARAMETER_KEY));
        assertEquals(FollowEvent.EVENT_TYPE, actual.getType());
        assertEquals("ActivityPub", actual.getUser().getName());
    }

    @Test
    void supportedEventsDefault()
    {
        List<RecordableEvent> actual =
            this.activityPubRecordableEventConverter.getSupportedEvents();
        assertEquals(2, actual.size());
        assertEquals(CreateEvent.class, actual.get(0).getClass());
        assertEquals(FollowEvent.class, actual.get(1).getClass());
    }
}
