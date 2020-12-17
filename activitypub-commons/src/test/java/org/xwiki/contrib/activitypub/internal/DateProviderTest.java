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

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static java.util.Calendar.HOUR;
import static java.util.Calendar.JANUARY;
import static java.util.Calendar.MARCH;
import static java.util.Calendar.getInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of {@link DateProvider}.
 *
 * @version $Id$
 * @since 1.5
 */
@ComponentTest
class DateProviderTest
{
    @InjectMockComponents
    private DateProvider dateProvider;

    @Test
    void isElapsedNowAnd1900()
    {
        Calendar calendar = getInstance();
        calendar.set(1900, JANUARY, 0);
        boolean elapsed = this.dateProvider.isElapsed(new Date(), calendar.getTime(), 1);
        assertTrue(elapsed);
    }

    @Test
    void isElapsed1900AndNow()
    {
        Calendar calendar = getInstance();
        calendar.set(1900, JANUARY, 0);
        boolean elapsed = this.dateProvider.isElapsed(calendar.getTime(), new Date(), 1);
        assertFalse(elapsed);
    }

    @Test
    void isElapsedNowAndNow()
    {

        Calendar calendar = getInstance();
        Date now = calendar.getTime();
        calendar.add(HOUR, 23);
        boolean elapsed = this.dateProvider.isElapsed(now, calendar.getTime(), 1);
        assertFalse(elapsed);
    }

    @Test
    void formattedDate()
    {
        Calendar calendar = getInstance();
        calendar.set(1910, MARCH, 3, 4, 5, 6);
        String formattedDate = this.dateProvider.getFormattedDate(calendar.getTime());
        assertEquals("Thu, 03 Mar 1910 03:55:45 GMT", formattedDate);
    }
}