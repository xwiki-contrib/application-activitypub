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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.ExternalServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Default implementation of {@link HTMLRenderer}. Produce an html representation of the provided {@link XDOM} while
 * taking care to make it seens from an outside site. For instance links are transformed to fully defined URLs.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Singleton
public class DefaultHTMLRenderer implements HTMLRenderer
{
    @Inject
    @Named("xhtml/1.0")
    private BlockRenderer renderer;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;
    
    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    @Override
    public String render(XDOM content, DocumentReference documentReference) throws ActivityPubException
    {
        XWikiContext xWikiContext = this.xwikiContextProvider.get();
        XWikiURLFactory currentURLFactory = xWikiContext.getURLFactory();
        XWikiDocument currentXWikiDocument = xWikiContext.getDoc();
        try {
            xWikiContext.setURLFactory(new ExternalServletURLFactory(xWikiContext));
            
            
            xWikiContext.setDoc(new XWikiDocument(documentReference));
            TransformationContext macroTransformationContext = new TransformationContext(content, null);

            try {
                this.macroTransformation.transform(content, macroTransformationContext);
                DefaultWikiPrinter printer = new DefaultWikiPrinter();
                this.renderer.render(content, printer);
                return printer.toString();
            } catch (Exception e) {
                throw new ActivityPubException("Error while rendering an HTML content", e);
            }
        } finally {
            xWikiContext.setURLFactory(currentURLFactory);
            xWikiContext.setDoc(currentXWikiDocument);
        }
    }
}
