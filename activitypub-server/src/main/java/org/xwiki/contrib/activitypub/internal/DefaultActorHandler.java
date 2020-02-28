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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;
import org.jsoup.nodes.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Link an ActivityPub actor to an XWiki User and retrieves the inbox/outbox of users.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActorHandler implements ActorHandler
{
    private static final LocalDocumentReference USER_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private DocumentAccessBridge documentAccess;

    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ActivityPubClient activityPubClient;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private ActivityPubJsonParser jsonParser;

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
        DocumentReference userReference = this.contextProvider.get().getUserReference();
        if (userReference == null) {
            throw new ActivityPubException("The context does not have any user reference, "
                + "the request might be anonymous.");
        } else {
            return getActor(userReference);
        }
    }

    @Override
    public AbstractActor getActor(EntityReference entityReference) throws ActivityPubException
    {
        // TODO: introduce cache mechanism
        XWikiDocument document;
        try {
            document = (XWikiDocument) this.documentAccess.getDocumentInstance(entityReference);
        } catch (Exception e) {
            throw new ActivityPubException(
                String.format("Error while loading user document with reference [%s]", entityReference), e);
        }
        if (document == null) {
            throw new ActivityPubException(
                String.format("Cannot load document with reference [%s]", entityReference));
        }
        BaseObject userXObject = document.getXObject(USER_CLASS_REFERENCE);
        if (userXObject != null) {
            XWikiUser xWikiUser = new XWikiUser(new DocumentReference(entityReference));
            String login = xWikiUser.getFullName();
            AbstractActor actor = this.activityPubStorage.retrieveEntity(login);
            if (actor == null) {
                String fullname = String.format("%s %s",
                    userXObject.getStringValue("first_name"), userXObject.getStringValue("last_name"));
                actor = createActor(fullname, login);
            }
            return actor;
        } else {
            throw new ActivityPubException(
                String.format("Cannot find any user in document [%s]", document.getDocumentReference()));
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
    public EntityReference getXWikiUserReference(AbstractActor actor)
    {
        String userName = actor.getPreferredUsername();
        if (isLocalActor(actor) && isExistingUser(userName)) {
            return resolveUser(userName);
        } else {
            return null;
        }
    }

    private DocumentReference resolveUser(String username)
    {
        String serializedRef = username;
        if (!username.contains(".")) {
            serializedRef = String.format("XWiki.%s", username);
        }

        return this.stringDocumentReferenceResolver.resolve(serializedRef);
    }

    @Override
    public boolean isExistingUser(String serializedUserReference)
    {
        XWikiUser xWikiUser = new XWikiUser(resolveUser(serializedUserReference));
        return xWikiUser.exists(this.contextProvider.get());
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
    public AbstractActor getLocalActor(String serializedUserReference) throws ActivityPubException
    {
        return this.getActor(resolveUser(serializedUserReference));
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
            URI uri = new URI(xWikiActorURL);
            return String.format("%s://%s/xwiki/activitypub/Person/%s", uri.getScheme(), uri.getAuthority(), userName);
        } catch (IOException | URISyntaxException e) {
            throw new ActivityPubException(
                String.format("Error when trying to resolve the XWiki actor from [%s]", xWikiActorURL), e);
        }
    }
}
