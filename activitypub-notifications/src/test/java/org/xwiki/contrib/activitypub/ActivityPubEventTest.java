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
package org.xwiki.contrib.activitypub;

import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Follow;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link ActivityPubEvent}.
 *
 * @since 1.0
 * @version $Id$
 */
public class ActivityPubEventTest
{
    private ActivityPubEvent<Create> activityPubEvent;

    @BeforeEach
    void beforeEach()
    {
        this.activityPubEvent = new ActivityPubEvent<>(new Create(), new HashSet<>());
    }

    @Test
    void matchNull()
    {
        assertFalse(this.activityPubEvent.matches(null));
    }

    @Test
    void matchObject()
    {
        assertFalse(this.activityPubEvent.matches(new Object()));
    }

    @Test
    void matchActivityPubEvent()
    {
        assertTrue(this.activityPubEvent.matches(new ActivityPubEvent<>(new Follow(), new HashSet<>())));
    }
}
