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
package org.xwiki.contrib.activitypub.webfinger.script;

import java.net.URL;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.webfinger.WebfingerClient;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;

/**
 * Script service for Webfinger.
 *
 * @since 1.1
 * @version $Id$
 */
@Component
@Singleton
@Named("webfinger")
public class WebfingerScriptService implements ScriptService
{
    private static Boolean webfingerConfigured;

    @Inject
    private Provider<XWikiContext> contextProvider;
    
    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private Logger logger;

    @Inject
    private WebfingerClient webfingerClient;

    private void testWebfingerConfiguration()
    {
        try {
            XWikiContext context = this.contextProvider.get();
            URL url = context.getURLFactory().getServerURL(context);
            int port = url.getPort();
            String domain;
            if (port != 80 && port > 0) {
                domain = String.format("%s:%d", url.getHost(), port);
            } else {
                domain = String.format("%s", url.getHost());
            }
            webfingerConfigured = this.webfingerClient.testWebFingerConfiguration(domain);
        } catch (Exception e) {
            logger.debug("Error while testing webfinger configuration.", e);
            webfingerConfigured = false;
        }
    }

    /**
     * @return {@code true} iff the current server has a working implementation of WebFinger.
     */
    public boolean isWebfingerConfigured()
    {
        if (webfingerConfigured == null) {
            testWebfingerConfiguration();
        }

        return webfingerConfigured;
    }

    /**
     * Search for existing Webfinger identifiers.
     *
     * @param partialId a partial identifier use to perform a request.
     * @param limit a limit number of result.
     * @return the list of matching Webfinger records stored locally.
     * @since 1.2
     */
    @Unstable
    public List<JSONResourceDescriptor> queryExistingIdentifier(String partialId, int limit)
    {
        try {
            return this.activityPubStorage.searchWebFinger(partialId, limit);
        } catch (ActivityPubException e) {
            logger.error("Error while performing Webfinger query", e);
            return Collections.emptyList();
        }
    }
}
