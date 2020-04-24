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
package org.xwiki.contrib.activitypub.internal.storage;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.time.DateUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Inbox;
import org.xwiki.contrib.activitypub.entities.Outbox;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.InternalURINormalizer;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonParser;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.SolrUtils;

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
    private static final String WEBFINGER_TYPE = "webfinger";
    private static final String DEFAULT_QUERY = "*";
    private static final String ACTIVITYPUB = "activitypub";

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Inject
    @Named("relative")
    private ActivityPubJsonParser jsonParser;

    @Inject
    @Named("relative")
    private ActivityPubJsonSerializer jsonSerializer;

    @Inject
    private ActivityPubObjectReferenceResolver resolver;

    @Inject
    private WebfingerJsonSerializer webfingerJsonSerializer;

    @Inject
    private WebfingerJsonParser webfingerJsonParser;

    @Inject
    private Logger logger;

    @Inject
    private DefaultURLHandler urlHandler;

    @Inject
    private InternalURINormalizer internalURINormalizer;

    @Inject
    private SolrUtils solrUtils;

    @Inject
    private Solr solr;

    private SolrClient getSolrClient() throws SolrException
    {
        return this.solr.getClient(ACTIVITYPUB);
    }

    @Override
    public boolean isStorageReady()
    {
        try {
            return getSolrClient() != null;
        } catch (SolrException e) {
            logger.debug("Error while initializing solr client.", e);
            return false;
        }
    }

    private void storeInformation(ActivityPubObject entity, String id)
        throws ActivityPubException, SolrException, IOException, SolrServerException
    {
        SolrInputDocument inputDocument = new SolrInputDocument();
        inputDocument.addField(ID_FIELD, id);
        inputDocument.addField(TYPE_FIELD, entity.getType());
        inputDocument.addField(CONTENT_FIELD, this.jsonSerializer.serialize(entity));
        inputDocument.addField(UPDATED_DATE_FIELD, new Date());
        inputDocument.addField(XWIKI_REFERENCE_FIELD, entity.getXwikiReference());
        inputDocument.addField(IS_PUBLIC_FIELD, entity.isPublic());
        this.solrUtils.set(AUTHORS_FIELD, Objects.isNull(entity.getAttributedTo()) ? Collections.emptyList()
            : entity.getAttributedTo().stream()
            .map(ActivityPubObjectReference::getLink)
            .map(this.internalURINormalizer::relativizeURI)
            .map(URI::toASCIIString)
            .collect(Collectors.toSet()), inputDocument);
        this.solrUtils.set(TARGETED_FIELD, this.resolver.resolveTargets(entity).stream()
            .map(ActivityPubObject::getId)
            .map(this.internalURINormalizer::relativizeURI)
            .map(URI::toASCIIString)
            .collect(Collectors.toSet()), inputDocument);
        this.getSolrClient().add(inputDocument);
        this.getSolrClient().commit();
    }

    @Override
    public URI storeEntity(ActivityPubObject entity) throws ActivityPubException
    {
        URI storageId;
        try {
            // the entity doesn't have any URI ID yet: we'll create an UID, and store the entity only using this UID
            // So we can retrieve the entity even in case of server URL change.
            // The URI ID will be then computed with this UID.
            if (entity.getId() == null) {
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
                    uuid = entity.getXwikiReference();
                } else {
                    // FIXME: we cannot rely on hashCode because of possible collisions and size limitation,
                    //  but we shouldn't rely on total randomness because of dedup.
                    uuid = UUID.randomUUID().toString();
                }
                ActivityPubResourceReference resourceReference =
                    new ActivityPubResourceReference(entity.getType(), uuid);
                entity.setId(this.serializer.serialize(resourceReference));
                storageId = this.internalURINormalizer.retrieveRelativeURI(resourceReference);
            } else {
                storageId = this.internalURINormalizer.relativizeURI(entity.getId());
            }

            // we don't store the entity ID in the content: instead we rely on the ID put on the document.
            URI entityID = entity.getId();
            entity.setId(null);
            this.storeInformation(entity, storageId.toASCIIString());

            // we put back the ID in case the entity would be used afterwards
            entity.setId(entityID);
            return entity.getId();
        } catch (Exception e) {
            throw new ActivityPubException(String.format("Error while storing [%s].", entity), e);
        }
    }

    private String getActorEntityUID(AbstractActor actor, String entitySuffix)
    {
        return String.format("%s-%s", actor.getXwikiReference(), entitySuffix);
    }

    private boolean retrieveSolrDocument(SolrDocument solrDocument)
    {
        boolean result = true;
        String type = (String) solrDocument.getFieldValue(TYPE_FIELD);

        // We only refresh person or collections, since it's the types the more likely to change
        if ("person".equalsIgnoreCase(type)
            || "collection".equalsIgnoreCase(type)
            || "orderedcollection".equalsIgnoreCase(type)) {

            // if the document is older than one day, and does not belongs to the current instance
            // then we need to refresh it.
            Date updatedDate = (Date) solrDocument.getFieldValue(UPDATED_DATE_FIELD);
            URI id = URI.create((String) solrDocument.getFieldValue(ID_FIELD));
            result = this.urlHandler.belongsToCurrentInstance(id)
                || new Date().before(DateUtils.addDays(updatedDate, 1));
        }
        return result;
    }

    @Override
    public <T extends ActivityPubObject> T retrieveEntity(URI uri) throws ActivityPubException
    {
        T result = null;
        try {
            URI storageId = this.internalURINormalizer.relativizeURI(uri);
            SolrDocument solrDocument = this.getSolrClient().getById(storageId.toASCIIString());
            if (solrDocument != null && !solrDocument.isEmpty() && retrieveSolrDocument(solrDocument)) {
                result = (T) this.jsonParser.parse((String) solrDocument.getFieldValue(CONTENT_FIELD));
                result.setId(uri);
            }
            return result;
        } catch (Exception e) {
            throw new ActivityPubException(
                String.format("Error when trying to retrieve the entity of id [%s]", uri), e);
        }
    }

    @Override
    public void storeWebFinger(JSONResourceDescriptor jsonResourceDescriptor) throws ActivityPubException
    {
        try {
            SolrInputDocument inputDocument = new SolrInputDocument();
            inputDocument.addField(ID_FIELD, jsonResourceDescriptor.getSubject());
            inputDocument.addField(TYPE_FIELD, WEBFINGER_TYPE);
            inputDocument.addField(CONTENT_FIELD, this.webfingerJsonSerializer.serialize(jsonResourceDescriptor));
            inputDocument.addField(UPDATED_DATE_FIELD, new Date());
            this.getSolrClient().add(inputDocument);
            this.getSolrClient().commit();
        } catch (IOException | SolrException | SolrServerException e) {
            throw new ActivityPubException(
                String.format("Error while storing WebFinger record [%s]", jsonResourceDescriptor), e);
        }
    }

    @Override
    public List<JSONResourceDescriptor> searchWebFinger(String query, int limit) throws ActivityPubException
    {
        List<JSONResourceDescriptor> result = new ArrayList<>();
        try {
            String queryString = String.format("filter(type:%s) AND id:*%s*", WEBFINGER_TYPE, query);
            SolrQuery solrQuery = new SolrQuery(DEFAULT_QUERY).addFilterQuery(queryString).setRows(limit);
            QueryResponse queryResponse = this.getSolrClient().query(solrQuery);
            SolrDocumentList results = queryResponse.getResults();
            for (SolrDocument solrDocument : results) {
                result.add(this.webfingerJsonParser.parse((String) solrDocument.getFieldValue(CONTENT_FIELD)));
            }
        } catch (SolrException | SolrServerException | IOException | WebfingerException e) {
            throw new ActivityPubException(
                String.format("Error while performing the query [%s] for WebFinger.", query), e);
        }
        return result;
    }

    @Override
    public <T extends ActivityPubObject> List<T> query(Class<T> type, String query, int limit)
        throws ActivityPubException
    {
        List<T> result = new ArrayList<>();

        String typeQueryString = String.format("filter(type:%s)", type.getSimpleName());
        SolrQuery solrQuery = new SolrQuery(DEFAULT_QUERY)
            .addFilterQuery(typeQueryString)
            .addFilterQuery(query)
            .addSort(UPDATED_DATE_FIELD, SolrQuery.ORDER.desc)
            .setRows(limit);

        try {
            QueryResponse queryResponse = this.getSolrClient().query(solrQuery);
            SolrDocumentList queryResults = queryResponse.getResults();
            for (SolrDocument queryResult : queryResults) {
                T activityPubObject =
                    this.jsonParser.parse((String) queryResult.getFieldValue(CONTENT_FIELD));
                String uid = (String) queryResult.getFieldValue(ID_FIELD);
                URI id = this.internalURINormalizer.retrieveAbsoluteURI(URI.create(uid));
                activityPubObject.setId(id);
                result.add(activityPubObject);
            }
        } catch (SolrException | SolrServerException | IOException e) {
            throw new ActivityPubException(
                String.format("Error while performing query [%s] for type [%s] with limit [%s]",
                    query, type.getSimpleName(), limit), e);
        }
        return result;
    }
}
