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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link ActivityPubStorage}.
 * Only keep the information in memory for now.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubStorage implements ActivityPubStorage
{
    private static final String INBOX_SUFFIX_ID = "inbox";
    private static final String OUTBOX_SUFFIX_ID = "outbox";
    private final Map<String, String> storage;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Inject
    @Named("activitypub")
    private ResourceReferenceResolver<ExtendedURL> urlResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ActivityPubJsonParser jsonParser;

    @Inject
    private ActivityPubJsonSerializer jsonSerializer;

    @Inject
    private ActivityPubObjectReferenceResolver resolver;

    @Inject
    private Logger logger;

    private URL serverUrl;

    /**
     * Default constructor.
     */
    public DefaultActivityPubStorage()
    {
        this.storage = new HashMap<>();
    }

    private URL getServerUrl() throws MalformedURLException
    {
        if (this.serverUrl == null) {
            XWikiContext context = this.contextProvider.get();
            this.serverUrl = context.getURLFactory().getServerURL(context);
        }
        return this.serverUrl;
    }

    private boolean isIdFromCurrentInstance(URI id)
    {
        // FIXME: This should definitely be computed in a better way
        try {
            return id.toURL().toString().contains(getServerUrl().toString());
        } catch (MalformedURLException e) {
            logger.error("Error while comparing server URL and actor ID", e);
        }
        return false;
    }

    @Override
    public String storeEntity(ActivityPubObject entity) throws ActivityPubException
    {
        if (entity.getId() != null && isIdFromCurrentInstance(entity.getId())) {
            this.storeEntity(entity.getId(), entity);
        } else if (entity.getId() != null && !isIdFromCurrentInstance(entity.getId())) {
            throw new ActivityPubException(
                String.format("Entity [%s] won't be stored since it's not part of the current instance", entity.getId())
            );
        }

        String uuid;
        if (entity instanceof Inbox) {
            Inbox inbox = (Inbox) entity;
            if (inbox.getAttributedTo() == null || inbox.getAttributedTo().isEmpty()) {
                throw new ActivityPubException("Cannot store an inbox without owner.");
            }
            AbstractActor owner = this.resolver.resolveReference(inbox.getAttributedTo().get(0));
            uuid = getActorEntityUID(owner, INBOX_SUFFIX_ID);
        } else if (entity instanceof Outbox) {
            Outbox outbox = (Outbox) entity;
            if (outbox.getAttributedTo() == null || outbox.getAttributedTo().isEmpty()) {
                throw new ActivityPubException("Cannot store an outbox without owner.");
            }
            AbstractActor owner = this.resolver.resolveReference(outbox.getAttributedTo().get(0));
            uuid = getActorEntityUID(owner, OUTBOX_SUFFIX_ID);
        } else if (entity instanceof AbstractActor) {
            uuid = ((AbstractActor) entity).getPreferredUsername();
        } else {
            // FIXME: we cannot rely on hashCode because of possible collisions and size limitation, but we shouldn't
            // rely on total randomness because of dedup.
            uuid = UUID.randomUUID().toString();
        }
        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference(entity.getType(), uuid);
        try {
            entity.setId(this.serializer.serialize(resourceReference));
            this.storage.put(uuid, this.jsonSerializer.serialize(entity));
            return uuid;
        } catch (SerializeResourceReferenceException | UnsupportedResourceReferenceException e) {
            throw new ActivityPubException(String.format("Error while storing [%s].", resourceReference), e);
        }
    }

    @Override
    public boolean storeEntity(URI uri, ActivityPubObject entity) throws ActivityPubException
    {
        try {
            // FIXME: the prefix should be at the very least dynamically computed.
            //  But maybe we should use a URI resolver.
            ExtendedURL extendedURL = new ExtendedURL(uri.toURL(), "xwiki/activitypub");
            ActivityPubResourceReference resourceReference = (ActivityPubResourceReference) this.urlResolver.
                resolve(extendedURL, new ResourceType("activitypub"), Collections.emptyMap());
            return this.storeEntity(resourceReference.getUuid(), entity);
        } catch (MalformedURLException | CreateResourceReferenceException | UnsupportedResourceReferenceException e) {
            throw new ActivityPubException(String.format("Error when getting UID from URI [%s]", uri), e);
        }
    }

    @Override
    public boolean storeEntity(String uid, ActivityPubObject entity) throws ActivityPubException
    {
        if (StringUtils.isEmpty(uid)) {
            throw new ActivityPubException("The UID cannot be empty.");
        }
        boolean result = !this.storage.containsKey(uid);
        this.storage.put(uid, this.jsonSerializer.serialize(entity));
        return result;
    }

    @Override
    public <T extends ActivityPubObject> T retrieveEntity(String uuid) throws ActivityPubException
    {
        if (this.storage.containsKey(uuid)) {
            return (T) this.jsonParser.parse(this.storage.get(uuid));
        } else {
            return null;
        }
    }

    private String getActorEntityUID(AbstractActor actor, String entitySuffix)
    {
        return String.format("%s-%s", actor.getPreferredUsername(), entitySuffix);
    }
}
