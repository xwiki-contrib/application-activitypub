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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;

/**
 * Please document me.
 *
 * @version $Id$
 * @since X.Y.Z
 */
@Component
@Singleton
public class DefaultHTMLRenderer implements HTMLRenderer
{
    @Inject
    @Named("xhtml/1.0")
    private BlockRenderer renderer;

    @Override
    public String render(XDOM content) throws ActivityPubException
    {
        try {
            DefaultWikiPrinter printer = new DefaultWikiPrinter();
            this.renderer.render(content, printer);
            return printer.toString();
        } catch (Exception e) {
            throw new ActivityPubException("Error while rendering an HTML content", e);
        }
    }
}
