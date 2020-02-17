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
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Actor;
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
    private Logger logger;

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

    @Override
    public Actor getCurrentActor() throws ActivityPubException
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
    public Actor getActor(EntityReference entityReference) throws ActivityPubException
    {
        // TODO: introduce cache mechanism
        try {
            XWikiDocument document = (XWikiDocument) this.documentAccess.getDocumentInstance(entityReference);
            BaseObject userXObject = document.getXObject(USER_CLASS_REFERENCE);
            if (userXObject != null) {
                XWikiUser xWikiUser = new XWikiUser(new DocumentReference(entityReference));
                String login = xWikiUser.getFullName();
                Actor actor = this.activityPubStorage.retrieveEntity("actor", login);
                if (actor == null) {
                    String fullname = String.format("%s %s",
                        userXObject.getStringValue("first_name"), userXObject.getStringValue("last_name"));
                    actor = createActor(fullname, login);
                }
                return actor;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new ActivityPubException(
                String.format("Error while loading user document with reference [%s]", entityReference), e);
        }
    }

    private Actor createActor(String fullName, String preferredName) throws ActivityPubException
    {
        Actor actor = new Person();
        actor.setName(fullName);
        actor.setPreferredUsername(preferredName);

        Inbox inbox = new Inbox();
        inbox.setAttributedTo(Collections.singletonList(new ActivityPubObjectReference<Actor>().setObject(actor)));
        this.activityPubStorage.storeEntity(inbox);
        actor.setInbox(new ActivityPubObjectReference<Inbox>().setObject(inbox));

        Outbox outbox = new Outbox();
        outbox.setAttributedTo(Collections.singletonList(new ActivityPubObjectReference<Actor>().setObject(actor)));
        this.activityPubStorage.storeEntity(outbox);
        actor.setOutbox(new ActivityPubObjectReference<Outbox>().setObject(outbox));

        OrderedCollection<Actor> following = new OrderedCollection<Actor>();
        this.activityPubStorage.storeEntity(following);
        actor.setFollowing(new ActivityPubObjectReference<OrderedCollection<Actor>>().setObject(following));

        OrderedCollection<Actor> followers = new OrderedCollection<Actor>();
        this.activityPubStorage.storeEntity(followers);
        actor.setFollowers(new ActivityPubObjectReference<OrderedCollection<Actor>>().setObject(followers));

        this.activityPubStorage.storeEntity(actor);

        return actor;
    }

    @Override
    public EntityReference getXWikiUserReference(Actor actor)
    {
        String userName = actor.getPreferredUsername();
        if (isExistingUser(userName)) {
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
        DocumentReference userDocReference =
            this.stringDocumentReferenceResolver.resolve(serializedRef);
        return userDocReference;
    }

    @Override
    public boolean isExistingUser(String serializedUserReference)
    {
        XWikiUser xWikiUser = new XWikiUser(resolveUser(serializedUserReference));
        return xWikiUser.exists(this.contextProvider.get());
    }

    @Override
    public Actor getLocalActor(String serializedUserReference) throws ActivityPubException
    {
        return this.getActor(resolveUser(serializedUserReference));
    }

    @Override
    public Actor getRemoteActor(String actorURL) throws ActivityPubException
    {
        try {
            URI uri = new URI(actorURL);
            HttpMethod httpMethod = this.activityPubClient.get(uri);
            this.activityPubClient.checkAnswer(httpMethod);
            return this.jsonParser.parse(httpMethod.getResponseBodyAsStream());
        } catch (ActivityPubException | URISyntaxException | IOException e) {
            throw new ActivityPubException(
                String.format("Error when trying to retrieve the remote actor from [%s]", actorURL), e);
        }
    }

    @Override
    public Inbox getInbox(Actor actor) throws ActivityPubException
    {
        Inbox inbox = this.activityPubObjectReferenceResolver.resolveReference(actor.getInbox());
        inbox.setAttributedTo(Collections.singletonList(new ActivityPubObjectReference<Actor>().setObject(actor)));
        return inbox;
    }

    @Override
    public Outbox getOutbox(Actor actor) throws ActivityPubException
    {
        Outbox outbox = this.activityPubObjectReferenceResolver.resolveReference(actor.getOutbox());
        outbox.setAttributedTo(Collections.singletonList(new ActivityPubObjectReference<Actor>().setObject(actor)));
        return outbox;
    }
}
