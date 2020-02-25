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
package org.xwiki.contrib.activitypub.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an Inbox as defined by ActivityPub.
 *
 * @see <a href="https://www.w3.org/TR/activitypub/#inbox">ActivityPub Inbox definition</a>
 * @version $Id$
 * @since 1.0
 */
@Unstable
@JsonDeserialize(as = Inbox.class)
public class Inbox extends OrderedCollection<AbstractActivity>
{
    @JsonIgnore
    private Map<String, AbstractActivity> items;

    @JsonIgnore
    private List<Follow> pendingFollows;

    /**
     * Default constructor.
     */
    public Inbox()
    {
        this.items = new HashMap<>();
        this.pendingFollows = new ArrayList<>();
    }

    /**
     * Store an activity.
     * @param activity the activity to be stored.
     */
    public void addActivity(AbstractActivity activity)
    {
        if (activity.getId() == null) {
            throw new IllegalArgumentException("The activity ID must not be null.");
        }
        this.items.put(activity.getId().toASCIIString(), activity);
    }

    /**
     * Store the follow activities not yet handled.
     * @param follow a new follow activity received.
     */
    public void addPendingFollow(Follow follow)
    {
        this.pendingFollows.add(follow);
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

        Inbox object = (Inbox) o;
        return new EqualsBuilder()
            .appendSuper(super.equals(o))
            .append(items, object.items)
            .append(pendingFollows, object.pendingFollows).build();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .appendSuper(super.hashCode())
            .append(items)
            .append(pendingFollows).build();
    }
}
