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
package org.xwiki.contrib.activitypub.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.script.service.ScriptService;
import org.xwiki.user.UserReference;

/**
 * Script service for ActivityPub.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("activitypub")
public class ActivityPubScriptService implements ScriptService
{
    private static final String GET_CURRENT_ACTOR_ERR_MSG = "Failed to retrieve the current actor. Cause [{}].";

    private static final String GET_CURRENT_ACTOR_UNEXPECTED8ERR_MSG =
        "Failed to retrieve the current actor. Unexpected Cause [{}].";

    @Inject
    private ActivityPubClient activityPubClient;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    private ActivityHandler<Create> createActivityHandler;

    @Inject
    private Logger logger;

    /**
     * Send a Follow request to the given actor.
     * @param actor URL to the actor to follow.
     * @return {@code true} iff the request has been sent properly.
     */
    // FIXME: this should only be used when authenticated
    public boolean follow(String actor)
    {
        boolean result = false;

        try {
            AbstractActor remoteActor = this.actorHandler.getRemoteActor(actor);
            AbstractActor currentActor = this.actorHandler.getCurrentActor();
            Follow follow = new Follow().setActor(currentActor).setObject(remoteActor);
            this.activityPubStorage.storeEntity(follow);
            HttpMethod httpMethod = this.activityPubClient.postInbox(remoteActor, follow);
            try {
                this.activityPubClient.checkAnswer(httpMethod);
            } finally {
                httpMethod.releaseConnection();
            }
            result = true;
        } catch (ActivityPubException | IOException e) {
            this.logger.error("Error while trying to send a follow request to [{}].", actor, e);
        }

        return result;
    }

// This is not supported in 1.0
//    /**
//     * Send an Accept request to a received Follow.
//     * @param follow the follow activity to accept.
//     * @return {@code true} iff the request has been sent properly.
//     */
//    // FIXME: we should check that the current actor and followed actor is the same.
//    public boolean acceptFollow(Follow follow)
//    {
//        boolean result = false;
//        try {
//            AbstractActor currentActor = this.actorHandler.getCurrentActor();
//            Accept accept = new Accept().setActor(currentActor).setObject(follow);
//            this.activityPubStorage.storeEntity(accept);
//            HttpMethod httpMethod = this.activityPubClient.postOutbox(currentActor, accept);
//            this.activityPubClient.checkAnswer(httpMethod);
//            result = true;
//        } catch (ActivityPubException | IOException e) {
//            this.logger.error("Error while trying to send the accept follow request [{}]", follow, e);
//        }
//        return result;
//    }

    /**
     * Resolve and returns the given {@link ActivityPubObjectReference}.
     * @param reference the reference to resolve.
     * @param <T> the type of the reference.
     * @return the resulted object.
     * @throws ActivityPubException in case of error during the resolving.
     */
    public <T extends ActivityPubObject> T resolve(ActivityPubObjectReference<T> reference) throws ActivityPubException
    {
        try {
            T ret = this.activityPubObjectReferenceResolver.resolveReference(reference);
            return ret;
        } catch (ActivityPubException e) {
            this.logger.error("Error while trying to resolve a reference [{}]", reference, e);
            return null;
        }
    }

    /**
     * Retrieve and return the reference of the given actor or null if the actor doesn't belong to the current wiki.
     * @param actor the actor for which to retrieve the reference.
     * @return null if the actor doesn't belong to the current wiki, else it returns its EntityReference.
     */
    public UserReference getXWikiUserReference(AbstractActor actor)
    {
        try {
            return this.actorHandler.getXWikiUserReference(actor);
        } catch (ActivityPubException e) {
            this.logger.error("Error while trying to get an XWiki user from an actor [{}]", actor, e);
            return null;
        }
    }

    /**
     * Publish the given content as a note to be send to the adressed target.
     * The given targets can take different values:
     *   - followers: means that the note will be sent to the followers
     *   - an URI qualifying an actor: means that the note will be sent to that actor
     * If the list of targets is empty or null, it means the note will be private only.
     * @param targets the list of targets for the note (see below for information about accepted values)
     * @param content the actual concent of the note
     */
    public void publishNote(List<String> targets, String content)
    {
        try {
            AbstractActor currentActor = this.actorHandler.getCurrentActor();
            Note note = new Note()
                .setAttributedTo(Collections.singletonList(currentActor.getReference()))
                .setContent(content);
            if (targets != null && !targets.isEmpty()) {
                List<ProxyActor> to = new ArrayList<>();
                for (String target : targets) {
                    if ("followers".equals(target)) {
                        to.add(new ProxyActor(currentActor.getFollowers().getLink()));
                    } else if ("public".equals(target)) {
                        to.add(ProxyActor.getPublicActor());
                    } else {
                        // FIXME: we should check if target is an URI or not
                        AbstractActor remoteActor = this.actorHandler.getRemoteActor(target);
                        to.add(remoteActor.getProxyActor());
                    }
                }
                note.setTo(to);
            }
            this.activityPubStorage.storeEntity(note);

            Create create = new Create()
                .setActor(currentActor)
                .setObject(note)
                .setAttributedTo(note.getAttributedTo())
                .setTo(note.getTo())
                .setPublished(new Date());
            this.activityPubStorage.storeEntity(create);

            this.createActivityHandler.handleOutboxRequest(new ActivityRequest<>(currentActor, create));
        } catch (IOException | ActivityPubException e)
        {
            this.logger.error("Error while posting a note.");
        }
    }

    /**
     *
     * @return the list of actor followed by the current user.
     */
    public List<AbstractActor> following()
    {
        try {
            AbstractActor currentActor = this.actorHandler.getCurrentActor();
            OrderedCollection<AbstractActor> activityPubObjectReferences =
                this.activityPubObjectReferenceResolver.resolveReference(currentActor.getFollowing());
            return activityPubObjectReferences.getOrderedItems().stream().map(this::resolveActor)
                       .filter(Objects::nonNull).collect(Collectors.toList());
        } catch (ActivityPubException e) {
            this.logger.warn(GET_CURRENT_ACTOR_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        } catch (Exception e) {
            this.logger.warn(GET_CURRENT_ACTOR_UNEXPECTED8ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
    }

    private AbstractActor resolveActor(ActivityPubObjectReference<AbstractActor> it)
    {
        try {
            return this.activityPubObjectReferenceResolver.resolveReference(it);
        } catch (ActivityPubException e) {
            return null;
        }
    }

    /**
     *
     * @return the list of actors following the current user.
     */
    public List<AbstractActor> followers()
    {
        try {
            AbstractActor currentActor = this.actorHandler.getCurrentActor();
            OrderedCollection<AbstractActor> activityPubObjectReferences =
                this.activityPubObjectReferenceResolver.resolveReference(currentActor.getFollowers());
            return activityPubObjectReferences.getOrderedItems().stream().map(this::resolveActor)
                       .filter(Objects::nonNull).collect(Collectors.toList());
        } catch (ActivityPubException e) {
            this.logger.warn(GET_CURRENT_ACTOR_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        } catch (Exception e) {
            this.logger.warn(GET_CURRENT_ACTOR_UNEXPECTED8ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
    }
}
