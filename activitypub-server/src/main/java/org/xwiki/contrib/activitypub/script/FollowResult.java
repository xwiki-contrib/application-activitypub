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
package org.xwiki.contrib.activitypub.script;

import org.xwiki.contrib.activitypub.entities.AbstractActor;

/**
 * Result of a follow action ({@link ActivityPubScriptService#follow(AbstractActor)}).
 * It can either be a success, or by opposition, a failure.
 * In addition the translation key of a message to display to the user is also returned. 
 * 
 * @version $Id$
 * @since 1.0
 */
public class FollowResult
{
    private boolean success;

    private String message;

    /**
     * Initialize an object with a default message. By default the result is a failure.
     * @param message the default message.
     */
    public FollowResult(String message)
    {
        this.success = false;
        this.message = message;
    }

    /**
     * 
     * @return true if the follow operation is a success.
     */
    public boolean isSuccess()
    {
        return this.success;
    }

    /**
     * Set the success status of the follow operation.
     * @param success the success status.
     * @return the current object.
     */
    public FollowResult setSuccess(boolean success)
    {
        this.success = success;
        return this;
    }

    /**
     * 
     * @return the translation key of the follow message.
     */
    public String getMessage()
    {
        return this.message;
    }

    /**
     * 
     * @param message the translation key of the follow message.
     * @return the current object.
     */
    public FollowResult setMessage(String message)
    {
        this.message = message;
        return this;
    }
}
