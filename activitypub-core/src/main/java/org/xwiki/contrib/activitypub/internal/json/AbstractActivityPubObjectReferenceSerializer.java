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
package org.xwiki.contrib.activitypub.internal.json;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.resource.ResourceReferenceSerializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * An abstract Jackson Serializer for {@link ActivityPubObjectReference}.
 * This serializer only serialize the references with the objects IDs, which means that it also performs the storage
 * when no ID is found for the object to serialize.
 * The idea here is to avoid serializing big JSON and to avoid having to deal with self-referenced objects by only
 * serializing the references.
 * That abstract class allows to transform the obtained URI to relativize or make it absolute depending on the concrete
 * implementation.
 *
 * @version $Id$
 * @since 1.2
 */
public abstract class AbstractActivityPubObjectReferenceSerializer
    // it is expected that the ActivityPubObjectReference is the raw type, using it with <?> breaks Jackson typing
    extends JsonSerializer<ActivityPubObjectReference>
{
    // We don't inject it since it might need the serializer itself.
    private ActivityPubStorage activityPubStorage;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> activityPubResourceReferenceSerializer;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    private ActivityPubStorage getActivityPubStorage() throws ComponentLookupException
    {
        if (this.activityPubStorage == null) {
            this.activityPubStorage = this.componentManager.getInstance(ActivityPubStorage.class);
        }
        return this.activityPubStorage;
    }

    @Override
    public void serialize(ActivityPubObjectReference objectReference, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException
    {
        try {
            if (objectReference.isLink()) {
                jsonGenerator.writeString(this.transformURI(objectReference.getLink()).toASCIIString());
            } else {
                if (objectReference.isExpand()) {
                    jsonGenerator.writeObject(objectReference.getObject());
                } else {
                    ActivityPubObject object = objectReference.getObject();

                    // the ID wasn't null, but for some reason it wasn't a link, we kept it as an object in the
                    // serialization.
                    if (object.getId() != null) {
                        jsonGenerator.writeString(this.transformURI(object.getId()).toString());
                        // it doesn't have an ID: we need to store it and we serialize it as a link to avoid big JSON
                        // answers.
                    } else {
                        URI uri = this.getActivityPubStorage().storeEntity(object);
                        jsonGenerator.writeString(this.transformURI(uri).toASCIIString());
                    }
                }
            }
        } catch (ComponentLookupException | ActivityPubException e) {
            throw new IOException(
                String.format("Error when serializing reference [%s]", objectReference.toString()), e);
        }
    }

    /**
     * Transform the provided URI before the serialization. This can be useful in particular to resolve relative URI,
     * or in contrary to relativize absolute URI.
     *
     * @param inputURI the URI to transform.
     * @return a transformed URI.
     * @throws ActivityPubException in case of problem during the transformation.
     */
    public abstract URI transformURI(URI inputURI) throws ActivityPubException;
}
