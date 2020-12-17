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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Singleton;

import org.apache.commons.lang3.time.DateUtils;
import org.xwiki.component.annotation.Component;

/**
 * Provides common operation to manipulate dates on ActivityPub.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = { DateProvider.class })
@Singleton
public class DateProvider
{
    /**
     * @return the current time
     */
    public Date currentTime()
    {
        return new Date();
    }

    /**
     * Checks if the current date is after a period of time after the past date.
     *
     * @param currentDate the current date
     * @param pastDate the past date
     * @param duration the duration after the past date
     * @return {@code true} if the current date is after the duration from the past date, {@code false} otherwise
     */
    public boolean isElapsed(Date currentDate, Date pastDate, int duration)
    {
        return currentDate.after(DateUtils.addDays(pastDate, duration));
    }

    /**
     * @return a formatted current date, following the pattern {@code EEE, dd MMM yyyy HH:mm:ss z}
     */
    public String getFormattedDate()
    {
        return getFormattedDate(currentTime());
    }

    /**
     * @param date the date parameter
     * @return a formatted date from the date parameter, following the pattern {@code EEE, dd MMM yyyy HH:mm:ss z}
     */
    public String getFormattedDate(Date date)
    {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }
}
