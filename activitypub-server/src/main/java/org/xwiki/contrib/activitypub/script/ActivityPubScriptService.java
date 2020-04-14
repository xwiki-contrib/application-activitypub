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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
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
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Announce;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Document;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Script service for ActivityPub.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named("activitypub")
@Unstable
public class ActivityPubScriptService implements ScriptService
{
    private static final String GET_CURRENT_ACTOR_ERR_MSG = "Failed to retrieve the current actor. Cause [{}].";

    private static final String GET_CURRENT_ACTOR_UNEXPECTED_ERR_MSG =
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
    private ActivityHandler<Announce> announceActivityHandler;

    @Inject
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Inject
    private HTMLRenderer htmlRenderer;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private void checkAuthentication() throws ActivityPubException
    {
        UserReference userReference = this.userReferenceResolver.resolve(null);
        if (userReference == GuestUserReference.INSTANCE) {
            throw new ActivityPubException("You need to be authenticated to use this method.");
        }
    }

    /**
     * Retrieve an ActivityPub actor to be used in the methods of the script service. It follows this strategy for
     * resolving the given string: - if the string is null or blank, it resolves it by getting the current logged-in
     * user actor. It will throw an {@link ActivityPubException} if no one is logged-in. - if the string starts with
     * {@code http} or {@code @}, the actor will be resolved using {@link ActorHandler#getActor(String)}. The method
     * returns null if no actor is found.
     *
     * @param actor the string to resolved following the strategy described above.
     * @return an ActivityPub actor or null.
     * @since 1.1
     */
    @Unstable
    public AbstractActor getActor(String actor)
    {
        AbstractActor result = null;
        try {
            if (isBlank(actor)) {
                this.checkAuthentication();
                result = this.actorHandler.getCurrentActor();
            } else {
                result = this.actorHandler.getActor(actor.trim());
            }
        } catch (ActivityPubException e) {
            this.logger.error("Error while trying to get the actor [{}].", actor, e);
        }
        return result;
    }

    /**
     * @return a Service actor corresponding to the current wiki.
     * @since 1.2
     */
    @Unstable
    public Service getCurrentWikiActor()
    {
        XWikiContext context = this.contextProvider.get();
        try {
            return this.actorHandler.getActor(context.getWikiReference());
        } catch (ActivityPubException e) {
            this.logger.error("Error while trying to get the wiki actor [{}].", context.getWikiReference(), e);
        }
        return null;
    }

    /**
     * Check if the currently logged-in user can act on behalf of the given actor.
     * This method is useful if the current user wants to publish something as the global Wiki actor for example.
     * @param actor the actor the current user wants to act for.
     * @return {@code true} iff the current user is authorized to act for the given actor.
     * @since 1.2
     */
    @Unstable
    public boolean currentUserCanActFor(AbstractActor actor)
    {
        UserReference userReference = null;
        try {
            this.checkAuthentication();
            userReference = this.userReferenceResolver.resolve(null);
            return this.actorHandler.isAuthorizedToActFor(userReference, actor);
        } catch (ActivityPubException e) {
            this.logger.debug("Error while trying to check authorization for the actor [{}] with user reference [{}].",
                actor, userReference, e);
        }
        return false;
    }

    /**
     * Verify if the given actor is the current user.
     * @param actor the retrieved actor to check.
     * @return {@code true} if the given actor is the current logged user.
     * @since 1.1
     */
    @Unstable
    public boolean isCurrentUser(Person actor)
    {
        UserReference userReference = this.userReferenceResolver.resolve(null);
        try {
            return userReference.equals(this.actorHandler.getXWikiUserReference(actor));
        } catch (ActivityPubException e) {
            this.logger.error("Error while getting user reference for actor [{}]", actor, e);
            return false;
        }
    }

    /**
     * Send a Follow request to the given actor.
     * @param remoteActor the actor to follow.
     * @return {@code true} iff the request has been sent properly.
     */
    public FollowResult follow(AbstractActor remoteActor)
    {
        return follow(remoteActor, null);
    }

