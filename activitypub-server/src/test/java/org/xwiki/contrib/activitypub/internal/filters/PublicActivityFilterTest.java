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

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PublicActivityFilter}.
 *
 * @version $Id$
 * @since 1.2
 */
@ComponentTest
public class PublicActivityFilterTest
{
    @InjectMockComponents
    private PublicActivityFilter publicActivityFilter;

    @Test
    public void test()
    {
        AbstractActivity activity = mock(AbstractActivity.class);
        assertFalse(this.publicActivityFilter.test(activity));

        List<ProxyActor> proxyActorList = mock(List.class);
        when(activity.getTo()).thenReturn(proxyActorList);
        assertFalse(this.publicActivityFilter.test(activity));

        when(proxyActorList.contains(ProxyActor.getPublicActor())).thenReturn(true);
        assertTrue(this.publicActivityFilter.test(activity));
    }
}
