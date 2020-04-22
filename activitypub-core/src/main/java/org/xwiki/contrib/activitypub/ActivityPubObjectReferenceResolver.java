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
package org.xwiki.contrib.activitypub;

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.stability.Unstable;

/**
 * A resolver to automatically retrieve the object associated to a {@link ActivityPubObjectReference}.
 *
 * @since 1.0
 * @version $Id$
 */
@Unstable
@Role
public interface ActivityPubObjectReferenceResolver
{
    /**
     * Resolve the given reference either by returning its concrete object if the reference is already resolved, or by
     * loading and parsing the linked entity. This never returns null.
     * @param reference the reference to resolve.
     * @param <T> the concrete type of the object pointed by the reference.
     * @return a real instance of the object pointed by the reference.
     * @throws ActivityPubException in case any error occurred when loading or parsing the reference.
     */
    <T extends ActivityPubObject> T resolveReference(ActivityPubObjectReference<T> reference)
        throws ActivityPubException;

    /**
     * Resolve the targets of the given object for delivery and perform deduplication.
     * This methods perform a first resolution of the proxy actors and then store the information with
     * {@link ActivityPubObject#setComputedTargets(Set)} so it can be reused later.
     *
     * @param activityPubObject the object for which to resolve the targets
     * @return a set of concrete targeted actors.
     * @since 1.2
     */
    @Unstable
    Set<AbstractActor> resolveTargets(ActivityPubObject activityPubObject);
}
