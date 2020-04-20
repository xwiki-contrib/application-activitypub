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
import java.util.HashMap;
import java.util.List;

import javax.script.SimpleScriptContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.contrib.activitypub.events.AnnounceEvent;
import org.xwiki.contrib.activitypub.events.CreateEvent;
import org.xwiki.contrib.activitypub.events.FollowEvent;
import org.xwiki.contrib.activitypub.events.MessageEvent;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.IdBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.activitypub.internal.ActivityPubRecordableEventConverter.ACTIVITY_PARAMETER_KEY;

/**
 * Test of {@link ActivityPubNotificationDisplayer}.
 *
 * @since 1.0
 * @version $Id$
 */
@ComponentTest
public class ActivityPubNotificationDisplayerTest
{
    @InjectMockComponents
    private ActivityPubNotificationDisplayer activityPubNotificationDisplayer;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @MockComponent
    private ActivityPubJsonParser activityPubJsonParser;

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private TemplateManager templateManager;

    @Test
    void renderNotificationNoValidEvent() throws Exception
    {
        DefaultEvent event = new DefaultEvent();
        CompositeEvent compositeEvent = new CompositeEvent(event);

        NotificationException expt = assertThrows(NotificationException.class,
            () -> this.activityPubNotificationDisplayer.renderNotification(compositeEvent));

        assertEquals("Error while getting the activity of the event [null at null by null on null]", expt.getMessage());
    }

    @Test
    void renderNotificationOneEvent() throws Exception
    {
        when(this.activityPubJsonParser.parse("my content")).thenReturn(new Accept());
        when(this.scriptContextManager.getScriptContext()).thenReturn(new SimpleScriptContext());
        Template t = mock(Template.class);
        when(this.templateManager.getTemplate("activitypub/follow.vm")).thenReturn(t);
        ArrayList<Block> childBlocks = new ArrayList<>();
        childBlocks.add(new IdBlock("ok"));
        when(this.templateManager.execute(t)).thenReturn(new XDOM(childBlocks));

        DefaultEvent event = new DefaultEvent();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(ACTIVITY_PARAMETER_KEY, "my content");
        event.setParameters(parameters);
        event.setType(FollowEvent.EVENT_TYPE);
        CompositeEvent compositeEvent = new CompositeEvent(event);
        Block actual = this.activityPubNotificationDisplayer.renderNotification(compositeEvent);
        assertEquals(1, actual.getChildren().size());
        assertEquals("ok", ((IdBlock) actual.getChildren().get(0)).getName());
        verify(this.templateManager, times(1)).getTemplate("activitypub/follow.vm");
    }

    @Test
    void renderNotificationRetrieveError() throws Exception
    {
        when(this.activityPubJsonParser.parse(anyString())).thenThrow(new ActivityPubException("throwed"));

        DefaultEvent event = new DefaultEvent();
        HashMap<String, String> parameters = new HashMap<>();
        parameters.put(ACTIVITY_PARAMETER_KEY, "my content");
        event.setParameters(parameters);
        CompositeEvent compositeEvent = new CompositeEvent(event);

        NotificationException expt = assertThrows(NotificationException.class,
            () -> this.activityPubNotificationDisplayer.renderNotification(compositeEvent));

        assertEquals("Error while getting the activity of the event [null at null by null on null]", expt.getMessage());
    }

    @Test
    void getSuppotedEventsDefault()
    {
        List<String> supportedEvents = this.activityPubNotificationDisplayer.getSupportedEvents();
        assertEquals(Arrays.asList(
            CreateEvent.EVENT_TYPE,
            FollowEvent.EVENT_TYPE,
            AnnounceEvent.EVENT_TYPE,
            MessageEvent.EVENT_TYPE),
                supportedEvents);
    }
}
