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
package org.xwiki.contrib.activitypub.script;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Follow;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;

@Component
@Singleton
@Named("activitypub")
public class ActivityPubScriptService implements ScriptService
{
    @Inject
    private ActivityPubClient activityPubClient;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ActivityPubJsonParser jsonParser;

    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private Logger logger;

    public boolean follow(String profileURL)
    {
        boolean result = false;

        try {
            AbstractActor remoteActor = this.actorHandler.getRemoteActor(profileURL);
            AbstractActor currentActor = this.actorHandler.getCurrentActor();
            Follow follow = new Follow().setActor(currentActor).setObject(remoteActor);
            this.activityPubStorage.storeEntity(follow);
            HttpMethod httpMethod = this.activityPubClient.postInbox(remoteActor, follow);
            this.activityPubClient.checkAnswer(httpMethod);
            result = true;
        } catch (ActivityPubException e) {
            this.logger.error("Error while trying to send a follow request to [{}].", profileURL, e);
        }

        return result;
    }
}
