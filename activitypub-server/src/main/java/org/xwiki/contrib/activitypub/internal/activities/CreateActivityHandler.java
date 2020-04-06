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
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;

/**
 * Specific handler for {@link Create} activities.
 *
 * @version $Id$
 */
@Component
@Singleton
public class CreateActivityHandler extends AbstractActivityHandler<Create>
{
    @Override
    public void handleInboxRequest(ActivityRequest<Create> activityRequest) throws IOException, ActivityPubException
    {
        Create create = activityRequest.getActivity();
        create.getObject().setExpand(true);
        if (create.getId() == null) {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_BAD_REQUEST,
                "The ID of the activity must not be null.");
        } else {
            AbstractActor actor = activityRequest.getActor();
            Inbox inbox = this.getInbox(actor);
            inbox.addActivity(create);
            this.activityPubStorage.storeEntity(inbox);
            this.notifier.notify(create, Collections.singleton(this.actorHandler.getXWikiUserReference(actor)));
            this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, create);
        }
    }

    @Override
    public void handleOutboxRequest(ActivityRequest<Create> activityRequest)
        throws IOException, ActivityPubException
    {
        Create create = activityRequest.getActivity();
        if (create.getId() == null) {
            this.activityPubStorage.storeEntity(create);
        }

        AbstractActor actor = activityRequest.getActor();
        Outbox outbox = this.getOutbox(actor);
        outbox.addActivity(create);
        this.activityPubStorage.storeEntity(outbox);

        ResolvedTargets resolvedTargets = this.getTargets(create);

        for (AbstractActor targetActor : resolvedTargets.getActorTargets()) {
            create.getObject().setExpand(true);
            HttpMethod postMethod = this.activityPubClient.postInbox(targetActor, create);

            try {
                this.activityPubClient.checkAnswer(postMethod);
            } catch (ActivityPubException e) {
                // FIXME: in that case is the final answer still a 200 OK?
                this.logger.error("The sharing to followers didn't go well.", e);
            } finally {
                postMethod.releaseConnection();
            }
        }
        this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, create);
    }
}
