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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.events.MessageEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import static java.util.Collections.singletonList;
import static org.xwiki.contrib.activitypub.internal.ActivityPubNewMessagesListener.TYPE;

/**
 * Event listener for the ActivityPub messages received from the fediverse. New discussions are created from the new
 * messages if they are not already part of a discussion.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(TYPE)
@Singleton
public class ActivityPubNewMessagesListener implements EventListener
{
    /**
     * Type of the component.
     */
    public static final String TYPE = "ActivityPubNewMessagesListener";

    @Inject
    private ActivityPubDiscussionsService activityPubDiscussionsService;

    @Override
    public String getName()
    {
        return TYPE;
    }

    @Override
    public List<Event> getEvents()
    {
        return singletonList(new MessageEvent(null, null));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof MessageEvent) {
            MessageEvent messageEvent = (MessageEvent) event;
            try {
                this.activityPubDiscussionsService.handleActivity(messageEvent.getActivity());
            } catch (ActivityPubException e) {
                e.printStackTrace();
                // TODO log
            }
        }
    }
}
