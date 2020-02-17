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

import com.fasterxml.jackson.annotation.JsonSetter;

public class Activity extends ActivityPubObject
{
    private ActivityPubObjectReference<? extends Actor> actor;
    private ActivityPubObjectReference<? extends ActivityPubObject> object;

    public ActivityPubObjectReference<? extends Actor> getActor()
    {
        return actor;
    }

    @JsonSetter
    public <T extends Activity> T setActor(ActivityPubObjectReference<? extends Actor> actor)
    {
        this.actor = actor;
        return (T) this;
    }

    public <T extends Activity> T setActor(Actor actor)
    {
        return this.setActor(new ActivityPubObjectReference<Actor>().setObject(actor));
    }

    public ActivityPubObjectReference<? extends ActivityPubObject> getObject()
    {
        return object;
    }

    @JsonSetter
    public <T extends Activity> T setObject(ActivityPubObjectReference<? extends ActivityPubObject> object)
    {
        this.object = object;
        return (T) this;
    }

    public <T extends Activity> T setObject(ActivityPubObject object)
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
        Activity activity = (Activity) o;
        return Objects.equals(actor, activity.actor) &&
            Objects.equals(object, activity.object);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(super.hashCode(), actor, object);
    }
}
