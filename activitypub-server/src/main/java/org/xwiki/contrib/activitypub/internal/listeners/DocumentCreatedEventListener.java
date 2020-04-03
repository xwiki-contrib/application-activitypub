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

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.internal.async.PageCreatedRequest;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.xwiki.contrib.activitypub.internal.async.PageCreatedNotificationJob.ASYNC_REQUEST_TYPE;

/**
 * Listen for {@link DocumentCreatedEvent} and notify the followers of the creator with ActivityPub protocol.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named("ActivityPubDocumentCreatedEventListener")
public class DocumentCreatedEventListener extends AbstractEventListener
{
    private static final List<Event> EVENTS = Arrays.asList(new DocumentCreatedEvent());

    @Inject
    private Logger logger;

    @Inject
    private JobExecutor jobExecutor;

    /**
     * Default constructor.
     */
    public DocumentCreatedEventListener()
    {
        super("ActivityPubDocumentCreatedEventListener", EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        Request createJob = this.newRequest(document, context);

        try {
            this.jobExecutor.execute(ASYNC_REQUEST_TYPE, createJob);
        } catch (JobException e) {
            this.logger.warn("ActivityPub page creation [{}] event task failed. Cause [{}]", document,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private Request newRequest(XWikiDocument document, XWikiContext context)
    {
        /*
         * XWikiDocument is not serializable and cannot be passed safely to the job executor.
         * Only interesting parameters are passed explicitly on the request.
         */
        DocumentReference documentReference = document.getDocumentReference();
        DocumentReference authorReference = document.getAuthorReference();
        String viewURL = document.getURL("view", context);
        String documentTitle = document.getTitle();
        Date creationDate = document.getCreationDate();

        PageCreatedRequest ret =
            new PageCreatedRequest(documentReference, authorReference, viewURL, documentTitle, creationDate);
        ret.setId(ASYNC_REQUEST_TYPE, document.getKey());
        return ret;
    }
}
