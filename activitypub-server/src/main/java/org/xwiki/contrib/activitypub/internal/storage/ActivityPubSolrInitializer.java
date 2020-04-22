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
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.schema.SchemaResponse;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.search.solr.AbstractSolrCoreInitializer;
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
public class ActivityPubSolrInitializer extends AbstractSolrCoreInitializer
{
    private static final String NAME = "name";
    private static final long CURRENT_VERSION = 10200000;

    @Override
    protected long getVersion()
    {
        return CURRENT_VERSION;
    }

    /**
     * This returns true if there's already an ActivityPub core containing a content field.
     */
    private boolean oldSchemaAlreadyExists(SolrClient client) throws IOException, SolrServerException
    {
        SchemaResponse.FieldsResponse response = new SchemaRequest.Fields().process(client);
        if (response != null) {
            for (Map<String, Object> field : response.getFields()) {
                if (ActivityPubStorage.CONTENT_FIELD.equals(field.get(NAME))) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void createSchema() throws SolrException
    {
        try {
            // we normally enter here only if there's no version on the core,
            // so if there's already an existing schema it means a user installed an older version of AP.
            if (oldSchemaAlreadyExists(this.client)) {
                throw new SolrException("This version of ActivityPub is incompatible with previous version, "
                    + "you need to remove the existing ActivityPub Solr core before proceeding.");
            } else {
                this.addStringField(ActivityPubStorage.TYPE_FIELD, false, false);
                this.addStringField(ActivityPubStorage.CONTENT_FIELD, false, false);
                this.addStringField(ActivityPubStorage.XWIKI_REFERENCE_FIELD, false, false);
                this.addPDateField(ActivityPubStorage.UPDATED_DATE_FIELD, false, false);
                this.addBooleanField(ActivityPubStorage.IS_PUBLIC_FIELD, false, false);
                this.addStringField(ActivityPubStorage.AUTHORS_FIELD, true, false);
                this.addStringField(ActivityPubStorage.TARGETED_FIELD, true, false);
            }
        } catch (IOException | SolrServerException e) {
            throw new SolrException("Error while checking if the schema already exist.", e);
        }
    }

    @Override
    protected void migrateSchema(long cversion) throws SolrException
    {
        // no migration yet
    }
}
