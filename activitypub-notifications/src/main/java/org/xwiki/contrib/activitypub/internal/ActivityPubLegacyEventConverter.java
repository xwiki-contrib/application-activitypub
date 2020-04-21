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

import java.util.Collections;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.events.AnnounceEvent;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.FollowEvent;
import org.xwiki.contrib.activitypub.events.MessageEvent;
import org.xwiki.contrib.activitypub.events.UpdateEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.store.internal.AbstractLegacyEventConverter;
import org.xwiki.eventstream.store.internal.LegacyEvent;

import static org.xwiki.contrib.activitypub.internal.ActivityPubRecordableEventConverter.ACTIVITY_PARAMETER_KEY;

/**
 * We need this custom legacy converter to ensure that the activity parameters from the
 * {@link org.xwiki.eventstream.internal.DefaultEvent} are set in the parameter3 of the {@link LegacyEvent}: this is
 * what will be stored.
 *
 * @version $Id$
 * @since 1.0
 */
// FIXME: we should have a better management of type once XWIKI-17156 is fixed.
@Component(hints = {
    CreateEvent.EVENT_TYPE,
    FollowEvent.EVENT_TYPE,
    AnnounceEvent.EVENT_TYPE,
    MessageEvent.EVENT_TYPE,
    UpdateEvent.EVENT_TYPE
})
@Singleton
public class ActivityPubLegacyEventConverter extends AbstractLegacyEventConverter
{
    @Override
    public LegacyEvent convertEventToLegacyActivity(Event e)
    {
        LegacyEvent event = super.convertEventToLegacyActivity(e);
        if (e.getParameters().containsKey(ACTIVITY_PARAMETER_KEY)) {
            event.setParam3(e.getParameters().get(ACTIVITY_PARAMETER_KEY));
        }
        return event;
    }

    @Override
    public Event convertLegacyActivityToEvent(LegacyEvent e)
    {
        Event event = super.convertLegacyActivityToEvent(e);
        if (!StringUtils.isEmpty(e.getParam3())) {
            event.setParameters(Collections.singletonMap(ACTIVITY_PARAMETER_KEY, e.getParam3()));
        }
        return event;
    }
}
