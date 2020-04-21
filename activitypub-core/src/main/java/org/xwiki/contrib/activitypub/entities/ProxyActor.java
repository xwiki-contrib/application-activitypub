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
import java.net.URISyntaxException;
import java.util.Collections;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.stability.Unstable;

/**
 * This class aims at representing a proxy towards different kind of actors representation.
 * Its main goal is to be used for {@code to}, {@code bcc} or {@code attributedTo} attributes. Those attributes
 * accepts different kind of actors representation: actor objects or actor references
 * (like in {@link ActivityPubObjectReference}) but also list of actors references through the endpoint to followers
 * collection for example.
 * We use this class to allow putting any kind of {@link ActivityPubObjectReference} in the appropriate fields, and to
 * provide helpers to resolve and/or add the actors.
 *
 * @version $Id$
 * @since 1.1
 */
@Unstable
public class ProxyActor extends ActivityPubObjectReference<ActivityPubObject>
{
    private static URI publicActorURI;
    private static ProxyActor publicActor;

    private static final String METHOD_NOT_ALLOWED = "This method cannot be called in PublicActor.";

    static {
        try {
            publicActorURI = new URI("https://www.w3.org/ns/activitystreams#Public");
        } catch (URISyntaxException e) {
            // should never happen
        }
    }

    /**
     * Default constructor of a {@code ProxyActor}.
     * @param reference the reference to the actual actor(s) this proxy is created for.
     */
    public ProxyActor(URI reference)
    {
        if (reference == null) {
            throw new IllegalArgumentException("The reference of a proxy actor must not be null.");
        }
        super.setLink(reference);
    }

    /**
     * Helper constructor for Jackson serialization.
     * It's basically build an URI and calls {@link #ProxyActor(URI)}
     * @param reference a serialized URI as a string.
     */
    public ProxyActor(String reference)
    {
        this(URI.create(reference));
    }

    /**
     * @return a singleton instance to the public actor.
     */
    public static ProxyActor getPublicActor()
    {
        if (publicActor == null) {
            publicActor = new ProxyActor(publicActorURI);
        }
        return publicActor;
    }

    /**
     * @return {@code true} if this is a public actor.
     * @see <a href="https://www.w3.org/TR/activitypub/#public-addressing">information about the public actor</a>
     */
    public boolean isPublic()
    {
        return this.equals(getPublicActor());
    }

    /**
     * Return the actual references to the real actor(s).
     * If the proxy has been created for a single actor, the returned collection is a single list element.
     * If the proxy has been created for a collections of actors, it returns the collection.
     * In all other cases, it throws an exception.
     *
     * @param resolver an {@link ActivityPubObjectReferenceResolver} used to retrieve the actual type of elements.
     * @return a collections of reference to the actual actors.
     * @throws ActivityPubException in case of problems for the resolution, or if the type is wrong.
     */
    public java.util.Collection<ActivityPubObjectReference<AbstractActor>> resolveActors(
        ActivityPubObjectReferenceResolver resolver) throws ActivityPubException
    {
        java.util.Collection<ActivityPubObjectReference<AbstractActor>> result = null;
        ActivityPubObject object = resolver.resolveReference(this);
        if (object instanceof AbstractActor) {
            result = Collections.singletonList(object.getReference());
        } else if (object instanceof AbstractCollection) {
            AbstractCollection<AbstractActor> actorList = (AbstractCollection<AbstractActor>) object;
            result = actorList.getAllItems();
        } else {
            throw new ActivityPubException(String.format("The given element cannot be processed here: [%s]", object));
        }
        return result;
    }

    @Override
    public boolean equals(Object object)
    {
        if (object != null && ActivityPubObjectReference.class.isAssignableFrom(object.getClass())) {
            ActivityPubObjectReference<ActivityPubObject> objectReference = (ActivityPubObjectReference) object;
            return new EqualsBuilder()
                .append(isLink(), objectReference.isLink())
                .append(getLink(), objectReference.getLink()).build();
        } else {
            return super.equals(object);
        }
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }
}
