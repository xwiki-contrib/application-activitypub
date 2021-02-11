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
package org.xwiki.contrib.activitypub.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.test.junit5.LogCaptureExtension;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.xwiki.test.LogLevel.DEBUG;

/**
 * Test of {@link ActivityPubObject} with a focus on parsing and serialization of their json representations.
 *
 * @since 1.0
 * @version $Id$
 */
class ActivityPubObjectTest extends AbstractEntityTest
{
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(DEBUG);

    @Test
    void parseWithExplicitType() throws Exception
    {
        String json = this.readResource("wrongtype.json");
        ActivityPubException e = assertThrows(ActivityPubException.class, () -> this.parser.parse(json, Note.class));
        assertEquals("Error while parsing request with type [class org.xwiki.contrib.activitypub.entities.Note].",
            e.getMessage());
    }

    @Test
    void parseWithImplicitType() throws Exception
    {
        String json = this.readResource("wrongtype.json");
        assertNull(this.parser.parse(json));
        assertEquals(1, this.logCapture.size());
        assertEquals(Level.DEBUG, this.logCapture.getLogEvent(0).getLevel());
        assertEquals("ActivityPub Object type [Wrong] not found.", this.logCapture.getMessage(0));
    }
}
