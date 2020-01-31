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

import java.net.URI;

import org.xwiki.contrib.activitystream.entities.ObjectReference;
import org.xwiki.contrib.activitystream.entities.OrderedCollection;

public class Person extends org.xwiki.contrib.activitystream.entities.Person
{
    // TODO: Check IRI <-> URI conversion (https://tools.ietf.org/html/rfc3987#section-3.1)
    // We might have some weird cases with XWiki special username (containing non UTF-8 characters for example)
    private String preferredUsername;
    private URI inbox;
    private URI outbox;
    private ObjectReference<OrderedCollection> followers;
    private ObjectReference<OrderedCollection> following;
    private URI liked;

    public String getPreferredUsername()
    {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername)
    {
        this.preferredUsername = preferredUsername;
    }

    public URI getInbox()
    {
        return inbox;
    }

    public void setInbox(URI inbox)
    {
        this.inbox = inbox;
    }

    public URI getOutbox()
    {
        return outbox;
    }

    public void setOutbox(URI outbox)
    {
        this.outbox = outbox;
    }

    public ObjectReference<OrderedCollection> getFollowers()
    {
        return followers;
    }

    public void setFollowers(
        ObjectReference<OrderedCollection> followers)
    {
        this.followers = followers;
    }

    public ObjectReference<OrderedCollection> getFollowing()
    {
        return following;
    }

    public void setFollowing(
        ObjectReference<OrderedCollection> following)
    {
        this.following = following;
    }

    public URI getLiked()
    {
        return liked;
    }

    public void setLiked(URI liked)
    {
        this.liked = liked;
    }
}
