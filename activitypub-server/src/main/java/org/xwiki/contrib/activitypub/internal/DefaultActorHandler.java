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
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.user.User;
import org.xwiki.user.UserReference;

/**
 * Link an ActivityPub actor to an XWiki User and retrieves the inbox/outbox of users.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActorHandler implements ActorHandler
{
    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private ActivityPubClient activityPubClient;

    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private ActivityPubJsonParser jsonParser;

    @Inject
    private DefaultURLHandler defaultURLHandler;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> resourceReferenceSerializer;

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
    public AbstractActor getCurrentActor() throws ActivityPubException
    {
        return getActor(UserReference.CURRENT_USER_REFERENCE);
    }

    @Override
    public AbstractActor getActor(UserReference userReference) throws ActivityPubException
    {
        User user = this.xWikiUserBridge.resolveUser(userReference);

        if (user != null) {
            String login = this.xWikiUserBridge.getUserLogin(user);
            AbstractActor actor = this.activityPubStorage.retrieveEntity(login);
            if (actor == null) {
                String fullname = String.format("%s %s", user.getFirstName(), user.getLastName());
                actor = createActor(fullname, login);
            }
            return actor;
        } else {
            throw new ActivityPubException(
                String.format("Cannot find any user with reference [%s]", userReference));
        }
    }

    private AbstractActor createActor(String fullName, String login) throws ActivityPubException
    {
        AbstractActor actor = new Person();
        actor.setName(fullName);
        actor.setPreferredUsername(login);

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

        OrderedCollection<AbstractActor> following = new OrderedCollection<AbstractActor>();
        this.activityPubStorage.storeEntity(following);
        actor.setFollowing(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(following));

        OrderedCollection<AbstractActor> followers = new OrderedCollection<AbstractActor>();
        this.activityPubStorage.storeEntity(followers);
        actor.setFollowers(new ActivityPubObjectReference<OrderedCollection<AbstractActor>>().setObject(followers));

        this.activityPubStorage.storeEntity(actor);

        return actor;
    }

    @Override
    public UserReference getXWikiUserReference(AbstractActor actor) throws ActivityPubException
    {
        if (actor == null) {
            throw new ActivityPubException("Cannot find user reference from actor, actor is null");
        }
        String userName = actor.getPreferredUsername();
        if (isLocalActor(actor) && isExistingUser(userName)) {
            return this.xWikiUserBridge.resolveUser(userName);
        } else {
            return null;
        }
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
            return this.activityPubStorage.belongsToCurrentInstance(actor.getId());
        } else {
            String userName = actor.getPreferredUsername();
            return isExistingUser(userName);
        }
    }

    @Override
    public AbstractActor getLocalActor(String username) throws ActivityPubException
    {
        return this.getActor(this.xWikiUserBridge.resolveUser(username));
    }

    @Override
    public AbstractActor getRemoteActor(String actorURL) throws ActivityPubException
    {
        return getRemoteActor(actorURL, true);
    }

    /**
     * Implements {@see getRemoteActor} but tries to use actorURL as a XWiki url profile URL if the standard activitypub
     * request fails. 
     *
     * @param actorURL the URL of the remote actor.
     * @param fallback Specify if the url should be tried an a XWiki user profile URL in case of failure.
     * @return an instance of the actor.
     * @throws ActivityPubException in case of error while loading and parsing the request.
     */
    private AbstractActor getRemoteActor(String actorURL, boolean fallback) throws ActivityPubException
    {
        try {
            URI uri = new URI(actorURL);
            HttpMethod httpMethod = this.activityPubClient.get(uri);
            try {
                this.activityPubClient.checkAnswer(httpMethod);
                return this.jsonParser.parse(httpMethod.getResponseBodyAsStream());
            } finally {
                httpMethod.releaseConnection();
            }
        } catch (ActivityPubException | URISyntaxException | IOException e) {
            if (fallback) {
                String xWikiActorURL = this.resolveXWikiActorURL(actorURL);
                return this.getRemoteActor(xWikiActorURL, false);
            } else {
                throw new ActivityPubException(
                    String.format("Error when trying to retrieve the remote actor from [%s]", actorURL), e);
            }
        }
    }

    /**
     * Resolve the activity pub endpoint of an user from it's XWiki profile url.
     *
     * @param xWikiActorURL The user's XWiki profile url.
     * @return The activitpub endpoint of the user.
     * @throws ActivityPubException In case of error during the query to the user profile url.
     */
    private String resolveXWikiActorURL(String xWikiActorURL) throws ActivityPubException
    {
        try {
            Document doc = getJsoupConnection().url(xWikiActorURL).get();
            String userName = doc.selectFirst("html").attr("data-xwiki-document");
            ActivityPubResourceReference userResourceReference = new ActivityPubResourceReference("person", userName);
            URI serializedUser = this.resourceReferenceSerializer.serialize(userResourceReference);
            return this.defaultURLHandler.getAbsoluteURI(serializedUser).toASCIIString();
        } catch (Exception e) {
            throw new ActivityPubException(
                String.format("Error when trying to resolve the XWiki actor from [%s]", xWikiActorURL), e);
        }
    }
}
