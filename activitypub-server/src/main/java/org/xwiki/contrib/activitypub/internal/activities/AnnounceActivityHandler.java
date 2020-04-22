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
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Announce;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;

/**
 * Specific handler for {@link org.xwiki.contrib.activitypub.entities.Announce} activities.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Singleton
public class AnnounceActivityHandler extends AbstractActivityHandler<Announce>
{
    @Override
    public void handleInboxRequest(ActivityRequest<Announce> activityRequest) throws IOException, ActivityPubException
    {
        Announce announce = activityRequest.getActivity();
        if (announce.getId() == null) {
            this.answerError(activityRequest.getResponse(), HttpServletResponse.SC_BAD_REQUEST,
                    "The ID of the activity must not be null.");
        } else {
            if (announce.getObject() != null) {
                announce.getObject().setExpand(true);
            }
            AbstractActor actor = activityRequest.getActor();
            Inbox inbox = this.getInbox(actor);
            inbox.addActivity(announce);
            this.activityPubStorage.storeEntity(inbox);
            ActivityPubObject object = this.getOrPersist(announce.getObject());
            ActivityPubObjectReference<OrderedCollection<Announce>> shares = this.getSharesOrInit(object);

            shares.getObject().addItem(announce);

            this.activityPubStorage.storeEntity(object);
            this.notifier.notify(announce, Collections.singleton(actor));
            this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, announce);
        }
    }

    /**
     * Get the shares collection of an {@link ActivityPubObject}. If the collection does not exist, it is created and
     * returned.
     *
     * @param activityPubObject The {@link ActivityPubObject} object.
     * @return The shares collection of the object, possibly created if missing.
     * @throws ActivityPubException In case of error during the object initialization.
     */
    private ActivityPubObjectReference<OrderedCollection<Announce>> getSharesOrInit(ActivityPubObject activityPubObject)
            throws ActivityPubException
    {
        ActivityPubObjectReference<OrderedCollection<Announce>> shares = activityPubObject.getShares();

        if (shares == null) {
            OrderedCollection<Announce> announces = new OrderedCollection<>();
            this.activityPubStorage.storeEntity(announces);
            shares = new ActivityPubObjectReference<OrderedCollection<Announce>>().setObject(announces);
        }
        return shares;
    }

    private ActivityPubObject getOrPersist(ActivityPubObjectReference<? extends ActivityPubObject> object)
            throws ActivityPubException
    {
        ActivityPubObject ret = this.activityPubStorage.retrieveEntity(object.getLink());
        if (ret == null) {
            ret = this.activityPubObjectReferenceResolver.resolveReference(object);
        }
        return ret;
    }

    @Override
    public void handleOutboxRequest(ActivityRequest<Announce> activityRequest)
            throws IOException, ActivityPubException
    {
        Announce announce = activityRequest.getActivity();
        if (announce.getId() == null) {
            this.activityPubStorage.storeEntity(announce);
        }

        AbstractActor actor = activityRequest.getActor();
        Outbox outbox = this.getOutbox(actor);
        outbox.addActivity(announce);
        this.activityPubStorage.storeEntity(outbox);

        for (AbstractActor targetActor : this.activityPubObjectReferenceResolver.resolveTargets(announce)) {
            announce.getObject().setExpand(true);
            HttpMethod postMethod = this.activityPubClient.postInbox(targetActor, announce);

            try {
                this.activityPubClient.checkAnswer(postMethod);
            } catch (ActivityPubException e) {
                // FIXME: in that case is the final answer still a 200 OK?
                this.logger.error("The sharing to followers didn't go well.", e);
            } finally {
                postMethod.releaseConnection();
            }
        }
        this.answer(activityRequest.getResponse(), HttpServletResponse.SC_OK, announce);
    }
}
