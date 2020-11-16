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

import java.io.StringReader;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Provides the operation to access the {@link XDOM} of document bodies and large string properties.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { ActivityPubXDOMService.class })
@Singleton
public class ActivityPubXDOMService
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    @Inject
    private HTMLRenderer htmlRenderer;

    /**
     * Retrieved the {@link XDOM} from the referenced entity.
     *
     * @param entityReference the entity reference of the element from which the {@link XDOM} must be retrieved
     * @param doc the document containing the referenced entity
     * @param location the location of the element from which the {@link XDOM} must be retrieved
     * @return the {@link XDOM} of the referenced entity.
     */
    public Optional<XDOM> getXDOM(EntityReference entityReference, XWikiDocument doc, MentionLocation location)
    {
        Optional<XDOM> xdom = Optional.empty();
        if (location == MentionLocation.DOCUMENT) {
            xdom = Optional.of(doc.getXDOM());
        } else if (location == MentionLocation.COMMENT || location == MentionLocation.ANNOTATION
            || location == MentionLocation.AWM_FIELD)
        {
            if (entityReference instanceof ObjectPropertyReference) {
                BaseObject xObject = doc.getXObject(entityReference.extractReference(EntityType.OBJECT));
                try {
                    PropertyInterface propertyInterface = xObject.get(entityReference.getName());
                    if (propertyInterface instanceof LargeStringProperty) {
                        LargeStringProperty anInterface = (LargeStringProperty) propertyInterface;
                        String value = anInterface.getValue();
                        Parser instance =
                            this.componentManagerProvider.get().getInstance(Parser.class, doc.getSyntax().toIdString());
                        xdom = Optional.of(instance.parse(new StringReader(value)));
                    }
                } catch (XWikiException | ComponentLookupException | ParseException e) {
                    this.logger.warn("An error occurred during the parsing of [{}]. Cause: [{}]", entityReference,
                        getRootCauseMessage(e));
                }
            } else {
                this.logger.debug("[{}] is not an object property reference.", entityReference);
            }
        }
        return xdom;
    }

    /**
     * Renders the {@link XDOM}.
     *
     * @param xdom the xdom
     * @param documentReference the document reference in which the rendered {@link XDOM} is located
     * @return the rendered {@link XDOM}
     * @throws ActivityPubException in case of error during the rendering
     */
    public String render(XDOM xdom, DocumentReference documentReference) throws ActivityPubException
    {
        return this.htmlRenderer.render(xdom, documentReference);
    }
}
