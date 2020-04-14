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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.xwiki.component.annotation.Component;
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
    private static final String CONTENT = "content";
    private static final String STRING_TYPE = "string";

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
                createField(client, CONTENT, STRING_TYPE);
                createField(client, TYPE, STRING_TYPE);
                // FIXME: we should rely on the constant introduced by the new SolR API once it will be released.
                createField(client, "updatedDate", "pdate");
            }
        } catch (SolrServerException | IOException | org.apache.solr.common.SolrException e)
        {
            throw new SolrException("Error when initializing activitypub Solr schema", e);
        }
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
                if (CONTENT.equals(field.get(NAME))) {
                    return true;
                }
            }
        }
        return false;
    }
}
