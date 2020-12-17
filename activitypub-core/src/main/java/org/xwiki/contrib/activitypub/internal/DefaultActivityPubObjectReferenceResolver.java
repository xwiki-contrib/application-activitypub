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
package org.xwiki.contrib.activitypub.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.ProxyActor;

/**
 * Default implementation of {@link ActivityPubObjectReferenceResolver}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubObjectReferenceResolver implements ActivityPubObjectReferenceResolver
{
    @Inject
    private DateProvider dateProvider;

    @Inject
    private ActivityPubJsonParser activityPubJsonParser;

    @Inject
    private Provider<ActivityPubClient> activityPubClientProvider;

    @Inject
    private Provider<ActivityPubStorage> activityPubStorageProvider;

    @Inject
    private DefaultURLHandler defaultURLHandler;

    @Inject
    private Logger logger;

    @Override
    public <T extends ActivityPubObject> T resolveReference(ActivityPubObjectReference<T> reference)
        throws ActivityPubException
    {
        if (reference == null) {
            throw new ActivityPubException("Cannot resolve null reference");
        }
        T result = reference.getObject();
        if (!reference.isLink() && result == null) {
            throw new ActivityPubException("The reference property is null and does not have any ID to follow.");
        }

        // We try first to retrieve the object from the storage
        ActivityPubStorage activityPubStorage = this.activityPubStorageProvider.get();
        if (result == null) {
            result = activityPubStorage.retrieveEntity(reference.getLink());
            reference.setObject(result);
        }

        // If the storage didn't provide any result, or if it provided outdated result, then we need to reload the
        // information.
        if (result == null || this.shouldBeRefreshed(result)) {
            try {
                ActivityPubClient activityPubClient = this.activityPubClientProvider.get();
                HttpMethod getMethod = activityPubClient.get(reference.getLink());
                try {
                    activityPubClient.checkAnswer(getMethod);
                    result = this.activityPubJsonParser.parse(getMethod.getResponseBodyAsString());
                } finally {
                    getMethod.releaseConnection();
                }
                reference.setObject(result);
                activityPubStorage.storeEntity(result);
            } catch (IOException | ActivityPubException e) {
                // We might be trying to refresh an information, in that case we just rely on the information we already
                // manage to retrieve from the storage.
                if (result == null) {
                    throw new ActivityPubException(
                        String
                            .format("Error when retrieving the ActivityPub information from [%s]", reference.getLink()),
                        e);
                }
            }
        }
        return result;
    }

    @Override
    public Set<AbstractActor> resolveTargets(ActivityPubObject activityPubObject)
    {
        Set<AbstractActor> resolvedTargets;
        if (activityPubObject.getComputedTargets() == null) {
            resolvedTargets = new HashSet<>();
            this.resolveProxyActorList(activityPubObject.getTo(), resolvedTargets);
            activityPubObject.setComputedTargets(resolvedTargets);
        } else {
            resolvedTargets = activityPubObject.getComputedTargets();
        }
        return resolvedTargets;
    }

    @Override
    public <T extends ActivityPubObject> boolean shouldBeRefreshed(T activityPubObject)
    {
        Date date = new Date();
        Date lastUpdated = activityPubObject.getLastUpdated();
        return (CLASSES_TO_REFRESH.contains(activityPubObject.getClass())
            && !this.defaultURLHandler.belongsToCurrentInstance(activityPubObject.getId())
            && activityPubObject.getLastUpdated() != null
            && this.dateProvider.isElapsed(date, lastUpdated, MAX_DAY_BEFORE_REFRESH));
    }

    private void resolveProxyActorList(List<ProxyActor> proxyActorList, Set<AbstractActor> resolvedTargets)
    {
        if (proxyActorList != null && !proxyActorList.isEmpty()) {
            List<ActivityPubObjectReference<AbstractActor>> targetActors = new ArrayList<>();

            for (ProxyActor proxyActor : proxyActorList) {
                if (!proxyActor.isPublic()) {
                    try {
                        targetActors.addAll(proxyActor.resolveActors(this));
                    } catch (ActivityPubException e) {
                        // FIXME: for now we only log the error, in the future it would need a specific handling
                        // to try again later. See XAP-39
                        this.logger.error("Cannot resolve proxy actor [{}].", proxyActor, e);
                    }
                }
            }

            for (ActivityPubObjectReference<AbstractActor> actorReference : targetActors) {
                try {
                    resolvedTargets.add(this.resolveReference(actorReference));
                } catch (ActivityPubException e) {
                    // FIXME: for now we only log the error, in the future it would need a specific handling
                    // to try again later. See XAP-39
                    this.logger.error("Cannot resolve actor [{}].", actorReference, e);
                }
            }
        }
    }
}
