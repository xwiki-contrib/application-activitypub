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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.user.User;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.user.UserResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide common implementation to work with XWikiUsers for both ActivityPub and WebFinger.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = XWikiUserBridge.class)
@Singleton
public class XWikiUserBridge
{
    @Inject
    private UserManager userManager;

    @Inject
    private UserResolver<UserReference> userResolver;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private UserReferenceResolver<DocumentReference> userFromDocumentReferenceResolver;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DocumentAccessBridge documentAccess;

    /**
     * Check if there's an existing user with the given username.
     *
     * @param username the name of the user to check.
     * @return {@code true} if an user exists.
     */
    public boolean isExistingUser(String username)
    {
        UserReference userReference = this.userReferenceResolver.resolve(username);
        return this.userManager.exists(userReference);
    }

    /**
     * Retrieve a username associated to the given user.
     *
     * @param user the user for which to retrieve a username.
     * @return a username.
     */
    public String getUserLogin(User user)
    {
        return this.userReferenceSerializer.serialize(user.getUserReference());
    }

    /**
     * Converts an username to an {@link UserReference}.
     *
     * @param username the username.
     * @return the user reference.
     */
    public UserReference resolveUser(String username)
    {
        return this.userReferenceResolver.resolve(username);
    }

    /**
     * Retrieve the actual user associated to the given reference.
     *
     * @param reference the reference of an user.
     * @return the actual user associated to the given reference or null if the user does not exist.
     */
    public User resolveUser(UserReference reference)
    {
        if (this.userManager.exists(reference)) {
            return this.userResolver.resolve(reference);
        } else {
            return null;
        }
    }

    /**
     * Helper to convert a document reference to an user reference.
     *
     * @param documentReference the reference to convert.
     * @return a user reference.
     */
    public UserReference resolveDocumentReference(DocumentReference documentReference)
    {
        return this.userFromDocumentReferenceResolver.resolve(documentReference);
    }

    /**
     * Retrieve the URL to the profile of an user.
     *
     * @param user the user for which to retrieve the profile URL.
     * @return an absolute URL as a string.
     * @throws Exception in case of error when retrieving the URL.
     */
    public String getUserProfileURL(User user) throws Exception
    {
        UserReference userReference = user.getUserReference();

        // FIXME: this is a hack which only works with DocumentUserReference.
        String serializedReference = this.userReferenceSerializer.serialize(userReference);
        DocumentReference documentReference = this.documentReferenceResolver.resolve(serializedReference);
        return ((XWikiDocument) this.documentAccess.getDocumentInstance(documentReference))
            .getExternalURL("view", this.contextProvider.get());
    }
}
