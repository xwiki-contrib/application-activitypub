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
package org.xwiki.contrib.activitypub.webfinger;

/**
 * Default exception for webfinger.
 *
 * @since 1.1
 * @version $Id$
 */
public class WebfingerException extends Exception
{
    private final int errorCode;

    /**
     * Handling of a business error.
     * @param errorCode http error code to be returned to the client.
     */
    public WebfingerException(int errorCode)
    {
        this.errorCode = errorCode;
    }

    /**
     * Handling of a technical error.
     * @param message The detailed message of the exception.
     * @param cause The cause of the error
     */
    public WebfingerException(String message, Throwable cause)
    {
        super(message, cause);
        this.errorCode = 500;
    }

    /**
     * Handling of a technical error.
     * @param cause The cause of the error
     */
    public WebfingerException(Throwable cause)
    {
        super(cause);
        this.errorCode = 500;
    }

    /**
     *
     * @return the error code.
     */
    public int getErrorCode()
    {
        return this.errorCode;
    }
}
