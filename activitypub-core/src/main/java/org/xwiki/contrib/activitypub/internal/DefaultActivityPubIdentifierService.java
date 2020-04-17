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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubIdentifierService;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link ActivityPubIdentifierService}.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Singleton
public class DefaultActivityPubIdentifierService implements ActivityPubIdentifierService
{
    private static final String ACTOR_TYPE_ERROR = "This actor type is not supported yet [%s]";

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    private String getPrefixIdentifier(String login, String wikiName)
    {
        String fullPrefixFormat = String.format("%%s%s%%s", this.getWikiSeparator());
        String currentWikiId = this.wikiDescriptorManager.getCurrentWikiId();
        boolean isWiki = login == null;
        boolean isCurrentWiki = currentWikiId.equals(wikiName);

        String cleanLogin;
        if (login != null && login.contains("XWiki.")) {
            cleanLogin = login.split("XWiki\\.")[1];
        } else {
            cleanLogin = login;
        }

        String prefix;
        if (isWiki && isCurrentWiki) {
            prefix = String.format(fullPrefixFormat, this.getWikiIdentifier(), this.getWikiIdentifier());
        } else if (isWiki) {
            prefix = String.format(fullPrefixFormat, wikiName, this.getWikiIdentifier());
        } else if (!isCurrentWiki) {
            prefix = String.format(fullPrefixFormat, cleanLogin, wikiName);
        } else {
            prefix = cleanLogin;
        }

        return prefix;
    }

    @Override
    public String createIdentifier(AbstractActor actor, String username, String wikiName) throws ActivityPubException
    {
        String prefix;
        if (actor instanceof Person) {
            prefix = this.getPrefixIdentifier(username, wikiName);
        } else if (actor instanceof Service) {
            prefix = this.getPrefixIdentifier(null, wikiName);
        } else {
            throw new IllegalArgumentException(String.format(ACTOR_TYPE_ERROR, actor.getType()));
        }
        return prefix;
    }
}
