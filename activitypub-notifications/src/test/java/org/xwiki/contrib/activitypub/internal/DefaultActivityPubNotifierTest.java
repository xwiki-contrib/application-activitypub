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
package org.xwiki.contrib.activitypub.internal;

import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.xwiki.contrib.activitypub.ActivityPubEvent;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.Accept;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * Test of {@link DefaultActivityPubNotifier}.
 *
 * @since 1.0
 * @version $Id$
 */
@ComponentTest
public class DefaultActivityPubNotifierTest
{
    @InjectMockComponents
    private DefaultActivityPubNotifier defaultActivityPubNotifier;

    @MockComponent
    private ObservationManager observationManager;

    @Test
    void notifyNoTargets()
    {
        this.defaultActivityPubNotifier.notify(new Accept(), new HashSet<>());
        verify(this.observationManager)
            .notify(ArgumentMatchers.argThat(
                (ActivityPubEvent<? extends AbstractActivity> activityPubEvent) -> activityPubEvent.getTarget()
                                                                                       .isEmpty()),
                eq("org.xwiki.contrib:activitypub-notifications"), eq("Accept"));
    }

    @Test
    void notifyOneTarget()
    {
        final HashSet<EntityReference> targets = new HashSet<>();
        targets.add(new DocumentReference("xwiki", "XWiki", "test"));
        this.defaultActivityPubNotifier.notify(new Accept(), targets);
        verify(this.observationManager)
            .notify(ArgumentMatchers.argThat(
                (ActivityPubEvent<? extends AbstractActivity> activityPubEvent) -> activityPubEvent.getTarget().size() == 1),
                eq("org.xwiki.contrib:activitypub-notifications"), eq("Accept"));
    }
}
