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

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.stability.Unstable;

/**
 * External API to send an ActivityPub notification.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
@Role
public interface ActivityPubNotifier
{
    /**
     * Send a notification related to the given activity to the given targets.
     *
     * @param activity the activity source of the notification.
     * @param targetedActors the target actors of the notification.
     * @param <T> the real type of the activity
     * @throws ActivityPubException if the activity is not supported by any event.
     */
    <T extends AbstractActivity> void notify(T activity, Set<AbstractActor> targetedActors) throws ActivityPubException;
}
