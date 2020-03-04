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
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 *
 * Provides the implementation of business operation for {@link WebfingerResourceReferenceHandler}.
 *
 * @since 1.1
 * @version $Id$
 */
@Component
public class DefaultWebfingerService implements WebfingerService
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ResourceReferenceSerializer<ActivityPubResourceReference, URI> serializer;

    @Inject
    private DocumentAccessBridge documentAccess;

    @Override
    public DocumentReference resolveUser(String username)
    {
        return this.stringDocumentReferenceResolver.resolve(username);
    }

    @Override
    public boolean isExistingUser(DocumentReference documentReference)
    {
        XWikiUser xWikiUser = new XWikiUser(documentReference);
        return xWikiUser.exists(this.contextProvider.get());
    }

    @Override
    public boolean isExistingUser(String username)
    {
        DocumentReference userReference = this.resolveUser(username);
        return this.isExistingUser(userReference);
    }

    @Override
    public URI resolveProfilePageUrl(String username) throws SerializeResourceReferenceException,
                                                                 UnsupportedResourceReferenceException
    {
        ActivityPubResourceReference aprr = new ActivityPubResourceReference("Person", username);
        return this.serializer.serialize(aprr);
    }

    @Override
    public String resolveActivityPubUserUrl(DocumentReference user) throws Exception
    {
        return ((XWikiDocument) this.documentAccess.getDocumentInstance(user))
                   .getExternalURL("activitypub", this.contextProvider.get());
    }
}
