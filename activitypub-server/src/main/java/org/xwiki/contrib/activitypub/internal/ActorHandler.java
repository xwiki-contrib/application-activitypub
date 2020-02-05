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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubStore;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.ObjectReference;
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
    private Provider<XWikiContext> contextProvider;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    private URI getID(XWikiUser user)
    {
        return null;
    }

    public Actor getActor(EntityReference entityReference)
    {
        // TODO: introduce cache mechanism
        try {
            XWikiDocument document = (XWikiDocument) this.documentAccess.getDocumentInstance(entityReference);
            BaseObject userXObject = document.getXObject(USER_CLASS_REFERENCE);
            if (userXObject != null) {
                XWikiUser xWikiUser = new XWikiUser(new DocumentReference(entityReference));
                Actor actor = new Person();
                actor.setPreferredUsername(xWikiUser.getFullName());
                actor.setName(userXObject.get("first_name") + " " + userXObject.get("last_name"));
                actor.setId(getID(xWikiUser));
                return actor;
            }
        } catch (Exception e) {
            logger.error("Error while loading user document with reference [{}]", entityReference, e);
        }
        return null;
    }

    private DocumentReference resolveUser(String username)
    {
        DocumentReference userDocReference =
            this.stringDocumentReferenceResolver.resolve(String.format("XWiki.%s", username));
        return userDocReference;
    }

    public boolean isExistingUser(String serializedUserReference)
    {
        XWikiUser xWikiUser = new XWikiUser(resolveUser(serializedUserReference));
        return xWikiUser.exists(this.contextProvider.get());
    }

    public Actor getActor(String serializedUserReference)
    {
        return this.getActor(resolveUser(serializedUserReference));
    }

    public Inbox getActorInbox(Actor actor)
    {
        Inbox inbox = this.activityPubStorage.retrieveActorInbox(actor);
        if (inbox == null) {
            inbox = new Inbox();
            inbox.getAttributedTo().add(new ObjectReference<Actor>().setObject(actor));
            this.activityPubStorage.storeEntity(inbox);
        }
        return inbox;
    }

    public Outbox getActorOutbox(Actor actor)
    {
        Outbox outbox = this.activityPubStorage.retrieveActorOutbox(actor);
        if (outbox == null) {
            outbox = new Outbox();
            outbox.getAttributedTo().add(new ObjectReference<Actor>().setObject(actor));
            this.activityPubStorage.storeEntity(outbox);
        }
        return outbox;
    }
}
