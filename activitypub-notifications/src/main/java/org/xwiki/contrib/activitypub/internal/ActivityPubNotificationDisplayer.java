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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.events.AnnounceEvent;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.FollowEvent;
import org.xwiki.contrib.activitypub.events.MessageEvent;
import org.xwiki.contrib.activitypub.events.UpdateEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

import static org.xwiki.contrib.activitypub.internal.ActivityPubRecordableEventConverter.ACTIVITY_PARAMETER_KEY;

/**
 * Standard displayer for all activitypub events.
 * This displayer retrieves velocity templates based on the activity types of the events.
 *
 * @since 1.0
 * @version $Id$
 */
@Component
@Singleton
@Named("activitypub")
public class ActivityPubNotificationDisplayer implements NotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "compositeEvent";
    private static final String ACTIVITY_BINDING_NAME = "eventActivities";
    private static final String EVENT_TYPE_PREFIX = "activitypub.";
    private static final String TEMPLATE_FALLBACK = "activity";
    private static final String TEMPLATE_PATH = "activitypub/%s.vm";

    @Inject
    private ActivityPubJsonParser activityPubJsonParser;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Block renderNotification(CompositeEvent compositeEvent) throws NotificationException
    {
        Block result = new GroupBlock();
        ScriptContext scriptContext = scriptContextManager.getScriptContext();
        Map<Event, AbstractActivity> activities = getActivities(compositeEvent);
        if (activities.isEmpty()) {
            throw new NotificationException("No activity found on the activity composite event.");
        }

        String eventType = compositeEvent.getType();
        if (eventType.contains(EVENT_TYPE_PREFIX)) {
            eventType = eventType.substring(EVENT_TYPE_PREFIX.length());
        } else {
            eventType = TEMPLATE_FALLBACK;
        }
        String templateName = String.format(TEMPLATE_PATH, eventType);

        try {
            scriptContext.setAttribute(EVENT_BINDING_NAME, compositeEvent, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute(ACTIVITY_BINDING_NAME, activities, ScriptContext.ENGINE_SCOPE);

            Template template = templateManager.getTemplate(templateName);
            if (template != null) {
                result.addChildren(templateManager.execute(template).getChildren());
            } else {
                result.addChildren(templateManager.execute(
                    String.format(TEMPLATE_PATH, TEMPLATE_FALLBACK)
                ).getChildren());
            }
        } catch (Exception e) {
            throw new NotificationException("Failed to render the notification.", e);
        } finally {
            scriptContext.removeAttribute(EVENT_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
            scriptContext.removeAttribute(ACTIVITY_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
        }
        return result;
    }

    private Map<Event, AbstractActivity> getActivities(CompositeEvent compositeEvent)
        throws NotificationException
    {
        String errorMessage = "Error while getting the activity of the event [%s]";
        Map<Event, AbstractActivity> result = new HashMap<>();
        for (Event event : compositeEvent.getEvents()) {
            if (event.getParameters().containsKey(ACTIVITY_PARAMETER_KEY)) {
                try {
                    String activity = event.getParameters().get(ACTIVITY_PARAMETER_KEY);
                    result.put(event, this.activityPubJsonParser.parse(activity));
                } catch (ActivityPubException e) {
                    throw new NotificationException(String.format(errorMessage, event), e);
                }
            } else {
                throw new NotificationException(String.format(errorMessage, event));
            }
        }

        return result;
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return Arrays.asList(
            CreateEvent.EVENT_TYPE,
            FollowEvent.EVENT_TYPE,
            AnnounceEvent.EVENT_TYPE,
            MessageEvent.EVENT_TYPE,
            UpdateEvent.EVENT_TYPE
        );
    }
}
