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
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StreamActivityPubObjectReferenceSerializer}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
public class StreamActivityPubObjectReferenceSerializerTest
{
    @InjectMockComponents
    private StreamActivityPubObjectReferenceSerializer streamActivityPubObjectReferenceSerializer;

    @Test
    public void getFunction()
    {
        Function<Note, ActivityPubObjectReference<Note>> function =
            this.streamActivityPubObjectReferenceSerializer.getFunction();

        Note note = mock(Note.class);
        ActivityPubObjectReference noteReference = mock(ActivityPubObjectReference.class);
        when(note.getReference()).thenReturn(noteReference);
        assertSame(noteReference, function.apply(note));
    }
}
