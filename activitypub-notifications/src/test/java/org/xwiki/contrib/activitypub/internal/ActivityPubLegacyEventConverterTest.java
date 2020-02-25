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

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.store.internal.LegacyEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.activitypub.internal.ActivityPubRecordableEventConverter.ACTIVITY_PARAMETER_KEY;

@ComponentTest
public class ActivityPubLegacyEventConverterTest
{
    @InjectMockComponents
    private ActivityPubLegacyEventConverter activityPubLegacyEventConverter;

    @MockComponent
    private EventFactory eventFactory;

    @MockComponent
    private EntityReferenceResolver<String> resolver;

    @Test
    public void testConvertEventToLegacyActivityWithActivityParameterKey()
    {
        final DefaultEvent e = new DefaultEvent();
        final Map<String, String> parameters = new HashMap<>();
        parameters.put(ACTIVITY_PARAMETER_KEY, "randomValue");
        e.setParameters(parameters);
        final LegacyEvent actual = this.activityPubLegacyEventConverter.convertEventToLegacyActivity(e);
        Assertions.assertEquals("randomValue", actual.getParam3());
    }

    @Test
    public void testConvertEventToLegacyActivityWithoutActivityParameterKey()
    {
        final DefaultEvent e = new DefaultEvent();
        final LegacyEvent actual = this.activityPubLegacyEventConverter.convertEventToLegacyActivity(e);
        Assertions.assertEquals("", actual.getParam3());
    }

    @Test
    public void testConvertLegacyActivityToEventWithActivityParameterKey()
    {
        mockCreateRawEvent();
        mockEntityReferenceResolve();
        LegacyEvent e = new LegacyEvent();
        e.setParam3("randomValue2");
        Event actual = this.activityPubLegacyEventConverter.convertLegacyActivityToEvent(e);
        Assertions.assertEquals("randomValue2", actual.getParameters().get(ACTIVITY_PARAMETER_KEY));
    }

    @Test
    public void testConvertLegacyActivityToEventWithoutActivityParameterKey()
    {
        mockCreateRawEvent();
        mockEntityReferenceResolve();

        LegacyEvent e = new LegacyEvent();
        Event actual = this.activityPubLegacyEventConverter.convertLegacyActivityToEvent(e);
        Assertions.assertFalse(actual.getParameters().containsKey(ACTIVITY_PARAMETER_KEY));
    }

    private void mockCreateRawEvent()
    {
        when(eventFactory.createRawEvent()).thenReturn(new DefaultEvent());
    }

    private void mockEntityReferenceResolve()
    {
        final EntityReference parent = new SpaceReference("parentTest", "spaceTest1");
        final EntityReference test = new EntityReference("test", EntityType.DOCUMENT, parent);
        when(resolver.resolve(any(), any())).thenReturn(test);
    }
}
