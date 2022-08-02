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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.internal.stream.StreamActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.internal.stream.StreamActivityPubObjectReferenceSerializer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PublicActivityCollectionFilter}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
@ComponentList({ StreamActivityPubObjectReferenceSerializer.class, StreamActivityPubObjectReferenceResolver.class })
public class PublicActivityCollectionFilterTest
{
    @InjectMockComponents
    private PublicActivityCollectionFilter publicActivityCollectionFilter;

    @MockComponent
    private PublicActivityFilter publicActivityFilter;

    @MockComponent
    private ActivityPubObjectReferenceResolver resolver;

    @Test
    public void filterEmptyList() throws Exception
    {
        OrderedCollection<AbstractActivity> activityCollection = mock(OrderedCollection.class);
        when(activityCollection.getOrderedItems()).thenReturn(Collections.emptyList());

        OrderedCollection<AbstractActivity> obtainedActivity =
            publicActivityCollectionFilter.filter(activityCollection);
        verify(obtainedActivity).setOrderedItems(Collections.emptyList());
        assertSame(activityCollection, obtainedActivity);
    }

    @Test
    public void filter() throws Exception
    {
        OrderedCollection<AbstractActivity> activityCollection = mock(OrderedCollection.class);
        ActivityPubObjectReference<AbstractActivity> ref1 = mock(ActivityPubObjectReference.class);
        ActivityPubObjectReference<AbstractActivity> ref2 = mock(ActivityPubObjectReference.class);
        ActivityPubObjectReference<AbstractActivity> ref3 = mock(ActivityPubObjectReference.class);
        AbstractActivity activity1 = mock(AbstractActivity.class);
        AbstractActivity activity2 = mock(AbstractActivity.class);
        AbstractActivity activity3 = mock(AbstractActivity.class);
        when(this.resolver.resolveReference(ref1)).thenReturn(activity1);
        when(this.resolver.resolveReference(ref2)).thenReturn(activity2);
        when(this.resolver.resolveReference(ref3)).thenReturn(activity3);
        when(activity1.getReference()).thenReturn((ActivityPubObjectReference) ref1);
        when(activity2.getReference()).thenReturn((ActivityPubObjectReference) ref2);
        when(activity3.getReference()).thenReturn((ActivityPubObjectReference) ref3);

        when(this.publicActivityFilter.test(activity1)).thenReturn(true);
        when(this.publicActivityFilter.test(activity2)).thenReturn(false);
        when(this.publicActivityFilter.test(activity3)).thenReturn(true);

        List<ActivityPubObjectReference<AbstractActivity>> expected = Arrays.asList(ref1, ref3);
        when(activityCollection.getOrderedItems()).thenReturn(Arrays.asList(ref1, ref2, ref3));
        OrderedCollection<AbstractActivity> obtainedActivity = publicActivityCollectionFilter.filter(activityCollection);
        verify(obtainedActivity).setOrderedItems(expected);
        assertSame(activityCollection, obtainedActivity);

        when(this.publicActivityFilter.test(activity1)).thenReturn(false);
        when(this.publicActivityFilter.test(activity3)).thenReturn(false);
        obtainedActivity = publicActivityCollectionFilter.filter(activityCollection);
        verify(obtainedActivity).setOrderedItems(Collections.emptyList());
        assertSame(activityCollection, obtainedActivity);
    }
}
