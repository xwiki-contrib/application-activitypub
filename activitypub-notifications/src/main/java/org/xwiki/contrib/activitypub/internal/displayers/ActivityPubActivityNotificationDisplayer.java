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
package org.xwiki.contrib.activitypub.internal.displayers;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.entities.Activity;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;

@Component
@Singleton
public class ActivityPubActivityNotificationDisplayer
    implements org.xwiki.contrib.activitypub.internal.ActivityPubActivityNotificationDisplayer
{
    private static final String EVENT_BINDING_NAME = "event";
    private static final String ACTIVITY_BINDING_NAME = "activity";

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Override
    public Block displayActivityNotification(Event event, Activity activity)
        throws NotificationException
    {
        ScriptContext scriptContext = scriptContextManager.getScriptContext();
        try {
            scriptContext.setAttribute(EVENT_BINDING_NAME, event, ScriptContext.ENGINE_SCOPE);
            scriptContext.setAttribute(ACTIVITY_BINDING_NAME, activity, ScriptContext.ENGINE_SCOPE);

            String templateName = String.format("activity/%s.vm", activity.getType());
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
}
