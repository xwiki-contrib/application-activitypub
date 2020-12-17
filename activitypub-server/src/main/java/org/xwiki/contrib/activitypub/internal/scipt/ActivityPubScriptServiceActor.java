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
package org.xwiki.contrib.activitypub.internal.scipt;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * This component provides operations common to the ActivitySub script service and its sub-components, regarding
 * operations related to the ActivityPub actors.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = { ActivityPubScriptServiceActor.class })
@Singleton
public class ActivityPubScriptServiceActor
{
    private static final String GET_ACTOR_ERROR_MSG = "Error while trying to get the actor [{}].";

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Retrieve the actor will perform an action and perform checks on it. If targetActor is null, then the current
     * actor as defined by {@link ActorHandler#getCurrentActor()} is used. Else, if targetActor is given, we check if
     * the current logged-in user can act on behalf of this actor thanks to {@link ActorHandler#isAuthorizedToActFor(UserReference,
     * AbstractActor)} implementation. If the authorization is not met, or no user is currently logged-in, an exception
     * is thrown.
     *
     * @param targetActor the actual actor that must be used to perform an action, or null to use the current actor
     * @return the actor given in parameter or the resolved current actor
     * @throws ActivityPubException if no user is logged in or in case of authorization error.
     */
    public AbstractActor getSourceActor(AbstractActor targetActor) throws ActivityPubException
    {
        AbstractActor currentActor;
        checkAuthentication();
        if (targetActor == null) {
            currentActor = this.actorHandler.getCurrentActor();
        } else {
            UserReference userReference = this.userReferenceResolver.resolve(null);
            if (this.actorHandler.isAuthorizedToActFor(userReference, targetActor)) {
                currentActor = targetActor;
            } else {
                throw new ActivityPubException(
                    String.format("You cannot act on behalf of actor [%s]", targetActor));
            }
        }
        return currentActor;
    }

    /**
     * Fill the list of recipients of the object.
     *
     * @param targets The targets that will be filled as recipients.
     * @param actor The actor that send the object.
     * @param object The object that will be sent.
     */
    public void fillRecipients(List<String> targets, AbstractActor actor, ActivityPubObject object)
    {
        if (targets != null && !targets.isEmpty()) {
            List<ProxyActor> to = new ArrayList<>();
            for (String target : targets) {
                if ("followers".equals(target)) {
                    to.add(new ProxyActor(actor.getFollowers().getLink()));
                } else if ("public".equals(target)) {
                    to.add(ProxyActor.getPublicActor());
                } else {
                    AbstractActor remoteActor = this.getActor(target);
                    if (remoteActor != null) {
                        to.add(remoteActor.getProxyActor());
                    }
                }
            }
            object.setTo(to);
        }
    }

    /**
     * Find an actor by its identifier.
     *
     * @param actorIdentifier the actor identifier
     * @return the actor resolve from the identifier, {@code null} if the actor is not found
     */
    public AbstractActor getActor(String actorIdentifier)
    {
        AbstractActor result = null;
        try {
            if (isBlank(actorIdentifier)) {
                checkAuthentication();
                result = this.actorHandler.getCurrentActor();
            } else {
                result = this.actorHandler.getActor(actorIdentifier.trim());
            }
        } catch (ActivityPubException e) {
            this.logger.error(GET_ACTOR_ERROR_MSG, actorIdentifier, e);
        }
        return result;
    }

    /**
     * Verify if the current user is authenticated.
     *
     * @throws ActivityPubException in case of error during the verification
     */
    public void checkAuthentication() throws ActivityPubException
    {
        UserReference userReference = this.userReferenceResolver.resolve(null);
        if (userReference == GuestUserReference.INSTANCE) {
            throw new ActivityPubException("You need to be authenticated to use this method.");
        }
    }

    /**
     * Resolve the handler according to the type of the activity.
     *
     * @param activity the activity
     * @param <T> the type of the activity
     * @return the handle corresponding to the type of the activity
     * @throws ActivityPubException in case of error during the resolution of the handlers
     */
    public <T extends AbstractActivity> ActivityHandler<T> getActivityHandler(T activity) throws ActivityPubException
    {
        try {
            return this.componentManager
                .getInstance(new DefaultParameterizedType(null, ActivityHandler.class, activity.getClass()));
        } catch (ComponentLookupException e) {
            throw new ActivityPubException(
                String.format("Cannot find activity handler component for activity [%s]", activity.getClass()), e);
        }
    }
}
