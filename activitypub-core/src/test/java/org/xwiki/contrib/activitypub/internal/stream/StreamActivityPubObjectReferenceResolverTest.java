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
package org.xwiki.contrib.activitypub.internal.stream;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StreamActivityPubObjectReferenceResolver}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
public class StreamActivityPubObjectReferenceResolverTest
{
    @InjectMockComponents
    private StreamActivityPubObjectReferenceResolver streamActivityPubObjectReferenceResolver;

    @MockComponent
    private ActivityPubObjectReferenceResolver resolver;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    public void getFunction() throws ActivityPubException
    {
        Function<ActivityPubObjectReference<Note>, Note> function =
            this.streamActivityPubObjectReferenceResolver.getFunction();

        ActivityPubObjectReference<Note> noteReference = mock(ActivityPubObjectReference.class);
        Note note = mock(Note.class);
        when(this.resolver.resolveReference(noteReference)).thenReturn(note);
        assertSame(note, function.apply(noteReference));

        when(noteReference.toString()).thenReturn("mynoteref");
        when(this.resolver.resolveReference(noteReference)).thenThrow(new ActivityPubException("error"));
        assertNull(function.apply(noteReference));
        assertEquals("Error while resolving reference [mynoteref]", logCapture.getMessage(0));
    }
}
