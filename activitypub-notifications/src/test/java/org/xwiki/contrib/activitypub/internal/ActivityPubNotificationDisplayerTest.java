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
import java.util.HashMap;

import javax.inject.Inject;
import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.*;
import static org.xwiki.contrib.activitypub.internal.ActivityPubRecordableEventConverter.ACTIVITY_PARAMETER_KEY;

@ComponentTest
public class ActivityPubNotificationDisplayerTest
{
    @InjectMockComponents
    private ActivityPubNotificationDisplayer activityPubNotificationDisplayer;

    @RegisterExtension
    static LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @MockComponent
    private ActivityPubJsonParser activityPubJsonParser;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private TemplateManager templateManager;

    @Test
    public void testRenderNotificationNoValidEvent() throws Exception
    {
        final DefaultEvent event = new DefaultEvent();
        final CompositeEvent compositeEvent = new CompositeEvent(event);
        Block actual = activityPubNotificationDisplayer.renderNotification(compositeEvent);
        Assertions.assertTrue(actual.getChildren().isEmpty());
        Assert.assertEquals(String.format("The event [%s] cannot be processed.", event.toString()),
            logCapture.getMessage(0));
    }

    @Test
    public void testRenderNotificationOneEvent() throws Exception
    {

        when(activityPubJsonParser.parse("my content")).thenReturn(new Accept());
        when(scriptContextManager.getScriptContext()).thenReturn(new SimpleScriptContext());
        final Template t = new Template()
        {
            @Override public String getId()
            {
                return null;
            }

            @Override public String getPath()
            {
                return null;
            }

            @Override public TemplateContent getContent() throws Exception
            {
                return null;
            }
        };
        when(templateManager.getTemplate("activity/accept.vm")).thenReturn(t);
        final ArrayList<Block> childBlocks = new ArrayList<>();
        childBlocks.add(new IdBlock("ok"));
        when(templateManager.execute(t)).thenReturn(new XDOM(childBlocks));

        final DefaultEvent event = new DefaultEvent();
        final HashMap<String, String> parameters = new HashMap<>();
        parameters.put(ACTIVITY_PARAMETER_KEY, "my content");
        event.setParameters(parameters);
        final CompositeEvent compositeEvent = new CompositeEvent(event);
        Block actual = activityPubNotificationDisplayer.renderNotification(compositeEvent);
        Assertions.assertEquals(1, actual.getChildren().size());
    }

    @Test
    public void testRenderNotificationRetrieveError() throws Exception
    {

        when(activityPubJsonParser.parse(anyString())).thenThrow(new ActivityPubException("throwed"));

        final DefaultEvent event = new DefaultEvent();
        final HashMap<String, String> parameters = new HashMap<>();
        parameters.put(ACTIVITY_PARAMETER_KEY, "my content");
        event.setParameters(parameters);
        CompositeEvent compositeEvent = new CompositeEvent(event);
        Assertions.assertThrows(NotificationException.class,
            () -> activityPubNotificationDisplayer.renderNotification(compositeEvent));
    }
}
