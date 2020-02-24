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
import java.net.URI;

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;

/**
 * A simple HttpClient dedicated to use ActivityPub.
 *
 * @version $Id$
 */
@Role
public interface ActivityPubClient
{
    /**
     * Post an activity in the actor inbox.
     * @param actor the actor who owns the inbox in which to post.
     * @param activity the activity to post.
     * @return an {@link HttpMethod} which contains the answer.
     * @throws ActivityPubException in case of error during the post or the activity serialization.
     */
    HttpMethod postInbox(AbstractActor actor, AbstractActivity activity) throws ActivityPubException;

    /**
     * Post an activity in the actor outbox.
     * @param actor the actor who owns the outbox in which to post.
     * @param activity the activity to post.
     * @return an {@link HttpMethod} which contains the answer.
     * @throws ActivityPubException in case of error during the post or the activity serialization.
     */
    HttpMethod postOutbox(AbstractActor actor, AbstractActivity activity) throws ActivityPubException;

    /**
     * Post an activity in the given URI.
     * @param uri the URI where to post the activity (should be an inbox or an outbox).
     * @param activity the activity to post.
     * @return an {@link HttpMethod} which contains the answer.
     * @throws ActivityPubException in case of error during the post or the activity serialization.
     */
    HttpMethod post(URI uri, AbstractActivity activity) throws ActivityPubException;

    /**
     * Performs an HTTP GET on the given URI.
     * @param uri the URI to retrieve.
     * @return an {@link HttpMethod} which contains the answer.
     * @throws IOException in case of error with the HTTP request.
     */
    HttpMethod get(URI uri) throws IOException;

    /**
     * Ensure that the {@link HttpMethod}:
     *   1. has been sent
     *   2. received a 200 OK
     *   3. receive a response with the right Content-Type headers for ActivityPub.
     * @param method the HttpMethod to check.
     * @throws ActivityPubException in case one of the check is not satisfied.
     */
    void checkAnswer(HttpMethod method) throws ActivityPubException;
}
