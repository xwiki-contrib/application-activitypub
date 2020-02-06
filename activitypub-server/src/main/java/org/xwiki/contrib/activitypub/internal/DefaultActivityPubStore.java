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
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStore;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;

@Component
@Singleton
public class DefaultActivityPubStore implements ActivityPubStore
{
    private static final String INBOX_SUFFIX_ID = "inbox";
    private static final String OUTBOX_SUFFIX_ID = "outbox";
    private final Map<String, ActivityPubObject> storage;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Inject
    private Logger logger;

    public DefaultActivityPubStore()
    {
        this.storage = new HashMap<>();
    }

    @Override
    public String storeEntity(ActivityPubObject entity)
    {
        if (entity.getId() != null) {
            throw new IllegalArgumentException("Entity with existing ID Not yet implemented");
        }

        String uuid;
        if (entity instanceof Inbox) {
            Inbox inbox = (Inbox) entity;
            if (inbox.getOwner() == null) {
                throw new IllegalArgumentException("Cannot store an inbox without owner.");
            }
            uuid = getActorEntityUID(inbox.getOwner(), INBOX_SUFFIX_ID);
        } else if (entity instanceof Outbox) {
            Outbox outbox = (Outbox) entity;
            if (outbox.getOwner() == null) {
                throw new IllegalArgumentException("Cannot store an outbox without owner.");
            }
            uuid = getActorEntityUID(outbox.getOwner(), OUTBOX_SUFFIX_ID);
        } else if (entity instanceof Actor) {
            uuid = entity.getName();
        } else {
            // FIXME: we cannot rely on hashCode because of possible collisions and size limitation, but we shouldn't
            // rely on total randomness because of dedup.
            uuid = UUID.randomUUID().toString();
        }
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference(entity.getType(), uuid);
        try {
            entity.setId(this.serializer.serialize(resourceReference));
            this.storage.put(uuid, entity);
            return uuid;
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            this.logger.error("Error while serializing [{}].", resourceReference, e);
        }

        return null;
    }

    @Override
    public boolean storeEntity(String uid, ActivityPubObject entity)
    {
        boolean result = !this.storage.containsKey(uid);
        this.storage.put(uid, entity);
        return result;
    }

    @Override
    public <T extends ActivityPubObject> T retrieveEntity(String entityType, String uuid)
    {
        String key = uuid;
        if ("inbox".equalsIgnoreCase(entityType)) {
            key = String.format("%s-%s", uuid, INBOX_SUFFIX_ID);
        } else if ("outbox".equalsIgnoreCase(entityType)) {
            key = String.format("%s-%s", uuid, OUTBOX_SUFFIX_ID);
        }
        return (T) this.storage.get(key);
    }

    private String getActorEntityUID(Actor actor, String entitySuffix)
    {
        return String.format("%s-%s", actor.getName(), entitySuffix);
    }
}
