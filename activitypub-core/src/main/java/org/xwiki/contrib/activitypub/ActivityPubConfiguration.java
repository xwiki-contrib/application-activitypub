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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Defines the configuration of the server.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
@Role
public interface ActivityPubConfiguration
{
    /**
     * Behaviour to adopt in case of Follow request.
     */
    enum FollowPolicy
    {
        /**
         * Always Accept follow request.
         */
        ACCEPT,

        /**
         * Always Reject follow request.
         */
        REJECT,

        /**
         * Always Ask users in case of follow request.
         */
        ASK
    }

    /**
     * @return which follow policy is used by the server.
     */
    FollowPolicy getFollowPolicy();

    /**
     * @return the reference to the group who handles a followed wiki.
     */
    DocumentReference getWikiGroup();

    /**
     * @return the value of the page notification configuration value.
     * @since 1.2
     */
    boolean isPagesNotification();

    /**
     * @return the value of the user page notificatin value.
     * @since 1.2
     */
    boolean isUserPagesNotification();
}
