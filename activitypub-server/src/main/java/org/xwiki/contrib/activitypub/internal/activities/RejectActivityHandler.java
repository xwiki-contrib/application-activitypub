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
import java.util.Collections;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Reject;

/**
 * Specific handler for {@link Reject} activities.
 *
 * @version $Id$
 */
@Component
@Singleton
public class RejectActivityHandler extends AbstractActivityHandler<Reject>
{
    private static final String ONLY_FOLLOW_IMPLEMENTED =
        "Only follow activities can be accepted in the current implementation.";

    @Override
    public void handleInboxRequest(ActivityRequest<Reject> activityRequest)
        throws IOException, ActivityPubException
    {
        Reject reject = activityRequest.getActivity();
        if (reject.getId() == null) {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_BAD_REQUEST,
                "The ID of the activity must not be null.");
        } else {
            AbstractActor rejectingActor = this.activityPubObjectReferenceResolver.resolveReference(reject.getActor());
            ActivityPubObject object = this.activityPubObjectReferenceResolver.resolveReference(reject.getObject());

            if (object instanceof Follow) {
                Follow follow = (Follow) object;
                AbstractActor followingActor =
                    this.activityPubObjectReferenceResolver.resolveReference(follow.getActor());
                this.notifier
                    .notify(reject, Collections.singleton(followingActor));
                this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, reject);
            } else {
                this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                    ONLY_FOLLOW_IMPLEMENTED);
            }
        }
    }

    @Override
    public void handleOutboxRequest(ActivityRequest<Reject> activityRequest)
        throws IOException, ActivityPubException
    {
        Reject reject = activityRequest.getActivity();
        this.activityPubStorage.storeEntity(reject);
        AbstractActor rejectingActor = this.activityPubObjectReferenceResolver.resolveReference(reject.getActor());
        ActivityPubObject object = this.activityPubObjectReferenceResolver.resolveReference(reject.getObject());

        if (object instanceof Follow) {
            Follow follow = (Follow) object;
            AbstractActor followingActor = this.activityPubObjectReferenceResolver.resolveReference(follow.getActor());
            HttpMethod postMethod = this.activityPubClient.postInbox(followingActor, reject);

            try {
                this.activityPubClient.checkAnswer(postMethod);
            } catch (ActivityPubException e) {
                // FIXME: in that case is the final answer still a 200 OK?
                this.logger.error("Error while posting the accept to the following user.", e);
            } finally {
                postMethod.releaseConnection();
            }

            this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, reject);
        } else {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_NOT_IMPLEMENTED,
                ONLY_FOLLOW_IMPLEMENTED);
        }
    }
}
