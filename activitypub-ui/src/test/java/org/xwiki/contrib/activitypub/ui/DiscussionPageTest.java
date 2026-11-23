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

import org.apache.velocity.tools.generic.EscapeTool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Page test of {@code ActivityPub.Discussion}.
 *
 * @version $Id$
 */
@ComponentList({ DiscussionTestMacro.class, RenderingScriptService.class })
@XWikiSyntax21ComponentList
@XHTML10ComponentList
class DiscussionPageTest extends PageTest
{
    private static final DocumentReference DISCUSSION_PAGE =
        new DocumentReference("xwiki", "ActivityPub", "Discussion");

    // Needed by the rendering script service.
    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.XHTML_1_0);
        registerVelocityTool("escapetool", new EscapeTool());
    }

    @Test
    void referenceStaysInTheMacroParameter() throws Exception
    {
        this.request.put("reference", "x\"/}}{{info}}injected{{/info}}");

        this.context.setDoc(loadPage(DISCUSSION_PAGE));
        Document result = Jsoup.parse(renderPage(DISCUSSION_PAGE));

        Element macro = result.selectFirst(".test-macro");
        assertNotNull(macro);
        assertEquals("x\"/}}{{info}}injected{{/info}}", macro.attr("data-reference"));
        assertEquals("APDiscussion", macro.attr("data-namespace"));
        assertFalse(result.text().contains("injected"));
        assertEquals(0, result.select(".xwikirenderingerror").size());
    }

    @Test
    void regularReference() throws Exception
    {
        this.request.put("reference", "xwiki:Discussions.Discussion.123-abc.WebHome");

        this.context.setDoc(loadPage(DISCUSSION_PAGE));
        Document result = Jsoup.parse(renderPage(DISCUSSION_PAGE));

        assertEquals("xwiki:Discussions.Discussion.123-abc.WebHome",
            result.selectFirst(".test-macro").attr("data-reference"));
    }
}
