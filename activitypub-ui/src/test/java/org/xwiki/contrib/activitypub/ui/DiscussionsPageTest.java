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
package org.xwiki.contrib.activitypub.ui;

import java.net.URI;

import org.apache.velocity.tools.generic.EscapeTool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.script.ActivityPubScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code ActivityPub.Discussions}.
 *
 * @version $Id$
 */
@ComponentList({ ListDiscussionsTestMacro.class, RenderingScriptService.class })
@XWikiSyntax21ComponentList
@XHTML10ComponentList
class DiscussionsPageTest extends PageTest
{
    private static final DocumentReference DISCUSSIONS_PAGE =
        new DocumentReference("xwiki", "ActivityPub", "Discussions");

    // Needed by the rendering script service.
    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.XHTML_1_0);
        registerVelocityTool("escapetool", new EscapeTool());
        AbstractActor actor = new Person().setId(new URI("http://xwiki.org/actors/a"));
        ActivityPubScriptService activityPubScriptService = mock(ActivityPubScriptService.class);
        when(activityPubScriptService.getActor(anyString())).thenReturn(actor);
        when(activityPubScriptService.currentUserCanActFor(any())).thenReturn(true);
        this.componentManager.registerComponent(ScriptService.class, "activitypub", activityPubScriptService);
    }

    @Test
    void actorStaysInTheMacroParameters() throws Exception
    {
        this.request.put("actor", "a\"}}{{info}}injected{{/info}}");

        this.context.setDoc(loadPage(DISCUSSIONS_PAGE));
        Document result = Jsoup.parse(renderPage(DISCUSSIONS_PAGE));

        Element macro = result.selectFirst(".test-macro");
        assertNotNull(macro);
        assertEquals("http://xwiki.org/actors/a", macro.attr("data-reference"));
        assertEquals("discussionsAPDashboard", macro.attr("data-list-id"));
        assertEquals(
            "/xwiki/bin/view/ActivityPub/Discussion?reference=__REFERENCE__"
                + "&actor=a%22%7D%7D%7B%7Binfo%7D%7Dinjected%7B%7B%2Finfo%7D%7D",
            macro.attr("data-link-template"));
        assertEquals(0, result.select(".xwikirenderingerror").size());
    }
}
