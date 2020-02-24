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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubNotifier;
import org.xwiki.eventstream.RecordableEventDescriptor;

/**
 * An event descriptor for ActivityPub to allows enabling/disabling the notifications.
 *
 * @version $Id$
 */
@Component
@Named("ActivityPubEvent")
@Singleton
public class ActivityPubEventDescriptor implements RecordableEventDescriptor
{
    @Override
    public String getEventType()
    {
        return ActivityPubNotifier.EVENT_TYPE;
    }

    @Override
    public String getApplicationName()
    {
        return "activitypub.application.name";
    }

    @Override
    public String getDescription()
    {
        return "activitypub.application.description";
    }

    @Override
    public String getApplicationIcon()
    {
        return "branch";
    }
}
