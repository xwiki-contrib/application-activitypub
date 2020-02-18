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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;

@Component
@Singleton
@Named("activitypub")
public class ActivityPubNotificationDisplayer implements NotificationDisplayer
{
    @Inject
    private Logger logger;

    @Inject
    private ActivityPubActivityNotificationDisplayer activityPubActivityNotificationDisplayer;

    @Inject
    private ActivityPubJsonParser activityPubJsonParser;

    @Override
    public Block renderNotification(CompositeEvent compositeEvent) throws NotificationException
    {
        Block result = new GroupBlock();
        result.addChildren(this.processEvents(compositeEvent.getEvents()));
        return result;
    }

    private List<Block> processEvents(List<Event> events) throws NotificationException
    {
        List<Block> result = new ArrayList<>();
        for (Event event : events) {
            if (event.getParameters().containsKey("activity")) {
                AbstractActivity activity = this.getActivity(event);
                result.add(activityPubActivityNotificationDisplayer.displayActivityNotification(event, activity));
            } else {
                this.logger.error("The event [{}] cannot be processed.", event);
            }
        }
        return result;
    }

    private AbstractActivity getActivity(Event event)
    {
        String activity = event.getParameters().get("activity");
        return this.activityPubJsonParser.parse(activity);
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return Arrays.asList("activitypub");
    }
}
