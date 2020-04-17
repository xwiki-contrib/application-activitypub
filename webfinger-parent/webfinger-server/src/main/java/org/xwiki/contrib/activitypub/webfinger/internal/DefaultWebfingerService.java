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
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubIdentifierService;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.UserReference;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

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
    private static final String ACTOR_TYPE_ERROR = "This actor type is not supported yet [%s]";

    private static final String WIKI_SEPARATOR_SPLIT_REGEX = "\\.";

    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private DocumentAccessBridge documentAccess;

    @Inject
    private ActivityPubIdentifierService activityPubIdentifierService;

    private Service resolveWikiActor(String wikiName) throws WikiManagerException, ActivityPubException
    {
        boolean wikiExist;
        WikiReference wikiReference;
        Service result = null;

        // we are in the case xwiki.xwiki: we return the current wiki actor
        if (this.activityPubIdentifierService.getWikiIdentifier().equals(wikiName)) {
            wikiExist = true;
            wikiReference = this.contextProvider.get().getWikiReference();
            // we are in the case subwiki.wiki: we check if the subwiki exist before returning it.
        } else {
            wikiExist = this.wikiDescriptorManager.exists(wikiName);
            wikiReference = new WikiReference(wikiName);
        }
        if (wikiExist) {
            result = this.actorHandler.getActor(wikiReference);
        }
        return result;
    }

    private Person resolveUserActor(String username, String wikiName) throws WikiManagerException, ActivityPubException
    {
        Person result = null;
        UserReference userReference;
        if (wikiName != null && this.wikiDescriptorManager.exists(wikiName)) {
            WikiReference otherWiki = new WikiReference(wikiName);
            userReference = this.xWikiUserBridge.resolveUser(username, otherWiki);
        } else {
            userReference = this.xWikiUserBridge.resolveUser(username);
        }
        if (userReference != null && this.xWikiUserBridge.isExistingUser(userReference)) {
            result = this.actorHandler.getActor(userReference);
        }
        return result;
    }

    @Override
    public AbstractActor resolveActivityPubUser(String username) throws WebfingerException
    {
        AbstractActor result = null;
        try {
            if (username.contains(this.activityPubIdentifierService.getWikiSeparator())) {
                String[] split = username.split(WIKI_SEPARATOR_SPLIT_REGEX);
                if (split.length == 2) {
                    String firstPart = split[0];
                    String secondPart = split[1];

                    // we are in a case foo.xwiki: we need to resolve a subwiki actor
                    if (this.activityPubIdentifierService.getWikiIdentifier().equals(secondPart)) {
                        result = this.resolveWikiActor(firstPart);
                        // we are in the case identifier.subwiki: we look for the subwiki and then for the actor in it
                    } else {
                        result = this.resolveUserActor(firstPart, secondPart);
                    }
                }
            } else {
                result = this.resolveUserActor(username, null);
            }
        } catch (ActivityPubException | WikiManagerException e) {
            throw new WebfingerException(String.format("Error while resolving username [%s].", username), e);
        }

        return result;
    }

    @Override
    public URI resolveXWikiUserUrl(AbstractActor actor) throws WebfingerException
    {
        URI uri;
        try {
            if (actor instanceof Person) {
                UserReference xWikiUserReference = this.actorHandler.getXWikiUserReference((Person) actor);
                uri = this.xWikiUserBridge.getUserProfileURL(xWikiUserReference).toURI();
            } else if (actor instanceof Service) {
                WikiReference wikiReference = this.actorHandler.getXWikiWikiReference((Service) actor);
                String wikiName = StringUtils.substringBeforeLast(wikiReference.getName(), ".");
                WikiDescriptor byId = this.wikiDescriptorManager.getById(wikiName);
                DocumentReference mainPageReference = byId.getMainPageReference();
                uri = new URL(((XWikiDocument) this.documentAccess.getDocumentInstance(mainPageReference))
                    .getExternalURL("view", this.contextProvider.get())).toURI();
            } else {
                throw new IllegalArgumentException(String.format(ACTOR_TYPE_ERROR, actor.getType()));
            }
        } catch (Exception e) {
            throw new WebfingerException(String.format("Error while getting profile URL for user [%s]", actor),
                e);
        }

        return uri;
    }
}
