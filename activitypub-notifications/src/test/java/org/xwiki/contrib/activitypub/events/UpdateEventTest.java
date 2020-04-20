package org.xwiki.contrib.activitypub.events;/*
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link UpdateEvent}.
 *
 * @version $Id$
 * @since 1.2
 */
class UpdateEventTest
{
    private final UpdateEvent event = new UpdateEvent(null, null);

    @Test
    void getType()
    {
        assertEquals("activitypub.update", this.event.getType());
    }

    @Test
    void matchesTrue()
    {
        assertTrue(this.event.matches(new UpdateEvent(null, null)));
    }

    @Test
    void matchesFalse()
    {
        assertFalse(this.event.matches(new CreateEvent(null, null)));
    }
}