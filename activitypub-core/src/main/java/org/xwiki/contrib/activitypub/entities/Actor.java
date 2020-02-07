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

public abstract class Actor extends ActivityPubObject
{
    // TODO: Check IRI <-> URI conversion (https://tools.ietf.org/html/rfc3987#section-3.1)
    // We might have some weird cases with XWiki special username (containing non UTF-8 characters for example)
    private String preferredUsername;
    private ActivityPubObjectReference<Inbox> inbox;
    private ActivityPubObjectReference<Outbox> outbox;
    private ActivityPubObjectReference<OrderedCollection> followers;
    private ActivityPubObjectReference<OrderedCollection> following;

    public String getPreferredUsername()
    {
        return preferredUsername;
    }

    public <T extends Actor> T setPreferredUsername(String preferredUsername)
    {
        this.preferredUsername = preferredUsername;
        return (T) this;
    }

    public ActivityPubObjectReference<OrderedCollection> getFollowers()
    {
        return followers;
    }

    public <T extends Actor> T setFollowers(ActivityPubObjectReference<OrderedCollection> followers)
    {
        this.followers = followers;
        return (T) this;
    }

    public ActivityPubObjectReference<OrderedCollection> getFollowing()
    {
        return following;
    }

    public <T extends Actor> T setFollowing(ActivityPubObjectReference<OrderedCollection> following)
    {
        this.following = following;
        return (T) this;
    }

    public ActivityPubObjectReference<Inbox> getInbox()
    {
        return inbox;
    }

    public <T extends Actor> T setInbox(ActivityPubObjectReference<Inbox> inbox)
    {
        this.inbox = inbox;
        return (T) this;
    }

    public ActivityPubObjectReference<Outbox> getOutbox()
    {
        return outbox;
    }

    public <T extends Actor> T setOutbox(ActivityPubObjectReference<Outbox> outbox)
    {
        this.outbox = outbox;
        return (T) this;
    }
}
