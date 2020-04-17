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
package org.xwiki.contrib.activitypub.internal.filters;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.internal.stream.StreamActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.internal.stream.StreamActivityPubObjectReferenceSerializer;

/**
 * A filter that removes any non-public activities before returning the results.
 * This filter aimed at being used for {@link org.xwiki.contrib.activitypub.entities.Inbox} and
 * {@link org.xwiki.contrib.activitypub.entities.Outbox}.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Singleton
public class PublicActivityCollectionFilter implements CollectionFilter<OrderedCollection<AbstractActivity>>
{
    @Inject
    private PublicActivityFilter publicActivityFilter;

    @Inject
    private StreamActivityPubObjectReferenceResolver streamActivityPubObjectReferenceResolver;

    @Inject
    private StreamActivityPubObjectReferenceSerializer streamActivityPubObjectReferenceSerializer;

    @Override
    public OrderedCollection<AbstractActivity> filter(OrderedCollection<AbstractActivity> collection)
    {
        List<ActivityPubObjectReference<AbstractActivity>> activityList = collection.getOrderedItems().stream()
            .map(streamActivityPubObjectReferenceResolver.getFunction())
            .filter(publicActivityFilter)
            .map(streamActivityPubObjectReferenceSerializer.getFunction())
            .collect(Collectors.toList());
        collection.setOrderedItems(activityList);
        return collection;
    }
}
