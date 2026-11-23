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
import java.net.URI;

import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.script.ActivityPubScriptService;
import org.xwiki.contrib.activitypub.webfinger.script.WebfingerScriptService;
import org.xwiki.localization.script.LocalizationScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
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
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Page test of the likers menu entry of {@code ActivityPub.UserLikes}.
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
    AttributeFilter.class
})
@XWikiSyntax21ComponentList
@XHTML10ComponentList
class UserLikesPageTest extends PageTest
{
    private static final DocumentReference USER_LIKES_PAGE =
        new DocumentReference("xwiki", "ActivityPub", "UserLikes");

    private static final DocumentReference UIX_CLASS = new DocumentReference("xwiki", "XWiki", "UIExtensionClass");

    private static final String UIX_NAME = "org.xwiki.contrib.activitypub.ui.menu-viewers-like";

    private static final String ACTOR_LABEL = "a & < > ~ ]] >> \"}}{{info}}x{{/info}} **b** <b id=\"bold\">b</b>";

    private static final String PAGE_NAME = "a\"><i id=\"bold\">x</i>";

    // Needed by the rendering script service.
    @MockComponent
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    private ActivityPubScriptService activityPubScriptService;

    private WebfingerScriptService webfingerScriptService;

    @BeforeEach
    void setUp() throws Exception
    {
        setOutputSyntax(Syntax.XHTML_1_0);
        registerVelocityTool("escapetool", new EscapeTool());
        ModelScriptService modelScriptService = mock(ModelScriptService.class);
        when(modelScriptService.resolveDocument("ActivityPub.LikersView"))
            .thenReturn(new DocumentReference("xwiki", "ActivityPub", "LikersView"));
        when(modelScriptService.serialize(any(), eq("default"))).thenReturn("Space." + PAGE_NAME);
        this.componentManager.registerComponent(ScriptService.class, "model", modelScriptService);
        this.activityPubScriptService = mock(ActivityPubScriptService.class);
        this.webfingerScriptService = mock(WebfingerScriptService.class);
        this.componentManager.registerComponent(ScriptService.class, "activitypub", this.activityPubScriptService);
        this.componentManager.registerComponent(ScriptService.class, "webfinger", this.webfingerScriptService);
        LocalizationScriptService localizationScriptService = mock(LocalizationScriptService.class);
        when(localizationScriptService.render(anyString())).thenReturn("Likes <b id=\"label\">of</b> the fediverse");
        this.componentManager.registerComponent(ScriptService.class, "localization", localizationScriptService);
    }

    @Test
    void likersMenuEntryEscapesTheReferenceOfThePage() throws Exception
    {
        XWikiDocument userLikes = loadPage(USER_LIKES_PAGE);
        String uixContent = null;
        for (BaseObject object : userLikes.getXObjects(UIX_CLASS)) {
            if (object != null && UIX_NAME.equals(object.getStringValue("name"))) {
                uixContent = object.getLargeStringValue("content");
            }
        }
        assertNotNull(uixContent);

        // The skin template providing the macro is not available in the page tests: replace it with a minimal one.
        XWikiDocument page = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", PAGE_NAME), this.context);
        page.setSyntax(Syntax.XWIKI_2_1);
        page.setContent("{{velocity output=\"false\"}}\n"
            + "#macro (submenuitem $href $label $id $class $icon)\n"
            + "<a id=\"$id\" href=\"$href\">$label</a>\n"
            + "#end\n"
            + "{{/velocity}}\n\n"
            + uixContent.replace("#template('menus_macros.vm')", ""));
        this.xwiki.saveDocument(page, this.context);
        this.context.setDoc(page);

        Document result = Jsoup.parse(page.getRenderedContent(Syntax.XHTML_1_0, this.context));

        Element link = result.selectFirst("a#activitypub-likers");
        assertNotNull(link, result.html());
        assertEquals("/xwiki/bin/view/ActivityPub/LikersView?reference=Space.a%22%3E%3Ci+id%3D%22bold%22%3Ex%3C%2Fi%3E",
            link.attr("href"));
        assertEquals("Likes <b id=\"label\">of</b> the fediverse", link.text());
        assertEquals(0, result.select("#bold").size());
        assertEquals(0, result.select("#label").size());
        assertEquals(0, result.select(".xwikirenderingerror").size());
    }

    @Test
    void remoteActorIsDisplayedAsLink() throws Exception
    {
        Document result = renderActorLink(false);

        Element link = result.selectFirst(".activitypub-actor-link span.wikiexternallink > a");
        assertNotNull(link, result.html());
        assertEquals("http://remote.org/actors/a", link.attr("href"));
        assertEquals(ACTOR_LABEL, link.text());
        assertEquals(1, result.select("a").size());
        assertEquals(0, result.select("#bold").size());
        assertEquals(0, result.select(".xwikirenderingerror").size());
    }

    @Test
    void localActorIsDisplayedAsLink() throws Exception
    {
        Document result = renderActorLink(true);

        Element link = result.selectFirst(".activitypub-actor-link > a");
        assertNotNull(link, result.html());
        assertEquals("http://remote.org/actors/a", link.attr("href"));
        assertEquals(ACTOR_LABEL, link.text());
        assertEquals(1, result.select("a").size());
        assertEquals(0, result.select("#bold").size());
        assertEquals(0, result.select(".xwikirenderingerror").size());
    }

    private Document renderActorLink(boolean local) throws Exception
    {
        when(this.activityPubScriptService.belongsToCurrentInstance(anyString())).thenReturn(local);
        AbstractActor actor = new Person().setId(new URI("http://remote.org/actors/a"));
        when(this.webfingerScriptService.getWebfingerId(actor)).thenReturn(ACTOR_LABEL);
        registerVelocityTool("actor", actor);
        XWikiDocument page = this.xwiki.getDocument(new DocumentReference("xwiki", "Space", "Actor"), this.context);
        page.setSyntax(Syntax.XWIKI_2_1);
        page.setContent("{{include reference=\"ActivityPub.UserLikes\"/}}\n\n{{velocity}}\n"
            + "{{html clean=\"false\"}}#activityPubActorLink($actor){{/html}}\n{{/velocity}}");
        this.xwiki.saveDocument(page, this.context);

        loadPage(USER_LIKES_PAGE);
        this.context.setDoc(page);
        return Jsoup.parse(page.getRenderedContent(Syntax.XHTML_1_0, this.context));
    }
}
