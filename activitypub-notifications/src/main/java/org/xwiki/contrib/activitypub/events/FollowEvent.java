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
package org.xwiki.contrib.activitypub.events;

import java.util.Set;

import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.stability.Unstable;

/**
 * A specific events when it concerns the follow activities.
 * This event is voluntarily not bounded to {@link org.xwiki.contrib.activitypub.entities.Follow} type, so we can
 * also handle {@link org.xwiki.contrib.activitypub.entities.Accept} or
 * {@link org.xwiki.contrib.activitypub.entities.Reject} activities with it, since they are related to the follow
 * activities.
 *
 * @param <T> the actual type of the activity.
 * @version $Id$
 * @since 1.1
 */
@Unstable
public class FollowEvent<T extends AbstractActivity> extends AbstractActivityPubEvent<T>
{
    /**
     * Default name for those events.
     */
    public static final String EVENT_TYPE = "activitypub.follow";

    /**
     * Default constructor.
     *
     * @param activity the activity to notify about
     * @param target the serialized references of users to notify to
     */
    public FollowEvent(T activity, Set<String> target)
    {
        super(activity, target);
    }

    @Override
    public String getType()
    {
        return EVENT_TYPE;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent != null && otherEvent instanceof FollowEvent;
    }
}
