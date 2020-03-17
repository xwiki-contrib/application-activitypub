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
package org.xwiki.contrib.activitypub.webfinger.internal;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.user.UserReference;

/**
 *
 * Provides the implementation of business operation for {@link WebfingerResourceReferenceHandler}.
 *
 * @since 1.1
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWebfingerService implements WebfingerService
{
    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private DefaultURLHandler defaultURLHandler;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Override
    public URI resolveActivityPubUserUrl(String username) throws WebfingerException
    {
        // FIXME: deal more cleanly with the username prefix.
        ActivityPubResourceReference aprr = new ActivityPubResourceReference("Person", "xwiki:" + username);
        try {
            return this.defaultURLHandler.getAbsoluteURI(this.serializer.serialize(aprr));
        } catch (Exception e) {
            throw new WebfingerException(String.format("Error while serializing reference for user [%s]", username), e);
        }
    }

    @Override
    public String resolveXWikiUserUrl(UserReference userReference) throws WebfingerException
    {
        try {
            return this.xWikiUserBridge.getUserProfileURL(userReference);
        } catch (Exception e) {
            throw new WebfingerException(String.format("Error while getting profile URL for user [%s]", userReference),
                e);
        }
    }
}
