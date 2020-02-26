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

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;

/**
 * Default implementation of {@link ActivityPubObjectReferenceResolver}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultActivityPubObjectReferenceResolver implements ActivityPubObjectReferenceResolver
{
    @Inject
    private ActivityPubJsonParser activityPubJsonParser;
    
    @Inject
    private ActivityPubClient activityPubClient;

    @Override
    public <T extends ActivityPubObject> T resolveReference(ActivityPubObjectReference<T> reference)
        throws ActivityPubException
    {
        T result = reference.getObject();
        if (!reference.isLink() && result == null) {
            throw new ActivityPubException("The reference property is null and does not have any ID to follow.");
        }
        if (result == null) {
            try {
                HttpMethod getMethod = this.activityPubClient.get(reference.getLink());
                this.activityPubClient.checkAnswer(getMethod);
                result = this.activityPubJsonParser.parse(getMethod.getResponseBodyAsString());
                reference.setObject(result);
            } catch (IOException e) {
                throw new ActivityPubException(
                    String.format("Error when retrieving the ActivityPub information from [%s]", reference.getLink()),
                    e);
            }
        }
        return result;
    }
}
