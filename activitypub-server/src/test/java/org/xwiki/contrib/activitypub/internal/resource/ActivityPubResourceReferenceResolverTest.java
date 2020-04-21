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
package org.xwiki.contrib.activitypub.internal.resource;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link ActivityPubResourceReferenceResolver}.
 *
 * @version $Id$
 */
@ComponentTest
public class ActivityPubResourceReferenceResolverTest
{
    @InjectMockComponents
    private ActivityPubResourceReferenceResolver resourceReferenceResolver;

    @Test
    public void resolveWithAPSegment() throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("activitypub", "foo", "bar"),
            Collections.singletonMap("test", Arrays.asList("42", "43")));

        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("foo", "bar");
        resourceReference.addParameter("test", "42");
        resourceReference.addParameter("test", "43");

        assertEquals(resourceReference,
            this.resourceReferenceResolver.resolve(
                extendedURL,
                new ResourceType("activitypub"),
                Collections.emptyMap()));
    }

    @Test
    public void resolve() throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("foo", "bar"),
            Collections.singletonMap("test", Arrays.asList("42", "43")));

        ActivityPubResourceReference resourceReference = new ActivityPubResourceReference("foo", "bar");
        resourceReference.addParameter("test", "42");
        resourceReference.addParameter("test", "43");

        assertEquals(resourceReference,
            this.resourceReferenceResolver.resolve(
                extendedURL,
                new ResourceType("activitypub"),
                Collections.emptyMap()));
    }
}
