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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DefaultActivityPubConfiguration}.
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultActivityPubConfigurationTest
{
    @InjectMockComponents
    private DefaultActivityPubConfiguration activityPubConfiguration;

    @MockComponent
    @Named("activitypub")
    private ConfigurationSource configuration;

    @Test
    public void getFollowPolicyAccept()
    {
        when(this.configuration.getProperty("followPolicy", "reject")).thenReturn("accept");
        assertEquals(ActivityPubConfiguration.FollowPolicy.ACCEPT, this.activityPubConfiguration.getFollowPolicy());
    }

    @Test
    public void getFollowPolicyReject()
    {
        when(this.configuration.getProperty("followPolicy", "reject")).thenReturn("reject");
        assertEquals(ActivityPubConfiguration.FollowPolicy.REJECT, this.activityPubConfiguration.getFollowPolicy());
    }

    @Test
    public void getFollowPolicyAsk()
    {
        when(this.configuration.getProperty("followPolicy", "reject")).thenReturn("ask");
        assertEquals(ActivityPubConfiguration.FollowPolicy.ASK, this.activityPubConfiguration.getFollowPolicy());
    }

    @Test
    public void getFollowPolicyErr()
    {
        when(this.configuration.getProperty("followPolicy", "reject")).thenReturn("err");
        assertEquals(ActivityPubConfiguration.FollowPolicy.REJECT, this.activityPubConfiguration.getFollowPolicy());
    }
}
