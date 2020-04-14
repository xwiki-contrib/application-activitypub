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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.user.UserReference;
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
    private static final String WIKI_IDENTIFIER = "xwiki";

    private static final String WIKI_SEPARATOR = ".";

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
    private DefaultURLHandler urlHandler;

    private Service resolveWikiActor(String wikiName) throws WikiManagerException, ActivityPubException
    {
        boolean wikiExist;
        WikiReference wikiReference;
        Service result = null;

        // we are in the case xwiki.xwiki: we return the current wiki actor
        if (WIKI_IDENTIFIER.equals(wikiName)) {
            wikiExist = true;
            wikiReference = contextProvider.get().getWikiReference();
        // we are in the case subwiki.wiki: we check if the subwiki exist before returning it.
        } else {
            wikiExist = wikiDescriptorManager.exists(wikiName);
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
        UserReference userReference = null;
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
        XWikiContext context = this.contextProvider.get();
        try {
            if (username.contains(WIKI_SEPARATOR)) {
                String[] split = username.split(WIKI_SEPARATOR_SPLIT_REGEX);
                if (split.length == 2) {
                    String firstPart = split[0];
                    String secondPart = split[1];

                    // we are in a case foo.xwiki: we need to resolve a subwiki actor
                    if (WIKI_IDENTIFIER.equals(secondPart)) {
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
        URI uri = null;
        try {
            if (actor instanceof Person) {
                UserReference xWikiUserReference = this.actorHandler.getXWikiUserReference((Person) actor);
                uri = this.xWikiUserBridge.getUserProfileURL(xWikiUserReference).toURI();
            } else if (actor instanceof Service) {
                WikiReference wikiReference = this.actorHandler.getXWikiWikiReference((Service) actor);
                DocumentReference mainPageReference =
                    this.wikiDescriptorManager.getById(wikiReference.getName()).getMainPageReference();
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

    private String getPrefixIdentifier(String login, String wikiName)
    {
        String prefix;
        String fullPrefixFormat = String.format("%%s%s%%s", WIKI_SEPARATOR);
        String currentWikiId = this.wikiDescriptorManager.getCurrentWikiId();
        boolean isWiki = login == null;
        boolean isCurrentWiki = (currentWikiId.equals(wikiName));

        String cleanLogin = null;
        if (login != null && login.contains("XWiki.")) {
            cleanLogin = login.split("XWiki\\.")[1];
        } else {
            cleanLogin = login;
        }
        if (isWiki && isCurrentWiki) {
            prefix = String.format(fullPrefixFormat, WIKI_IDENTIFIER, WIKI_IDENTIFIER);
        } else if (isWiki) {
            prefix = String.format(fullPrefixFormat, wikiName, WIKI_IDENTIFIER);
        } else if (!isCurrentWiki) {
            prefix = String.format(fullPrefixFormat, cleanLogin, wikiName);
        } else {
            prefix = cleanLogin;
        }

        return prefix;
    }

    @Override
    public String getWebFingerIdentifier(AbstractActor actor) throws WebfingerException
    {
        try {
            String identifier = String.format("%%s@%s", this.urlHandler.getServerUrl().getHost());
            if (this.urlHandler.belongsToCurrentInstance(actor.getId())) {
                String prefix;

                if (actor instanceof Person) {
                    UserReference xWikiUserReference = this.actorHandler.getXWikiUserReference((Person) actor);
                    DocumentReference documentReference = this.xWikiUserBridge.getDocumentReference(xWikiUserReference);
                    String userWiki = documentReference.getWikiReference().getName();

                    prefix = this.getPrefixIdentifier(actor.getPreferredUsername(), userWiki);
                } else if (actor instanceof Service) {
                    WikiReference xWikiWikiReference = this.actorHandler.getXWikiWikiReference((Service) actor);

                    prefix = this.getPrefixIdentifier(null, xWikiWikiReference.getName());
                } else {
                    throw new IllegalArgumentException(String.format(ACTOR_TYPE_ERROR, actor.getType()));
                }
                return String.format(identifier, prefix);
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        "We only resolve WebFinger identifier for the current instance, and [%s] does not belong "
                            + "to it.", actor.getId()));
            }
        } catch (MalformedURLException | IllegalArgumentException | ActivityPubException e) {
            throw new WebfingerException(String.format("Cannot resolve the WebFinger identifier for user [%s]", actor),
                e);
        }
    }
}
