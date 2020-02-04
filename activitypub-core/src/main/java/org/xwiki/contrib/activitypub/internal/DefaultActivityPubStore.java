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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubStore;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Object;
import org.xwiki.contrib.activitypub.entities.Outbox;

@Component
@Singleton
public class DefaultActivityPubStore implements ActivityPubStore
{
    private static final String INBOX_SUFFIX_ID = "inbox";
    private static final String OUTBOX_SUFFIX_ID = "outbox";
    private final Map<String, Object> storage;

    public DefaultActivityPubStore()
    {
        this.storage = new HashMap<>();
    }

    @Override
    public String storeEntity(Object entity)
    {
        if (entity.getId() != null) {
            throw new IllegalArgumentException("Entity with existing ID Not yet implemented");
        }

        String uuid;
        if ("inbox".equalsIgnoreCase(entity.getType())) {
            uuid = getActorEntityUID(entity.getAttributedTo().get(0).getObject(), INBOX_SUFFIX_ID);
        } else if ("outbox".equalsIgnoreCase(entity.getType())) {
            uuid = getActorEntityUID(entity.getAttributedTo().get(0).getObject(), OUTBOX_SUFFIX_ID);
        } else {
            // FIXME: we cannot rely on hashCode because of possible collisions and size limitation, but we shouldn't
            // rely on total randomness because of dedup.
            uuid = UUID.randomUUID().toString();
        }
        this.storage.put(uuid, entity);
        return uuid;
    }

    @Override
    public <T extends Object> T retrieveEntity(String uuid)
    {
        return (T) this.storage.get(uuid);
    }

    @Override
    public Inbox retrieveActorInbox(Actor actor)
    {
        return this.retrieveEntity(getActorEntityUID(actor, INBOX_SUFFIX_ID));
    }

    @Override
    public Outbox retrieveActorOutbox(Actor actor)
    {
        return this.retrieveEntity(getActorEntityUID(actor, OUTBOX_SUFFIX_ID));
    }

    private String getActorEntityUID(Actor actor, String entitySuffix)
    {
        return String.format("%s-%s", actor.getId(), entitySuffix);
    }
}
