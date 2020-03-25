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

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.user.UserReference;

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
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private XWikiUserBridge userBridge;

    @Inject
    private Logger logger;

    /**
     * Return the webfinger id of the user on the current server.
     * @param user the user name.
     * @return The webfinger id.
     */
    public String getWebfingerId(DocumentReference user)
    {
        try {
            String userLogin = this.userBridge.getUserLogin(this.userBridge.resolveDocumentReference(user));
            XWikiContext context = this.contextProvider.get();
            URL url = context.getURLFactory().getServerURL(context);
            int port = url.getPort();
            if (port != 80 && port > 0) {
                return String.format("%s@%s:%d", userLogin, url.getHost(), port);
            } else {
                return String.format("%s@%s", userLogin, url.getHost());
            }
        } catch (MalformedURLException e) {
            this.logger.warn("Can't resolve the server URL. Cause [{}]", ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
