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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubIdentifierService;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.SignatureService;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.PublicKey;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.webfinger.WebfingerClient;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.contrib.activitypub.webfinger.entities.Link;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Link an ActivityPub actor to an XWiki User and retrieves the inbox/outbox of users.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActorHandler implements ActorHandler
{
    private static final List<String> SERVICE_USER_SPACE_REFERENCE = Arrays.asList("ActivityPub", "ServiceActors");

    private static final String STANDARD_ERROR_MESSAGE = "This type of actor is not supported yet [%s]";

    private static final String WEBFINGER_SEPARATOR = "@";

    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private ActivityPubClient activityPubClient;

    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private ActivityPubJsonParser jsonParser;

    @Inject
    private SignatureService signatureService;

    @Inject
    private WebfingerClient webfingerClient;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ActivityPubConfiguration activityPubConfiguration;

    @Inject
    private GroupManager groupManager;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DefaultURLHandler defaultURLHandler;

    @Inject
    private Logger logger;

    @Inject
    private ActivityPubIdentifierService activityPubIdentifierService;

    private HttpConnection jsoupConnection;

    private HttpConnection getJsoupConnection()
    {
        if (this.jsoupConnection == null) {
            this.jsoupConnection = new HttpConnection();
        }
        return this.jsoupConnection;
    }

    /**
     * Helper to inject a mock of HttpConnection for testing purpose.
     * @param connection the connection to use.
     */
    protected void setJsoupConnection(HttpConnection connection)
    {
        this.jsoupConnection = connection;
    }

    @Override
    public Person getCurrentActor() throws ActivityPubException
    {
        return getActor(CurrentUserReference.INSTANCE);
    }

    @Override
    public Person getActor(UserReference userReference) throws ActivityPubException
    {
        String errorMessage = String.format("Cannot find any user with reference [%s]", userReference);
        if (this.xWikiUserBridge.isExistingUser(userReference)) {
            String login = this.xWikiUserBridge.getUserLogin(userReference);
            ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("Person", login);
            Person actor;
            try {
                actor = this.activityPubStorage.retrieveEntity(this.serializer.serialize(resourceReference));
            } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
                throw new ActivityPubException(errorMessage, e);
            }
            if (actor == null) {
                UserProperties userProperties = this.xWikiUserBridge.resolveUser(userReference);
                String fullname = String.format("%s %s", userProperties.getFirstName(), userProperties.getLastName());
                DocumentReference dr = this.xWikiUserBridge.getDocumentReference(userReference);
                actor = this.createActor(new Person(), fullname, dr.getName(), dr.getWikiReference().getName());
            }
            return actor;
        } else {
            throw new ActivityPubException(errorMessage);
        }
    }

    @Override
    public Service getActor(WikiReference wikiReference) throws ActivityPubException
    {
        String login = wikiReference.getName();
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("Service", login);
        Service actor;
        try {
            actor = this.activityPubStorage.retrieveEntity(this.serializer.serialize(resourceReference));
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            throw new ActivityPubException(
                String.format("Error while serializing reference to find actor [%s]", login), e);
        }
        if (actor == null) {
            String name = String.format("Wiki %s", login);
            actor = this.createActor(new Service(), name, null, login);
        }
        return actor;
    }

    private <T extends AbstractActor> T createActor(T actor, String fullName, String username,
        String wikiName)
        throws ActivityPubException
    {
        actor.setName(fullName);

        actor.setPreferredUsername(this.activityPubIdentifierService.createIdentifier(actor, username, wikiName));

        Inbox inbox = new Inbox();
        inbox.setAttributedTo(
            Collections.singletonList(new ActivityPubObjectReference<AbstractActor>().setObject(actor)));
        this.activityPubStorage.storeEntity(inbox);
        actor.setInbox(new ActivityPubObjectReference<Inbox>().setObject(inbox));

        Outbox outbox = new Outbox();
        outbox.setAttributedTo(
            Collections.singletonList(new ActivityPubObjectReference<AbstractActor>().setObject(actor)));
        this.activityPubStorage.storeEntity(outbox);
        actor.setOutbox(new ActivityPubObjectReference<Outbox>().setObject(outbox));

        OrderedCollection<AbstractActor> following = new OrderedCollection<>();
        this.activityPubStorage.storeEntity(following);
        actor.setFollowing(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(following));

        OrderedCollection<AbstractActor> followers = new OrderedCollection<>();
        this.activityPubStorage.storeEntity(followers);
        actor.setFollowers(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(followers));

        // We need to store first the actor, so we set the ID which is used for the publickey
        // TODO: this would need to be improved, for example by directly storing the publickey in the DB.
        this.activityPubStorage.storeEntity(actor);

        PublicKey publicKey = this.initPublicKey(actor);
        actor.setPublicKey(publicKey);

        this.activityPubStorage.storeEntity(actor);
        return actor;
    }

    @Override
    public UserReference getXWikiUserReference(Person actor) throws ActivityPubException
    {
        if (actor == null) {
            throw new ActivityPubException("Cannot find user reference from actor, actor is null");
        }

        return this.getUserReference(actor);
    }

    private UserReference getUserReference(Person actor)
    {
        String userName = actor.getPreferredUsername().split(WEBFINGER_SEPARATOR)[0];
        if (this.isLocalActor(actor)) {
            return this.xWikiUserBridge.resolveUser(userName);
        } else {
            return null;
        }
    }

    @Override
    public WikiReference getXWikiWikiReference(Service actor)
    {
        return new WikiReference(actor.getPreferredUsername());
    }

    @Override
    public boolean isExistingUser(String username)
    {
        return this.xWikiUserBridge.isExistingUser(username);
    }

    @Override
    public boolean isLocalActor(AbstractActor actor)
    {
        if (actor.getId() != null) {
            return this.defaultURLHandler.belongsToCurrentInstance(actor.getId());
        } else {
            String userName = actor.getPreferredUsername().split(WEBFINGER_SEPARATOR)[0];
            return this.isExistingUser(userName);
        }
    }

    @Override
    public boolean isAuthorizedToActFor(UserReference authenticatedUser, AbstractActor targetActor)
        throws ActivityPubException
    {
        boolean result = false;

        // We only allow authenticated users.
        if (authenticatedUser != null) {
            // Someone's trying to post in the Outbox of a person: it needs to be the same person.
            if (targetActor instanceof Person) {
                UserReference xWikiUserReference = this.getXWikiUserReference((Person) targetActor);
                result = authenticatedUser.equals(xWikiUserReference);

                // Someone's trying to post in the Outbox of an entire Wiki: the person needs to belongs to
                // the group that manage the wiki
            } else if (targetActor instanceof Service) {
                XWikiContext xWikiContext = this.contextProvider.get();
                DocumentReference userDocumentReference = this.xWikiUserBridge.getDocumentReference(authenticatedUser);
                DocumentReference wikiGroup = this.activityPubConfiguration.getWikiGroup();
                try {
                    Collection<DocumentReference> groups =
                        this.groupManager.getGroups(userDocumentReference, xWikiContext.getWikiReference(), true);
                    result = groups.contains(wikiGroup);
                } catch (GroupException e) {
                    throw new ActivityPubException(
                        String.format("Error while looking for groups for [%s].", userDocumentReference), e);
                }
            }
        }
        return result;
    }

    @Override
    public String getNotificationTarget(AbstractActor targetedActor) throws ActivityPubException
    {
        String result;
        if (targetedActor instanceof Person) {
            UserReference xWikiUserReference = this.getXWikiUserReference((Person) targetedActor);
            result = this.userReferenceSerializer.serialize(xWikiUserReference);
        } else if (targetedActor instanceof Service) {
            result = this.entityReferenceSerializer.serialize(this.activityPubConfiguration.getWikiGroup());
        } else {
            throw new ActivityPubException(String.format(STANDARD_ERROR_MESSAGE, targetedActor.getType()));
        }

        return result;
    }

    @Override
    public DocumentReference getStoreDocument(AbstractActor actor) throws ActivityPubException
    {
        DocumentReference documentReference;
        if (actor instanceof Person) {
            UserReference userReference = this.getXWikiUserReference((Person) actor);
            documentReference = this.xWikiUserBridge.getDocumentReference(userReference);
        } else if (actor instanceof Service) {
            documentReference = new DocumentReference(actor.getPreferredUsername(), SERVICE_USER_SPACE_REFERENCE,
                actor.getPreferredUsername());
        } else {
            throw new ActivityPubException(String.format(STANDARD_ERROR_MESSAGE, actor.getType()));
        }
        return documentReference;
    }

    @Override
    public AbstractActor getActor(ActivityPubResourceReference resourceReference) throws ActivityPubException
    {
        String entityType = resourceReference.getEntityType();
        String uid = resourceReference.getUuid();
        if ("person".equalsIgnoreCase(entityType)) {
            UserReference userReference = this.xWikiUserBridge.resolveUser(uid);
            return this.getActor(userReference);
        } else if ("service".equalsIgnoreCase(entityType)) {
            return this.getActor(new WikiReference(uid));
        } else {
            throw new ActivityPubException(String.format(STANDARD_ERROR_MESSAGE, entityType));
        }
    }

    @Override
    public AbstractActor getActor(String username) throws ActivityPubException
    {
        AbstractActor ret = null;

        try {
            URI actorURI = new URI(username);

            // A URI with a scheme (absolute) or containing a @ cannot be an XWiki identifier
            if (actorURI.isAbsolute() || username.contains(WEBFINGER_SEPARATOR)) {
                // FIXME: we need to ensure to discard remote Actor info after some time for it to be efficient.
                ret = this.activityPubStorage.retrieveEntity(actorURI);
                boolean isAlreadyStored = (ret != null);

                // if it is not stored, try to resolve it as a WebFinger
                if (ret == null) {
                    ret = this.getWebfingerActor(username);
                }

                // if WebFinger didn't worked, try to resolve it as AP URL
                if (ret == null && actorURI.isAbsolute()) {
                    ret = this.getRemoteActor(actorURI);
                }

                // if AP URL didn't work, try it as an XWiki URL
                if (ret == null && actorURI.isAbsolute()) {
                    URI xWikiActorURI = this.resolveXWikiActorURL(actorURI.toURL());
                    if (xWikiActorURI != null) {
                        ret = this.getRemoteActor(xWikiActorURI);
                    }
                }

                // if we managed to find an actor and it wasn't stored yet we store it for speeding further resolutions.
                if (ret != null && !isAlreadyStored) {
                    this.activityPubStorage.storeEntity(ret);
                }
            } else {
                // the given argument might be an XWiki identifier
                try {
                    ret = this.getActor(this.xWikiUserBridge.resolveUser(username));
                } catch (ActivityPubException ex) {
                    this.logger.warn("Cannot find the asked user [{}].", username, ex);
                }
            }

        } catch (URISyntaxException | IOException e) {
            throw new ActivityPubException(
                String.format("Error while loading information for actor [%s].", username), e);
        }
        return ret;
    }

    private AbstractActor getWebfingerActor(String webfingerResource) throws IOException
    {
        try {
            JSONResourceDescriptor jrd = this.webfingerClient.get(webfingerResource);
            URI href =
                jrd.getLinks().stream().filter(it -> Objects.equals(it.getRel(), "self")).findFirst().map(Link::getHref)
                    .orElse(null);
            return this.getRemoteActor(href);
        } catch (WebfingerException e) {
            logger.debug("Error when querying the WebFinger resource from [{}].", webfingerResource, e);
        }
        return null;
    }

    /**
     * Implements {@see getRemoteActor} but tries to use actorURL as a XWiki url profile URL if the standard activitypub
     * request fails. 
     *
     * @param actorURI the URI of the remote actor.
     * @return an instance of the actor.
     * @throws IOException in case of error while loading and parsing the request.
     */
    private AbstractActor getRemoteActor(URI actorURI) throws IOException
    {
        HttpMethod httpMethod = this.activityPubClient.get(actorURI);
        try {
            this.activityPubClient.checkAnswer(httpMethod);
            return this.jsonParser.parse(httpMethod.getResponseBodyAsStream());
        } catch (ActivityPubException e) {
            this.logger.debug("Error when querying the ActivityPub actor from [{}].", actorURI, e);
        } finally {
            httpMethod.releaseConnection();
        }
        return null;
    }

    /**
     * Resolve the activity pub endpoint of an user from it's XWiki profile url.
     *
     * @param xWikiActorURL The user's XWiki profile url.
     * @return The activitpub endpoint of the user.
     */
    private URI resolveXWikiActorURL(URL xWikiActorURL) throws IOException, URISyntaxException
    {
        Document doc = getJsoupConnection().url(xWikiActorURL).get();
        String userName = doc.selectFirst("html").attr("data-xwiki-document");
        if (StringUtils.isNotBlank(userName)) {
            return new URI(String.format("%s://%s/xwiki/activitypub/Person/%s",
                xWikiActorURL.getProtocol(), xWikiActorURL.getAuthority(), userName));
        } else {
            return null;
        }
    }

    @Override
    public PublicKey initPublicKey(AbstractActor actor) throws ActivityPubException
    {
        String pubKey = this.signatureService.getPublicKeyPEM(actor);

        String idStr = actor.getId().toASCIIString();
        return new PublicKey()
                .setId(idStr + "#main-key")
                .setOwner(idStr)
                .setPublicKeyPem(pubKey);
    }
}
