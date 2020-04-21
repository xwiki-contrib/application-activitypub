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
package org.xwiki.contrib.activitypub.internal.listeners;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubConfiguration;
import org.xwiki.contrib.activitypub.internal.async.PageChangedRequest;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.contrib.activitypub.internal.async.jobs.PageUpdatedNotificationJob.ASYNC_REQUEST_TYPE;

/**
 * Listen for {@link DocumentUpdatedEvent} and notify the followers of the updater with ActivityPub protocol.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Singleton
@Named("ActivityPubDocumentUpdatedEventListener")
public class DocumentUpdatedEventListener extends AbstractEventListener
{
    private static final List<Event> EVENTS = singletonList(new DocumentUpdatedEvent());

    private static final String ERROR_MSG = "ActivityPub page update [{}] event task failed. Cause [{}]";

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private ActivityPubConfiguration configuration;

    /**
     * Default constructor.
     */
    public DocumentUpdatedEventListener()
    {
        super("ActivityPubDocumentUpdatedEventListener", EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.configuration.isPagesNotification()) {
            XWikiDocument document = (XWikiDocument) source;
            if (!Boolean.TRUE.equals(document.isHidden())) {
                XWikiContext context = (XWikiContext) data;

                try {
                    XWikiRCSNodeInfo revisionInfo = document.getRevisionInfo(document.getVersion(), context);
                    if (!revisionInfo.isMinorEdit()) {
                        PageChangedRequest ret = this.newRequest(document, context);
                        this.jobExecutor.execute(ASYNC_REQUEST_TYPE, ret);
                    }
                } catch (XWikiException | JobException e) {
                    this.logger.warn(ERROR_MSG, document, getRootCauseMessage(e));
                }
            }
        }
    }

    private PageChangedRequest newRequest(XWikiDocument document, XWikiContext context)
    {
        /*
         * XWikiDocument is not serializable and cannot be passed safely to the job executor.
         * Only interesting parameters are passed explicitly on the request.
         */
        PageChangedRequest ret =
            new PageChangedRequest()
                .setDocumentReference(document.getDocumentReference())
                .setAuthorReference(document.getAuthorReference())
                .setDocumentTitle(document.getTitle())
                .setContent(document.getXDOM())
                .setCreationDate(document.getCreationDate())
                .setViewURL(document.getURL("view", context));
        ret.setId(ASYNC_REQUEST_TYPE, document.getKey());
        return ret;
    }
}
