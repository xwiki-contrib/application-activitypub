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
package org.xwiki.contrib.activitypub.internal.activities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubNotifier;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;

/**
 * Abstract handler for all {@link ActivityHandler}.
 *
 * @param <T> the type of activity to handle.
 * @version $Id$
 */
public abstract class AbstractActivityHandler<T extends AbstractActivity> implements ActivityHandler<T>
{
    @Inject
    protected ActivityPubJsonSerializer activityPubJsonSerializer;

    @Inject
    protected ActivityPubStorage activityPubStorage;

    @Inject
    protected ActivityPubNotifier notifier;

    @Inject
    protected ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    protected ActorHandler actorHandler;

    @Inject
    protected ActivityPubClient activityPubClient;

    @Inject
    protected ActivityPubConfiguration activityPubConfiguration;

    /**
     * Answer with an activity in the response body: generally used for 2xx answers.
     * @param response the servlet used to answer.
     * @param statusCode the code of the response.
     * @param activity the activity to serialize in the body of the response.
     * @throws ActivityPubException in case of problem during the serialization.
     * @throws IOException in case of problem in the HTTP answer.
     */
    protected void answer(HttpServletResponse response, int statusCode, AbstractActivity activity)
        throws ActivityPubException, IOException
    {
        if (response != null) {
            response.setStatus(statusCode);
            response.setContentType("application/activity+json");
            this.activityPubJsonSerializer.serialize(response.getOutputStream(), activity);
        }
    }

    /**
     * Answer with an error message.
     * @param response the servlet used to answer.
     * @param statusCode the code of the response.
     * @param error the error message to put in the response body (in text/plain).
     * @throws IOException in case of problem in the HTTP answer.
     */
    protected void answerError(HttpServletResponse response, int statusCode, String error) throws IOException
    {
        if (response != null) {
            response.setStatus(statusCode);
            response.setContentType("text/plain");
            response.getOutputStream().write(error.getBytes(StandardCharsets.UTF_8));
        }
    }
}
