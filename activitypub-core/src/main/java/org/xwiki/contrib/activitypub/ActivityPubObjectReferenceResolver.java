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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Collection;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.model.reference.DocumentReference;
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
     * Define the default maximum number of day before an information might be considered outdated.
     * See {@link #shouldBeRefreshed(ActivityPubObject)} for more information.
     */
    int MAX_DAY_BEFORE_REFRESH = 1;
    /**
     * Define the classes that should be considered as outdated after some amount of time.
     * See {@link #shouldBeRefreshed(ActivityPubObject)} for more information.
     */
    List<Class<? extends ActivityPubObject>> CLASSES_TO_REFRESH = Arrays.asList(
        Person.class,
        Service.class,
        Collection.class,
        OrderedCollection.class
    );

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

    /**
     * Define if an information should be refreshed by requesting it from its ID.
     * This method allows to determine if we should try to perform an Http Request even if an information is locally
     * stored. The informations should be discard if there's a chance they have changed on their original server.
     * Then only information containing at least an ID referring to a remote server should be refreshed and after a
     * given amount of time (see {@link #MAX_DAY_BEFORE_REFRESH} for the default amount of time). Moreover we consider
     * by default that only some classes should be refreshed (see {@link #CLASSES_TO_REFRESH}).
     *
     * @param activityPubObject the object to determine if we need to refresh it or not.
     * @param <T> the concrete type of object.
     * @return {@code true} if this object needs to be retrieved back with an Http request, and {@code false} to rely
     *         on the current implementation.
     */
    <T extends ActivityPubObject> boolean shouldBeRefreshed(T activityPubObject);

    /**
     * Resolve an XWiki {@link DocumentReference} to an ActivityPub {@link Page}.
     * Note that the resolved page is already stored but might have several attributes not defined:
     * the page might have been created from the given document reference.
     * In all cases, this method ensures that only one occurence of a {@link Page} is stored for a given
     * {@link DocumentReference}.
     *
     * @param documentReference the reference for which to retrieve an associated page instance.
     * @return a page matching the given reference.
     * @since 1.5
     */
    @Unstable
    default Page resolveDocumentReference(DocumentReference documentReference) throws ActivityPubException
    {
        return new Page();
    }
}
