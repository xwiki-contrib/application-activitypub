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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.apache.solr.schema.DatePointField;
import org.apache.solr.schema.FieldType;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.SolrException;

/**
 * Initialize the ActivityPub Solr core to persist the data.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Named("activitypub")
@Singleton
public class ActivityPubSolrInitializer implements SolrCoreInitializer
{
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String STRING_TYPE = "string";
    private static final String DATE_TYPE = "pdate";

    @Override
    public String getCoreName()
    {
        return "activitypub";
    }

    @Override
    public void initialize(SolrClient client) throws SolrException
    {
        try {
            SchemaResponse.FieldsResponse response = new SchemaRequest.Fields().process(client);
            if (!schemaAlreadyExists(response)) {
                createFieldTypes(client);
                createField(client, ActivityPubStorage.CONTENT_FIELD, STRING_TYPE);
                createField(client, ActivityPubStorage.TYPE_FIELD, STRING_TYPE);
                // FIXME: we should rely on the constant introduced by the new SolR API once it will be released.
                createField(client, ActivityPubStorage.UPDATED_DATE_FIELD, DATE_TYPE);
                createField(client, ActivityPubStorage.XWIKI_REFERENCE_FIELD, STRING_TYPE);
            }
        } catch (SolrServerException | IOException | org.apache.solr.common.SolrException e)
        {
            throw new SolrException("Error when initializing activitypub Solr schema", e);
        }
    }

    // FIXME: This needs to be removed after the release of XWiki 12.3RC1
    // Note that we also need the change from
    // https://github.com/xwiki/xwiki-platform/commit/4fc3a7102ee2ea2beae612ebd67f9089a52aa7c4
    private void createFieldTypes(SolrClient client) throws IOException, SolrServerException
    {
        FieldTypeDefinition definition = new FieldTypeDefinition();
        Map<String, Object> typeAttributes = new HashMap<>();
        typeAttributes.put(FieldType.TYPE_NAME, DATE_TYPE);
        typeAttributes.put(FieldType.CLASS_NAME, DatePointField.class.getName());
        typeAttributes.put("docValues", true);
        definition.setAttributes(typeAttributes);
        new SchemaRequest.AddFieldType(definition).process(client);
    }

    private void createField(SolrClient client, String name, String type) throws IOException, SolrServerException
    {
        Map<String, Object> fieldAttributes = new HashMap<>();
        fieldAttributes.put(NAME, name);
        fieldAttributes.put(TYPE, type);
        new SchemaRequest.AddField(fieldAttributes).process(client);
    }

    private boolean schemaAlreadyExists(SchemaResponse.FieldsResponse response)
    {
        if (response != null) {
            for (Map<String, Object> field : response.getFields()) {
                if (ActivityPubStorage.CONTENT_FIELD.equals(field.get(NAME))) {
                    return true;
                }
            }
        }
        return false;
    }
}
