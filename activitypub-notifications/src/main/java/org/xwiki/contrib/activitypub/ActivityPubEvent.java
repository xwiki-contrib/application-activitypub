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
package org.xwiki.contrib.activitypub;

import java.util.Set;

import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.TargetableEvent;
import org.xwiki.stability.Unstable;

/**
 * Defines the ActivityPub event to be displayed as Notifications.
 * @param <T> the type of the activity
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class ActivityPubEvent<T extends AbstractActivity> implements RecordableEvent, TargetableEvent
{
    private Set<String> target;
    private T activity;

    /**
     * Default constructor.
     * @param activity the activity to notify about
     * @param target the serialized references of users to notify to
     */
    public ActivityPubEvent(T activity, Set<String> target)
    {
        this.activity = activity;
        this.target = target;
    }

    @Override
    public Set<String> getTarget()
    {
        return target;
    }

    /**
     * @return the activity this event is about.
     */
    public T getActivity()
    {
        return activity;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent != null && otherEvent instanceof ActivityPubEvent;
    }
}
