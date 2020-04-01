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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
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
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.ProxyActor;

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

    @Inject
    protected Logger logger;

    protected static class ResolvedTargets
    {
        private Set<AbstractActor> actorTargets;
        private boolean containsPublic;

        public Set<AbstractActor> getActorTargets()
        {
            return actorTargets;
        }

        public boolean isContainsPublic()
        {
            return containsPublic;
        }
    }

    protected ResolvedTargets getTargets(T activity)
    {
        ResolvedTargets resolvedTargets = new ResolvedTargets();
        resolvedTargets.actorTargets = new HashSet<>();
        this.resolveProxyActorList(activity.getTo(), resolvedTargets);
        return resolvedTargets;
    }

    private void resolveProxyActorList(List<ProxyActor> proxyActorList, ResolvedTargets resolvedTargets)
    {
        if (proxyActorList != null && !proxyActorList.isEmpty()) {
            List<ActivityPubObjectReference<AbstractActor>> targetActors = new ArrayList<>();

            for (ProxyActor proxyActor : proxyActorList) {
                if (!proxyActor.isPublic()) {
                    try {
                        targetActors.addAll(proxyActor.resolveActors(this.activityPubObjectReferenceResolver));
                    } catch (ActivityPubException e) {
                        // FIXME: for now we only log the error, in the future it would need a specific handling
                        // to try again later. See XAP-39
                        this.logger.error("Cannot resolve proxy actor [{}].", proxyActor, e);
                    }
                } else {
                    resolvedTargets.containsPublic = true;
                }
            }

            for (ActivityPubObjectReference<AbstractActor> actorReference : targetActors) {
                try {
                    resolvedTargets.actorTargets.add(
                        this.activityPubObjectReferenceResolver.resolveReference(actorReference));
                } catch (ActivityPubException e) {
                    // FIXME: for now we only log the error, in the future it would need a specific handling
                    // to try again later. See XAP-39
                    this.logger.error("Cannot resolve actor [{}].", actorReference, e);
                }
            }
        }
    }

    /**
     * Helper method to return the actual {@link Inbox} from an {@link AbstractActor}.
     * @param actor the actor from which to retrieve the inbox
     * @return a concrete instance of inbox
     * @throws ActivityPubException in case of error when resolving the reference.
     */
    protected Inbox getInbox(AbstractActor actor) throws ActivityPubException
    {
        return this.activityPubObjectReferenceResolver.resolveReference(actor.getInbox());
    }

    /**
     * Helper method to return the actual {@link Outbox} from an {@link AbstractActor}.
     * @param actor the actor from which to retrieve the outbox
     * @return a concrete instance of outbox
     * @throws ActivityPubException in case of error when resolving the reference.
     */
    protected Outbox getOutbox(AbstractActor actor) throws ActivityPubException
    {
        return this.activityPubObjectReferenceResolver.resolveReference(actor.getOutbox());
    }

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
            response.setContentType(ActivityPubClient.CONTENT_TYPE_STRICT);
            response.setCharacterEncoding("UTF-8");
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
        } else {
            this.logger.error(error);
        }
    }
}