    /**
     * Send a Follow request to the given actor.
     * @param remoteActor the actor to follow.
     * @param sourceActor the source of the follow: if null the current actor will be resolved and used.
     * @return {@code true} iff the request has been sent properly.
     * @since 1.2
     */
    public FollowResult follow(AbstractActor remoteActor, AbstractActor sourceActor)
    {
        FollowResult result = new FollowResult("activitypub.follow.followNotRequested");

        try {
            AbstractActor currentActor = this.getSourceActor(sourceActor);
            if (Objects.equals(currentActor, remoteActor)) {
                // can't follow yourself.
                return result.setMessage("activitypub.follow.followYourself");
            }

            Optional<Stream<AbstractActor>> oaas = this.getAbstractActorStream(currentActor);
            Optional<Boolean> aBoolean = oaas.map(s -> s.anyMatch(f -> Objects.equals(f, remoteActor)));
            if (aBoolean.orElse(false)) {
                // can't follow the same user twice.
                return result.setMessage("activitypub.follow.alreadyFollowed");
            }

            Follow follow = new Follow().setActor(currentActor).setObject(remoteActor);
            this.activityPubStorage.storeEntity(follow);
            HttpMethod httpMethod = this.activityPubClient.postInbox(remoteActor, follow);
            try {
                this.activityPubClient.checkAnswer(httpMethod);
            } finally {
                httpMethod.releaseConnection();
            }
            result.setSuccess(true).setMessage("activitypub.follow.followRequested");
        } catch (ActivityPubException | IOException e) {
            this.logger.error("Error while trying to send a follow request to [{}].", remoteActor, e);
        }

        return result;
    }

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
     * Retrieve the actor will perform an action and perform checks on it.
     * If targetActor is null, then the current actor as defined by {@link ActorHandler#getCurrentActor()} is used.
     * Else, if targetActor is given, we check if the current logged-in user can act on behalf of this actor thanks to
     * {@link ActorHandler#isAuthorizedToActFor(UserReference, AbstractActor)} implementation.
     * If the authorization is not met, or no user is currently logged-in, an exception is thrown.
     *
     * @param targetActor the actual actor that must be used to perform an action, or null to use the current actor
     * @return the actor given in parameter or the resolved current actor
     * @throws ActivityPubException if no user is logged in or in case of authorization error.
     */
    private AbstractActor getSourceActor(AbstractActor targetActor) throws ActivityPubException
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
     * Publish the given content as a note to be send to the adressed target.
     * The given targets can take different values:
     *   - followers: means that the note will be sent to the followers
     *   - an URI qualifying an actor: means that the note will be sent to that actor
     * If the list of targets is empty or null, it means the note will be private only.
     * @param targets the list of targets for the note (see below for information about accepted values)
     * @param content the actual concent of the note
     * @return {@code true} if everything went well, else return false.
     */
    public boolean publishNote(List<String> targets, String content)
    {
        return publishNote(targets, content, null);
    }

    /**
     * Publish the given content as a note to be send to the adressed target.
     * The given targets can take different values:
     *   - followers: means that the note will be sent to the followers
     *   - an URI qualifying an actor: means that the note will be sent to that actor
     * If the list of targets is empty or null, it means the note will be private only.
     * @param targets the list of targets for the note (see below for information about accepted values)
     * @param content the actual concent of the note
     * @param sourceActor the actor responsible from this message: if null, the current actor will be used.
     * @return {@code true} if everything went well, else return false.
     * @since 1.2
     */
    @Unstable
    public boolean publishNote(List<String> targets, String content, AbstractActor sourceActor)
    {
        try {
            AbstractActor currentActor = this.getSourceActor(sourceActor);

            Note note = new Note()
                    .setAttributedTo(Collections.singletonList(currentActor.getReference()))
                    .setContent(content);
            this.fillRecipients(targets, currentActor, note);
            this.activityPubStorage.storeEntity(note);

            Create create = new Create()
                    .setActor(currentActor)
                    .setObject(note)
                    .setAttributedTo(note.getAttributedTo())
                    .setTo(note.getTo())
                    .setPublished(new Date());
            this.activityPubStorage.storeEntity(create);

            this.createActivityHandler.handleOutboxRequest(new ActivityRequest<>(currentActor, create));
            return true;
        } catch (IOException | ActivityPubException e) {
            this.logger.error("Error while posting a note.", e);
            return false;
        }
    }

    /**
     * Share a page.
     *
     * @param targets List of recipients of the sharing.
     * @param page    The page to be shared.
     * @return True if the shared succeeded, false otherwise.
     */
    public boolean sharePage(List<String> targets, String page)
    {
        try {
            // get current actor
            AbstractActor currentActor = this.getSourceActor(null);

            DocumentReference dr = this.documentReferenceResolver.resolve(page);
            XWikiDocument xwikiDoc =
                    this.contextProvider.get().getWiki().getDocument(dr, this.contextProvider.get());
            String content = this.htmlRenderer.render(xwikiDoc.getXDOM(), dr);
            Document document = new Document()
                    .setName(xwikiDoc.getTitle())
                    .setAttributedTo(Collections.singletonList(currentActor.getReference()))
                    .setContent(content);
            this.fillRecipients(targets, currentActor, document);
            this.activityPubStorage.storeEntity(document);

            Announce announce = new Announce()
                    .setActor(currentActor)
                    .setObject(document)
                    .setAttributedTo(document.getAttributedTo())
                    .setTo(document.getTo())
                    .setPublished(new Date());
            this.activityPubStorage.storeEntity(announce);

            this.announceActivityHandler.handleOutboxRequest(new ActivityRequest<>(currentActor, announce));
            return true;
        } catch (IOException | ActivityPubException | XWikiException e) {
            this.logger.error("Error while sharing a page.", e);
            return false;
        }
    }

    /**
     * Fill the list of recipients of the object.
     *
     * @param targets The targets that will be filled as recipients.
     * @param actor The actor that send the object.
     * @param object The object that will be sent.
     */
    private void fillRecipients(List<String> targets, AbstractActor actor, ActivityPubObject object)
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
     * @param actor The actor of interest.
     * @return the list of actor followed by the current user.
     */
    public List<AbstractActor> following(AbstractActor actor)
    {
        try {
            Optional<Stream<AbstractActor>> abstractActorStream = this.getAbstractActorStream(actor);
            if (abstractActorStream.isPresent()) {
                return abstractActorStream.get().filter(Objects::nonNull).collect(Collectors.toList());
            }
        } catch (ActivityPubException e) {
            this.logger.warn(GET_CURRENT_ACTOR_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        } catch (Exception e) {
            this.logger.warn(GET_CURRENT_ACTOR_UNEXPECTED_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
    }

    /**
     * Return a stream of abstract actors following the actor.
     * @param actor The actor.
     * @return An optional stram of actor.
     * @throws ActivityPubException In case of error during the resolution of actors.
     */
    private Optional<Stream<AbstractActor>> getAbstractActorStream(AbstractActor actor)
        throws ActivityPubException
    {
        Stream<AbstractActor> abstractActorStream = null;
        ActivityPubObjectReference<OrderedCollection<AbstractActor>> following = actor.getFollowing();
        if (following != null) {
            OrderedCollection<AbstractActor> activityPubObjectReferences =
                this.activityPubObjectReferenceResolver.resolveReference(following);
            abstractActorStream = activityPubObjectReferences.getOrderedItems().stream().map(this::resolveActor);
        }
        return Optional.ofNullable(abstractActorStream);
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
     * @param actor The actor of interest.
     * @return the list of actors following the current user.
     */
    public List<AbstractActor> followers(AbstractActor actor)
    {
        try {
            ActivityPubObjectReference<OrderedCollection<AbstractActor>> followers = actor.getFollowers();
            if (followers != null) {
                OrderedCollection<AbstractActor> activityPubObjectReferences =
                    this.activityPubObjectReferenceResolver.resolveReference(followers);
                return activityPubObjectReferences.getOrderedItems().stream().map(this::resolveActor)
                           .filter(Objects::nonNull).collect(Collectors.toList());
            }
        } catch (ActivityPubException e) {
            this.logger.warn(GET_CURRENT_ACTOR_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        } catch (Exception e) {
            this.logger.warn(GET_CURRENT_ACTOR_UNEXPECTED_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
    }

    /**
     * Return the ActivityPub endpoint URL to the current wiki actor.
     * @return the URL to an activitypub endpoint for the actor linked to that user. Or null in case of error.
     */
    public String getActorURLFromCurrentWiki()
    {
        XWikiContext context = this.contextProvider.get();
        try {
            return actorHandler.getActor(context.getWikiReference()).getId().toASCIIString();
        } catch (ActivityPubException e) {
            logger.error("Cannot find the actor for Wiki [{}].", context.getWikiReference(), e);
            return null;
        }
    }

    /**
     * Return the ActivityPub endpoint URL to the user referred by the given document reference.
     * @param userDocumentReference a document reference to an XWiki user.
     * @return the URL to an activitypub endpoint for the actor linked to that user. Or null in case of error.
     */
    public String getActorURLFromDocumentReference(DocumentReference userDocumentReference)
    {
        UserReference userReference = this.xWikiUserBridge.resolveDocumentReference(userDocumentReference);
        try {
            return actorHandler.getActor(userReference).getId().toASCIIString();
        } catch (ActivityPubException e) {
            logger.error("Cannot find the actor from [{}].", userReference, e);
            return null;
        }
    }

    /**
     * @return {@code true} if the storage is ready.
     * @since 1.1
     */
    @Unstable
    public boolean isStorageReady()
    {
        return this.activityPubStorage.isStorageReady();
    }
}
