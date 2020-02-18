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

/**
 * Abstract type to represent an ActivityStream Actor.
 * Note that we actually reused both definition from ActivityStream and ActivityPub to define this entity since here
 * it's aimed at being used for ActivityPub.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/#actors">ActivityStream Actor definition</a>
 * @see <a href="https://www.w3.org/TR/activitypub/#actor-objects">ActivityPub Actor definition</a>
 * @version $Id$
 */
public abstract class AbstractActor extends ActivityPubObject
{
    // TODO: Check IRI <-> URI conversion (https://tools.ietf.org/html/rfc3987#section-3.1)
    // We might have some weird cases with XWiki special username (containing non UTF-8 characters for example)
    private String preferredUsername;
    private ActivityPubObjectReference<Inbox> inbox;
    private ActivityPubObjectReference<Outbox> outbox;
    private ActivityPubObjectReference<OrderedCollection<AbstractActor>> followers;
    private ActivityPubObjectReference<OrderedCollection<AbstractActor>> following;

    /**
     * @return the username of the actor.
     */
    public String getPreferredUsername()
    {
        return preferredUsername;
    }

    /**
     * @param preferredUsername the username of the actor.
     * @param <T> the type of actor.
     * @return the current object for fluent API.
     */
    public <T extends AbstractActor> T setPreferredUsername(String preferredUsername)
    {
        this.preferredUsername = preferredUsername;
        return (T) this;
    }

    /**
     * @return a reference to the collection of actors following the current one.
     * @see <a href="https://www.w3.org/TR/activitypub/#followers">ActivityPub definition</a>
     */
    public ActivityPubObjectReference<OrderedCollection<AbstractActor>> getFollowers()
    {
        return followers;
    }

    /**
     * @param followers a reference to the collection of actors following the current one.
     * @param <T> the type of actor.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitypub/#followers">ActivityPub definition</a>
     */
    public <T extends AbstractActor> T setFollowers(
        ActivityPubObjectReference<OrderedCollection<AbstractActor>> followers)
    {
        this.followers = followers;
        return (T) this;
    }

    /**
     * @return a reference to the collection of actors followed by the current one.
     * @see <a href="https://www.w3.org/TR/activitypub/#following">ActivityPub definition</a>
     */
    public ActivityPubObjectReference<OrderedCollection<AbstractActor>> getFollowing()
    {
        return following;
    }

    /**
     * @param following a reference to the collection of actors followed by the current one.
     * @param <T> the type of actor.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitypub/#following">ActivityPub definition</a>
     */
    public <T extends AbstractActor> T setFollowing(
        ActivityPubObjectReference<OrderedCollection<AbstractActor>> following)
    {
        this.following = following;
        return (T) this;
    }

    /**
     * @return a reference to the {@link Inbox} of the actor.
     * @see <a href="https://www.w3.org/TR/activitypub/#inbox">ActivityPub definition</a>
     */
    public ActivityPubObjectReference<Inbox> getInbox()
    {
        return inbox;
    }

    /**
     * @param inbox a reference to the {@link Inbox} of the actor.
     * @param <T> the type of the actor.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitypub/#inbox">ActivityPub definition</a>
     */
    public <T extends AbstractActor> T setInbox(ActivityPubObjectReference<Inbox> inbox)
    {
        this.inbox = inbox;
        return (T) this;
    }

    /**
     * @return a reference to the {@link Outbox} of the actor.
     * @see <a href="https://www.w3.org/TR/activitypub/#outbox">ActivityPub definition</a>
     */
    public ActivityPubObjectReference<Outbox> getOutbox()
    {
        return outbox;
    }

    /**
     * @param outbox a reference to the {@link Outbox} of the actor.
     * @param <T> the type of the actor.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitypub/#outbox">ActivityPub definition</a>
     */
    public <T extends AbstractActor> T setOutbox(ActivityPubObjectReference<Outbox> outbox)
    {
        this.outbox = outbox;
        return (T) this;
    }
}
