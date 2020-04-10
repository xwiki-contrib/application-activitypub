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
import javax.script.ScriptContext;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.events.AnnounceEvent;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.FollowEvent;
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
    private static final String EVENT_BINDING_NAME = "event";
    private static final String ACTIVITY_BINDING_NAME = "activity";

    @Inject
    private Logger logger;

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
        result.addChildren(this.processEvents(compositeEvent.getEvents()));
        return result;
    }

    private List<Block> processEvents(List<Event> events) throws NotificationException
    {
        List<Block> result = new ArrayList<>();
        for (Event event : events) {
            if (event.getParameters().containsKey(ACTIVITY_PARAMETER_KEY)) {
                AbstractActivity activity = null;
                try {
                    activity = this.getActivity(event);
                } catch (ActivityPubException e) {
                    throw new NotificationException("Error while getting the activity of an event", e);
                }
                result.add(this.displayActivityNotification(event, activity));
            } else {
                this.logger.error("The event [{}] cannot be processed.", event);
            }
        }
        return result;
    }

    private AbstractActivity getActivity(Event event) throws ActivityPubException
    {
        String activity = event.getParameters().get(ACTIVITY_PARAMETER_KEY);
        return this.activityPubJsonParser.parse(activity);
    }

    private Block displayActivityNotification(Event event, AbstractActivity activity)
        throws NotificationException
    {
        ScriptContext scriptContext = scriptContextManager.getScriptContext();
        try {
            scriptContext.setAttribute(EVENT_BINDING_NAME, event, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute(ACTIVITY_BINDING_NAME, activity, ScriptContext.ENGINE_SCOPE);

            String templateName = String.format("activity/%s.vm", activity.getType().toLowerCase());
            Template template = templateManager.getTemplate(templateName);

            return (template != null) ? templateManager.execute(template)
                : templateManager.execute("activity/activity.vm");

        } catch (Exception e) {
            throw new NotificationException("Failed to render the notification.", e);
        } finally {
            scriptContext.removeAttribute(EVENT_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
            scriptContext.removeAttribute(ACTIVITY_BINDING_NAME, ScriptContext.ENGINE_SCOPE);
        }
    }

    @Override
    public List<String> getSupportedEvents()
    {
        return Arrays.asList(CreateEvent.EVENT_TYPE, FollowEvent.EVENT_TYPE, AnnounceEvent.EVENT_TYPE);
    }
}
