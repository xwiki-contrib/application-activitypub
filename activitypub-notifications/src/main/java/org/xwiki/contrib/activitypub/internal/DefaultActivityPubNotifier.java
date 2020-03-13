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

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubNotifier;
import org.xwiki.contrib.activitypub.ActivityPubEvent;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.observation.ObservationManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Default implementation of the notifier: basically it creates an {@link ActivityPubEvent} and send it to the
 * {@link ObservationManager}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubNotifier implements ActivityPubNotifier
{
    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private ObservationManager observationManager;

    @Override
    public <T extends AbstractActivity> void notify(T activity, Set<UserReference> targets)
    {
        ActivityPubEvent<T> event = new ActivityPubEvent<T>(activity, this.serializeTargets(targets));
        this.observationManager.notify(event, "org.xwiki.contrib:activitypub-notifications", activity.getType());
    }

    private Set<String> serializeTargets(Set<UserReference> targets)
    {
        Set<String> result = new HashSet<>();
        for (UserReference target : targets) {
            result.add(this.userReferenceSerializer.serialize(target));
        }
        return result;
    }
}
