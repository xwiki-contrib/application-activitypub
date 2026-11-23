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
import org.xwiki.contrib.activitypub.webfinger.script.WebfingerScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.internal.macro.html.HTMLMacroXHTMLRenderer;
import org.xwiki.rendering.internal.macro.html.HTMLMacroXHTMLRendererFactory;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.script.RenderingScriptService;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XHTML10ComponentList;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.xml.internal.html.DefaultHTMLCleaner;
import org.xwiki.xml.internal.html.DefaultHTMLCleanerConfiguration;
import org.xwiki.xml.internal.html.filter.AttributeFilter;
import org.xwiki.xml.internal.html.filter.BodyFilter;
import org.xwiki.xml.internal.html.filter.ControlCharactersFilter;
import org.xwiki.xml.internal.html.filter.FontFilter;
import org.xwiki.xml.internal.html.filter.LinkFilter;
import org.xwiki.xml.internal.html.filter.ListFilter;
import org.xwiki.xml.internal.html.filter.ListItemFilter;
import org.xwiki.xml.internal.html.filter.UniqueIdFilter;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test of {@code ActivityPub.LikersView} and of the actor display macro of {@code ActivityPub.ListMessages}.
 *
 * @version $Id$
 */
@ComponentList({
    RenderingScriptService.class,
    DefaultHTMLCleaner.class,
    DefaultHTMLCleanerConfiguration.class,
    BodyFilter.class,
    ListItemFilter.class,
    ListFilter.class,
    UniqueIdFilter.class,
    LinkFilter.class,
    ControlCharactersFilter.class,
    FontFilter.class,
    AttributeFilter.class,
    HTMLMacroXHTMLRenderer.class,
    HTMLMacroXHTMLRendererFactory.class
})
@XWikiSyntax21ComponentList
@XHTML10ComponentList
class LikersViewPageTest extends PageTest
{
    private static final DocumentReference LIKERS_PAGE = new DocumentReference("xwiki", "ActivityPub", "LikersView");

    private static final DocumentReference LIST_MESSAGES_PAGE =
        new DocumentReference("xwiki", "ActivityPub", "ListMessages");

    private static final String PAYLOAD = "a & < > ~ ]] >> \"}}{{info}}x{{/info}} **b** <b id=\"bold\">b</b>";

    // Needed by the rendering script service.
    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    private static final String IN_HTML =
        "{{html clean=\"false\" wiki=\"true\"}}\n#displayActorInHtml($actor)\n{{/html}}";

    private ActivityPubScriptService activityPubScriptService;

    private WebfingerScriptService webfingerScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.XHTML_1_0);
        registerVelocityTool("escapetool", new EscapeTool());
        this.activityPubScriptService = mock(ActivityPubScriptService.class);
        when(this.activityPubScriptService.escapeXWikiSyntax(anyString())).thenCallRealMethod();
        this.webfingerScriptService = mock(WebfingerScriptService.class);
        // The localization of the page tests does not support the parameters.
        LocalizationScriptService localizationScriptService = mock(LocalizationScriptService.class);
        when(localizationScriptService.render(anyString(), anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0) + " " + invocation.getArgument(1));
        this.componentManager.registerComponent(ScriptService.class, "localization", localizationScriptService);
        this.componentManager.registerComponent(ScriptService.class, "activitypub", this.activityPubScriptService);
        this.componentManager.registerComponent(ScriptService.class, "webfinger", this.webfingerScriptService);
    }

    @Test
    void documentTitleIsDisplayedAsText() throws Exception
    {
        DocumentReference likedPage = new DocumentReference("xwiki", "Space", "Page");
        XWikiDocument document = this.xwiki.getDocument(likedPage, this.context);
        document.setTitle(PAYLOAD);
        this.xwiki.saveDocument(document, this.context);
        this.request.put("reference", "xwiki:Space.Page");

        loadPage(LIST_MESSAGES_PAGE);
        this.context.setDoc(loadPage(LIKERS_PAGE));
        Document result = Jsoup.parse(renderPage(LIKERS_PAGE));

        assertEquals("activitypub.like.likers.nolikes [" + PAYLOAD + "]", result.text());
        assertEquals(0, result.select(".xwikirenderingerror").size());
        assertEquals(0, result.select("#bold").size());
    }

    @Test
    void remoteActorInWikiContext() throws Exception
    {
        Document result = renderActor(false, "#displayActor($actor)");

        assertRemoteLink(result);
    }

    @Test
    void remoteActorInHtmlContext() throws Exception
    {
        Document result = renderActor(false, IN_HTML);

        assertRemoteLink(result);
    }

    @Test
    void localActorInWikiContext() throws Exception
    {
        Document result = renderActor(true, "#displayActor($actor)");

        assertLocalLink(result);
    }

    @Test
    void localActorInHtmlContext() throws Exception
    {
        Document result = renderActor(true, IN_HTML);

        assertLocalLink(result);
    }

    private void assertRemoteLink(Document result)
    {
        Element link = result.selectFirst("span.wikiexternallink > a");
        assertNotNull(link, result.html());
        assertEquals("http://remote.org/actors/a", link.attr("href"));
        assertEquals(PAYLOAD, link.text());
        assertEquals(1, result.select("a").size());
        assertEquals(0, result.select(".xwikirenderingerror").size());
        assertEquals(0, result.select("#bold").size());
    }

    private void assertLocalLink(Document result)
    {
        Element link = result.selectFirst("a");
        assertNotNull(link, result.html());
        assertEquals("http://remote.org/actors/a", link.attr("href"));
        assertEquals(PAYLOAD, link.text());
        assertEquals(1, result.select("a").size());
        assertEquals(0, result.select(".wikiexternallink").size());
        assertEquals(0, result.select(".xwikirenderingerror").size());
        assertEquals(0, result.select("#bold").size());
    }

    private Document renderActor(boolean local, String content) throws Exception
    {
        when(this.activityPubScriptService.belongsToCurrentInstance(anyString())).thenReturn(local);
        AbstractActor actor = new Person().setId(new URI("http://remote.org/actors/a"));
        when(this.webfingerScriptService.getWebfingerId(actor)).thenReturn(PAYLOAD);
        registerVelocityTool("actor", actor);
        DocumentReference reference = new DocumentReference("xwiki", "Space", "Actor");
        XWikiDocument document = this.xwiki.getDocument(reference, this.context);
        document.setSyntax(Syntax.XWIKI_2_1);
        document.setContent("{{include reference=\"ActivityPub.ListMessages\"/}}\n\n{{velocity}}\n" + content
            + "\n{{/velocity}}");
        this.xwiki.saveDocument(document, this.context);

        loadPage(LIST_MESSAGES_PAGE);
        this.context.setDoc(document);
        return Jsoup.parse(document.getRenderedContent(Syntax.XHTML_1_0, this.context));
    }
}
