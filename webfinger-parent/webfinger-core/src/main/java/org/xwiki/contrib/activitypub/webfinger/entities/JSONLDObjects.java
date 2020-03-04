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
package org.xwiki.contrib.activitypub.webfinger.entities;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 
 * FIXME: duplicate from activitypub-core
 * 
 * An artificial type to put the right JSON-LD context property everywhere.
 *
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class JSONLDObjects
{
    private static final String ACTIVITY_STREAM_CONTEXT = "https://www.w3.org/ns/activitystreams";

    @JsonProperty("@context")
    private URI context;

    /**
     * Default constructor: by default the ActivityStream context is set.
     */
    public JSONLDObjects()
    {
        try {
            this.context = new URI(ACTIVITY_STREAM_CONTEXT);
        } catch (URISyntaxException e) {
            // Should never happen
            e.printStackTrace();
        }
    }

    /**
     * @return the current context
     */
    public URI getContext()
    {
        return context;
    }

    /**
     * @param context the current context.
     */
    public void setContext(URI context)
    {
        this.context = context;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JSONLDObjects object = (JSONLDObjects) o;
        return new EqualsBuilder().append(context, object.context).build();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(context).build();
    }
}
