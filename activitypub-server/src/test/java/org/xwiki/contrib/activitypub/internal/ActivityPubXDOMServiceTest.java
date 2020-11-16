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

import java.util.Optional;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;

import ch.qos.logback.classic.Level;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.xwiki.mentions.MentionLocation.ANNOTATION;
import static org.xwiki.mentions.MentionLocation.AWM_FIELD;
import static org.xwiki.mentions.MentionLocation.DOCUMENT;
import static org.xwiki.rendering.syntax.Syntax.CONFLUENCE_1_0;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Tests of {@link ActivityPubXDOMService}.
 *
 * @version $Id$
 * @since 1.4
 */
@ComponentTest
class ActivityPubXDOMServiceTest
{
    public static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    public static final String COMMENT_FIELD_NAME = "comment";

    @InjectMockComponents
    private ActivityPubXDOMService activityPubXDOMService;

    @MockComponent
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    @Test
    void getXDOMInDocument()
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        XDOM xdom = new XDOM(emptyList());
        when(xWikiDocument.getXDOM()).thenReturn(xdom);
        Optional<XDOM> actual =
            this.activityPubXDOMService.getXDOM(DOCUMENT_REFERENCE, xWikiDocument, DOCUMENT);
        assertEquals(Optional.of(xdom), actual);
    }

    @ParameterizedTest
    @EnumSource(value = MentionLocation.class, names = { "ANNOTATION", "AWM_FIELD", "COMMENT" })
    void getXDOMInComment(MentionLocation mentionLocation) throws Exception
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        BaseObject baseObject = mock(BaseObject.class);
        ObjectReference objectReference = new ObjectReference("XWiki.XWikiComments", DOCUMENT_REFERENCE);
        ObjectPropertyReference objectPropertyReference =
            new ObjectPropertyReference(COMMENT_FIELD_NAME, objectReference);
        XDOM xdom = new XDOM(emptyList());
        LargeStringProperty largeStringProperty = new LargeStringProperty();
        largeStringProperty.setValue("CONTENT");
        ComponentManager componentManager = mock(ComponentManager.class);
        Parser parser = mock(Parser.class);

        when(xWikiDocument.getSyntax()).thenReturn(CONFLUENCE_1_0);
        when(xWikiDocument.getXObject(objectPropertyReference.extractReference(EntityType.OBJECT)))
            .thenReturn(baseObject);
        when(baseObject.get(COMMENT_FIELD_NAME)).thenReturn(largeStringProperty);
        when(this.componentManagerProvider.get()).thenReturn(componentManager);
        when(componentManager.getInstance(Parser.class, CONFLUENCE_1_0.toIdString())).thenReturn(parser);
        when(parser.parse(any())).thenReturn(xdom);

        Optional<XDOM> actual =
            this.activityPubXDOMService.getXDOM(objectPropertyReference, xWikiDocument, mentionLocation);
        assertEquals(Optional.of(xdom), actual);
    }

    @Test
    void getXDOMWrongEntityType()
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        ObjectReference objectReference = new ObjectReference("XWiki.XWikiComments", DOCUMENT_REFERENCE);
        this.activityPubXDOMService.getXDOM(objectReference, xWikiDocument, AWM_FIELD);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("[Object xwiki:XWiki.Doc^XWiki.XWikiComments] is not an object property reference.",
            this.logCapture.getMessage(0));
    }

    @Test
    void getXDOMErrorOccurs() throws Exception
    {
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        BaseObject baseObject = mock(BaseObject.class);
        ObjectReference objectReference = new ObjectReference("XWiki.XWikiComments", DOCUMENT_REFERENCE);
        ObjectPropertyReference objectPropertyReference =
            new ObjectPropertyReference(COMMENT_FIELD_NAME, objectReference);

        when(xWikiDocument.getSyntax()).thenReturn(CONFLUENCE_1_0);
        when(xWikiDocument.getXObject(objectPropertyReference.extractReference(EntityType.OBJECT)))
            .thenReturn(baseObject);
        when(baseObject.get(COMMENT_FIELD_NAME)).thenThrow(XWikiException.class);

        this.activityPubXDOMService.getXDOM(objectPropertyReference, xWikiDocument, ANNOTATION);
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
        assertEquals(
            "An error occurred during the parsing of [Object_property xwiki:XWiki.Doc^XWiki.XWikiComments.comment]. "
                + "Cause: [XWikiException: Error number 0 in 0]",
            this.logCapture.getMessage(0));
    }
}