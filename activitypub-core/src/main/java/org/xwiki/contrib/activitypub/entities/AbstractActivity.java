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

import java.util.Objects;

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonSetter;

/**
 * An abstract type to represent any kind of Activity defined by ActivityStream.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-core/#activities">ActivityStream explanation about activities</a>
 * @version $Id$
 * @since 1.0
 */
@Unstable
public abstract class AbstractActivity extends ActivityPubObject
{
    private ActivityPubObjectReference<? extends AbstractActor> actor;
    private ActivityPubObjectReference<? extends ActivityPubObject> object;

    /**
     * @return the reference to the actor of the activity.
     */
    public ActivityPubObjectReference<? extends AbstractActor> getActor()
    {
        return actor;
    }

    /**
     * Set the actor of the activity with the given reference.
     * This method is meant to be used by the JSON parser.
     *
     * @param actor the reference to use to set the actor.
     * @param <T> the type of activity.
     * @return the current activity for fluent API.
     */
    @JsonSetter
    public <T extends AbstractActivity> T setActor(ActivityPubObjectReference<? extends AbstractActor> actor)
    {
        this.actor = actor;
        return (T) this;
    }

    /**
     * Helper to set directly the actor of the activity by using the concrete object instead of the reference.
     *
     * @param actor the concrete actor to set.
     * @param <T> the type of activity.
     * @return the current activity for fluent API.
     */
    public <T extends AbstractActivity> T setActor(AbstractActor actor)
    {
        return this.setActor(new ActivityPubObjectReference<AbstractActor>().setObject(actor));
    }

    /**
     * @return the reference to the object of the activity.
     */
    public ActivityPubObjectReference<? extends ActivityPubObject> getObject()
    {
        return object;
    }

    /**
     * Set the object of the activity with the given reference.
     * This method is meant to be used by the JSON parser.
     *
     * @param object the reference to use to set the object.
     * @param <T> the type of activity.
     * @return the current activity for fluent API.
     */
    @JsonSetter
    public <T extends AbstractActivity> T setObject(ActivityPubObjectReference<? extends ActivityPubObject> object)
    {
        this.object = object;
        return (T) this;
    }

    /**
     * Helper to set directly the object of the activity by using the concrete object instead of the reference.
     *
     * @param object the concrete object to set.
     * @param <T> the type of activity.
     * @return the current activity for fluent API.
     */
    public <T extends AbstractActivity> T setObject(ActivityPubObject object)
    {
        return this.setObject(new ActivityPubObjectReference<ActivityPubObject>().setObject(object));
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
        if (!super.equals(o)) {
            return false;
        }
        AbstractActivity activity = (AbstractActivity) o;
        return Objects.equals(actor, activity.actor)
            && Objects.equals(object, activity.object);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), actor, object);
    }
}
