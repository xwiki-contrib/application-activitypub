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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStore;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
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
@Component(roles = ActorHandler.class)
@Singleton
public class ActorHandler
{
    private static final LocalDocumentReference USER_CLASS_REFERENCE =
        new LocalDocumentReference("XWiki", "XWikiUsers");

    @Inject
    private DocumentAccessBridge documentAccess;

    @Inject
    private Logger logger;

    @Inject
    private ActivityPubStore activityPubStorage;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    public Actor getActor(EntityReference entityReference) throws ActivityPubException
    {
        // TODO: introduce cache mechanism
        try {
            XWikiDocument document = (XWikiDocument) this.documentAccess.getDocumentInstance(entityReference);
            BaseObject userXObject = document.getXObject(USER_CLASS_REFERENCE);
            if (userXObject != null) {
                XWikiUser xWikiUser = new XWikiUser(new DocumentReference(entityReference));
                String fullname = xWikiUser.getFullName();
                Actor actor = this.activityPubStorage.retrieveEntity("actor", fullname);
                if (actor == null) {
                    String preferredName = String.format("%s %s",
                        userXObject.getStringValue("first_name"), userXObject.getStringValue("last_name"));
                    actor = createActor(fullname, preferredName);
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
        inbox.setOwner(actor);
        this.activityPubStorage.storeEntity(inbox);
        actor.setInbox(new ActivityPubObjectReference<Inbox>().setObject(inbox));

        Outbox outbox = new Outbox();
        outbox.setOwner(actor);
        this.activityPubStorage.storeEntity(outbox);
        actor.setOutbox(new ActivityPubObjectReference<Outbox>().setObject(outbox));

        OrderedCollection following = new OrderedCollection();
        this.activityPubStorage.storeEntity(following);
        actor.setFollowing(new ActivityPubObjectReference<OrderedCollection>().setObject(following));

        OrderedCollection followers = new OrderedCollection();
        this.activityPubStorage.storeEntity(followers);
        actor.setFollowers(new ActivityPubObjectReference<OrderedCollection>().setObject(followers));

        this.activityPubStorage.storeEntity(actor);

        return actor;
    }

    public EntityReference getXWikiUserReference(Actor actor)
    {
        String userName = actor.getName();
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

    public boolean isExistingUser(String serializedUserReference)
    {
        XWikiUser xWikiUser = new XWikiUser(resolveUser(serializedUserReference));
        return xWikiUser.exists(this.contextProvider.get());
    }

    public Actor getActor(String serializedUserReference) throws ActivityPubException
    {
        return this.getActor(resolveUser(serializedUserReference));
    }

    public boolean isActorFromCurrentInstance(Actor actor)
    {
        XWikiContext context = this.contextProvider.get();
        try {
            URL serverURL = context.getURLFactory().getServerURL(context);
            return (!actor.getId().relativize(serverURL.toURI()).toASCIIString().isEmpty());
        } catch (MalformedURLException | URISyntaxException e) {
            logger.error("Error while comparing server URL and actor ID", e);
        }
        return false;
    }
}
