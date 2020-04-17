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
package org.xwiki.contrib.activitypub.internal.stream;

import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;

/**
 * A generic component that allows to resolve a {@link ActivityPubObjectReference} in a stream.
 *
 * @version $Id$
 * @since 1.2
 */
@Component(roles = StreamActivityPubObjectReferenceResolver.class)
@Singleton
public class StreamActivityPubObjectReferenceResolver
{
    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * The function to be used a in stream map to resolve an ActivityPubObjectReference from its reference.
     *
     * @param <T> the actual true type of the ActivityPub object.
     * @return a function that perform the transformation with the right types.
     */
    public <T extends ActivityPubObject> Function<ActivityPubObjectReference<T>, T> getFunction()
    {
        return objectReference -> {
            try {
                return activityPubObjectReferenceResolver.resolveReference(objectReference);
            } catch (ActivityPubException e) {
                logger.error("Error while resolving reference [{}]", objectReference, e);
                return null;
            }
        };
    }
}
