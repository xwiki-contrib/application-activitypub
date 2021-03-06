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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Announce;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.contrib.activitypub.entities.Like;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.internal.DateProvider;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.InternalURINormalizer;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.internal.script.ActivityPubScriptServiceActor;
import org.xwiki.contrib.activitypub.internal.stream.StreamActivityPubObjectReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

import static java.util.Collections.singletonList;
import static org.apache.solr.client.solrj.util.ClientUtils.escapeQueryChars;

/**
 * Script service for ActivityPub.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named(ActivityPubScriptService.ROLEHINT)
@Unstable
public class ActivityPubScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     *
     * @since 1.5
     */
    @Unstable
    public static final String ROLEHINT = "activitypub";

    private static final String GET_CURRENT_ACTOR_ERR_MSG = "Failed to retrieve the current actor. Cause [{}].";

    private static final String GET_ACTOR_ERROR_MSG = "Error while trying to get the actor [{}].";

    private static final String GET_CURRENT_ACTOR_UNEXPECTED_ERR_MSG =
        "Failed to retrieve the current actor. Unexpected Cause [{}].";

    private static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER);

    private static final String MESSAGE_QUERY_FILTER = "filter(%s:%s)";

    @Inject
    private DateProvider dateProvider;

    @Inject
    private ActivityPubClient activityPubClient;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

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

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private InternalURINormalizer internalURINormalizer;

    @Inject
    private StreamActivityPubObjectReferenceResolver streamActivityPubObjectReferenceResolver;

    @Inject
    private DefaultURLHandler urlHandler;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    private PublishNoteScriptService publishNoteScriptService;

    @Inject
    private ActivityPubScriptServiceActor activityPubScriptServiceActor;

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null if none could be found
     * @since 1.5
     */
    @Unstable
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(ROLEHINT + '.' + serviceName);
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
        return this.activityPubScriptServiceActor.getActor(actor);
    }

    /**
     * Resolve an actor from a {@link DocumentReference}.
     *
     * @param actor the {@link DocumentReference} of the actor
     * @return the resolved actor or {code null} if the resolution failed
     * @since 1.3
     */
    @Unstable
    public AbstractActor getActor(DocumentReference actor)
    {
        try {
            return this.actorHandler.getActor(actor);
        } catch (ActivityPubException e) {
            this.logger.error(GET_ACTOR_ERROR_MSG, actor, e);
        }
        return null;
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
     * Check if the currently logged-in user can act on behalf of the given actor. This method is useful if the current
     * user wants to publish something as the global Wiki actor for example.
     *
     * @param actor the actor the current user wants to act for.
     * @return {@code true} iff the current user is authorized to act for the given actor.
     * @since 1.2
     */
    @Unstable
    public boolean currentUserCanActFor(AbstractActor actor)
    {
        UserReference userReference = null;
        try {
            this.activityPubScriptServiceActor.checkAuthentication();
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
     *
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
     *
     * @param remoteActor the actor to follow.
     * @return {@code true} iff the request has been sent properly.
     */
    public FollowResult follow(AbstractActor remoteActor)
    {
        return follow(remoteActor, null);
    }

    /**
     * Send a Follow request to the given actor.
     *
     * @param remoteActor the actor to follow.
     * @param sourceActor the source of the follow: if null the current actor will be resolved and used.
     * @return {@code true} iff the request has been sent properly.
     * @since 1.2
     */
    @Unstable
    public FollowResult follow(AbstractActor remoteActor, AbstractActor sourceActor)
    {
        FollowResult result = new FollowResult("activitypub.follow.followNotRequested");

        try {
            AbstractActor currentActor = this.activityPubScriptServiceActor.getSourceActor(sourceActor);
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
     * Compute and return the list of resolved targets. See {@link ActivityPubObjectReferenceResolver#resolveTargets(ActivityPubObject)}
     * for details.
     *
     * @param activityPubObject the object for which to compute the targets.
     * @return a set of {@link AbstractActor} being the concrete targets of the given object.
     * @since 1.2
     */
    @Unstable
    public Set<AbstractActor> resolveTargets(ActivityPubObject activityPubObject)
    {
        return this.activityPubObjectReferenceResolver.resolveTargets(activityPubObject);
    }

    /**
     * Resolve and returns the given {@link ActivityPubObjectReference}.
     *
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
     * Resolve the given document reference as a {@link Page}.
     * @param documentReference the document reference to resolve.
     * @return a page corresponding to the document reference.
     * @since 1.5
     */
    @Unstable
    public Page resolveDocument(DocumentReference documentReference)
    {
        try {
            return this.activityPubObjectReferenceResolver.resolveDocumentReference(documentReference);
        } catch (ActivityPubException e) {
            this.logger.error("Error while trying to resolve document reference [{}]", documentReference, e);
            return null;
        }
    }

    /**
     * Share a page.
     *
     * @param targets List of recipients of the sharing.
     * @param pageReference The serialized reference to the page to be shared.
     * @return True if the shared succeeded, false otherwise.
     */
    public boolean sharePage(List<String> targets, String pageReference)
    {
        try {
            // get current actor
            AbstractActor currentActor = this.activityPubScriptServiceActor.getSourceActor(null);

            DocumentReference documentReference = this.documentReferenceResolver.resolve(pageReference);
            boolean guestAccess =
                this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference);

            /*
             * Pages that cannot be viewed by the guest user are not allowed to be shared.
             * Additionally, sharing a page can only be realized by logged in users.
             */
            if (guestAccess && currentActor != null) {
                XWikiContext context = this.contextProvider.get();
                XWikiDocument xwikiDoc = context.getWiki().getDocument(documentReference, context);
                String content = this.htmlRenderer.render(xwikiDoc.getXDOM(), documentReference);
                URI documentUrl = this.urlHandler.getAbsoluteURI(URI.create(xwikiDoc.getURL("view", context)));
                Page page = this.activityPubObjectReferenceResolver.resolveDocumentReference(documentReference);
                page.setName(xwikiDoc.getTitle())
                    .setAttributedTo(singletonList(currentActor.getReference()))
                    .setUrl(singletonList(documentUrl))
                    .setContent(content)
                    .setPublished(xwikiDoc.getContentUpdateDate());
                this.activityPubScriptServiceActor.fillRecipients(targets, currentActor, page);
                this.activityPubStorage.storeEntity(page);

                Date published = this.dateProvider.currentTime();
                Announce announce = new Announce()
                    .setActor(currentActor)
                    .setObject(page)
                    .setAttributedTo(page.getAttributedTo())
                    .setTo(page.getTo())
                    .setPublished(published);
                this.activityPubStorage.storeEntity(announce);

                this.activityPubScriptServiceActor.getActivityHandler(announce)
                    .handleOutboxRequest(new ActivityRequest<>(currentActor, announce));
                return true;
            } else {
                return false;
            }
        } catch (IOException | ActivityPubException | XWikiException | URISyntaxException e) {
            this.logger.error("Error while sharing a page.", e);
            return false;
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
     *
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
            abstractActorStream = activityPubObjectReferences.getOrderedItems().stream()
                .map(this.streamActivityPubObjectReferenceResolver.getFunction());
        }
        return Optional.ofNullable(abstractActorStream);
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
                return activityPubObjectReferences.getOrderedItems().stream()
                    .map(this.streamActivityPubObjectReferenceResolver.getFunction())
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
     *
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
     *
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
     * Retrieve the messages sent by the given actor.
     *
     * @param actor the actor who sent messages or null for the current actor.
     * @param limit the maximum number of messages to retrieve.
     * @return a list of {@link Note} or an empty list.
     * @since 1.2
     */
    @Unstable
    public List<Note> getSentMessages(AbstractActor actor, int limit)
    {
        try {
            AbstractActor currentActor = this.activityPubScriptServiceActor.getSourceActor(actor);
            // IDs are stored with relative URI.
            URI actorRelativeURI = this.internalURINormalizer.relativizeURI(currentActor.getId());
            String query = String.format(MESSAGE_QUERY_FILTER, ActivityPubStorage.AUTHORS_FIELD,
                escapeQueryChars(actorRelativeURI.toASCIIString()));
            return this.activityPubStorage.query(Note.class, query, limit);
        } catch (ActivityPubException e) {
            this.logger.warn(GET_CURRENT_ACTOR_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
    }

    /**
     * Retrieve the messages received by the given actor.
     *
     * @param actor the actor who received messages or null for the current actor.
     * @param limit the maximum number of messages to retrieve.
     * @return a list of {@link Note} or an empty list.
     * @since 1.2
     */
    @Unstable
    public List<Note> getReceivedMessages(AbstractActor actor, int limit)
    {
        try {
            AbstractActor currentActor = this.activityPubScriptServiceActor.getSourceActor(actor);
            // IDs are stored with relative URI.
            URI actorRelativeURI = this.internalURINormalizer.relativizeURI(currentActor.getId());
            String query = String.format(MESSAGE_QUERY_FILTER, ActivityPubStorage.TARGETED_FIELD,
                escapeQueryChars(actorRelativeURI.toASCIIString()));
            return this.activityPubStorage.query(Note.class, query, limit);
        } catch (ActivityPubException e) {
            this.logger.warn(GET_CURRENT_ACTOR_ERR_MSG, ExceptionUtils.getRootCauseMessage(e));
        }
        return Collections.emptyList();
    }

    /**
     * Record a Like for the object of the activity referenced by the given id.
     *
     * @param activityId the ID of an activity to like the object of.
     * @return {@code true} if the like has been properly performed.
     * @since 1.4
     */
    @Unstable
    public boolean likeActivity(String activityId)
    {
        if (!isLiked(activityId)) {
            try {
                AbstractActor currentActor = this.actorHandler.getCurrentActor();
                AbstractActivity activity = this.activityPubObjectReferenceResolver
                    .resolveReference(
                        new ActivityPubObjectReference<AbstractActivity>().setLink(URI.create(activityId)));
                ActivityPubObjectReference<? extends ActivityPubObject> objectReference = activity.getObject();
                if (objectReference != null) {
                    ActivityPubObject activityPubObject =
                        this.activityPubObjectReferenceResolver.resolveReference(objectReference);
                    Like likeActivity = new Like().setActor(currentActor).setObject(activityPubObject);
                    this.activityPubStorage.storeEntity(likeActivity);
                    this.activityPubScriptServiceActor.getActivityHandler(likeActivity)
                        .handleOutboxRequest(new ActivityRequest<>(currentActor, likeActivity));
                    AbstractActor originalActivityActor =
                        this.activityPubObjectReferenceResolver.resolveReference(activity.getActor());
                    HttpMethod httpMethod = this.activityPubClient.postInbox(originalActivityActor, likeActivity);
                    try {
                        this.activityPubClient.checkAnswer(httpMethod);
                    } finally {
                        httpMethod.releaseConnection();
                    }
                    return true;
                }
            } catch (ActivityPubException | ClassCastException | IOException e) {
                this.logger.warn("Error while liking activity [{}]: [{}]",
                    activityId, ExceptionUtils.getRootCauseMessage(e));
            }
        } else {
            this.logger.debug("Activity [{}] has already been liked.", activityId);
        }
        return false;
    }

    /**
     * Retrieve the number of likes associated to the document but performed on the fediverse.
     * @param reference the reference for which to retrieve the like number.
     * @return the number of like realized on the fediverse.
     * @since 1.5
     */
    @Unstable
    public int getLikeNumber(DocumentReference reference)
    {
        int result = 0;
        try {
            Page page = this.activityPubObjectReferenceResolver.resolveDocumentReference(reference);
            if (page.getLikes() != null) {
                OrderedCollection<Like> likes =
                    this.activityPubObjectReferenceResolver.resolveReference(page.getLikes());
                result = likes.getTotalItems();
            }
        } catch (ActivityPubException e) {
            this.logger.warn("Error while computing like number for [{}]", reference, e);
        }
        return result;
    }

    /**
     * Check if an activity has already been liked.
     *
     * @param activityId the activity to check for like.
     * @return {@code true} if the activity is already present in the likes of the actor.
     * @since 1.4
     */
    @Unstable
    public boolean isLiked(String activityId)
    {
        try {
            AbstractActor currentActor = this.actorHandler.getCurrentActor();
            OrderedCollection<? extends ActivityPubObject> likedElements =
                this.activityPubObjectReferenceResolver.resolveReference(currentActor.getLiked());
            AbstractActivity activity = this.activityPubObjectReferenceResolver
                .resolveReference(
                    new ActivityPubObjectReference<AbstractActivity>().setLink(URI.create(activityId)));
            ActivityPubObjectReference objectReference = activity.getObject();
            return likedElements.contains(objectReference);
        } catch (ActivityPubException | ClassCastException e) {
            this.logger.warn(String.format("Error while checking if activity [%s] is liked", activityId),
                ExceptionUtils.getRootCauseMessage(e));
        }
        return false;
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

    /**
     * Escape the xwiki syntax from the content.
     *
     * @param content the content to escape
     * @return the escaped content
     * @since 1.4
     */
    @Unstable
    public String escapeXWikiSyntax(String content)
    {
        if (content == null) {
            return null;
        }
        return content.replaceAll("\\{\\{", "&#123;&#123;");
    }

    /**
     * @param url An url.
     * @return True of the url points to a content of the current instance.
     * @since 1.2
     */
    public boolean belongsToCurrentInstance(String url)
    {
        try {
            return this.urlHandler.belongsToCurrentInstance(URI.create(url));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return the service dedicated to publish notes
     * @since 1.5
     */
    @Unstable
    public PublishNoteScriptService getPublishNote()
    {
        return this.publishNoteScriptService;
    }
}
