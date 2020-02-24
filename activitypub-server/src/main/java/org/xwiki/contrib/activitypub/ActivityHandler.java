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

import java.io.IOException;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;

/**
 * Handle an activity posted in an inbox or in an outbox, wrapped in an {@link ActivityRequest}.
 * @param <T> the type of the activity.
 * @version $Id$
 */
@Role
public interface ActivityHandler<T extends AbstractActivity>
{
    /**
     * Handle a post of the activity in the inbox.
     * @param activityRequest the request to handle.
     * @throws IOException in case of problem when handling the HTTP answer.
     * @throws ActivityPubException in case of error when handling the request.
     */
    void handleInboxRequest(ActivityRequest<T> activityRequest) throws IOException, ActivityPubException;

    /**
     * Handle a post of the activity in the outbox.
     * @param activityRequest the request to handle.
     * @throws IOException in case of problem when handling the HTTP answer.
     * @throws ActivityPubException in case of error when handling the request.
     */
    void handleOutboxRequest(ActivityRequest<T> activityRequest) throws IOException, ActivityPubException;
}
