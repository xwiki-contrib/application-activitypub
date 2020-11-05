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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Event listener for the mentions to actors for the fediverse.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named(ActivityPubMentionEventListener.TYPE)
@Singleton
public class ActivityPubMentionEventListener implements EventListener
{
    /**
     * Type of the component.
     */
    public static final String TYPE = "ActivityPubMentionEventListener";

    @Inject
    private ActivityPubMentionsSender activityPubMentionsSender;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private DefaultURLHandler urlHandler;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return TYPE;
    }

    @Override
    public List<Event> getEvents()
    {
        return singletonList(new NewMentionsEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (data instanceof MentionNotificationParameters) {
            MentionNotificationParameters mentionNotificationParameters = (MentionNotificationParameters) data;
            if (this.authorizationManager
                .hasAccess(Right.VIEW, null, mentionNotificationParameters.getEntityReference()))
            {
                this.resolveDoc(mentionNotificationParameters)
                    .ifPresent((XWikiDocument doc) -> {
                        try {
                            this.activityPubMentionsSender
                                .sendNotification(mentionNotificationParameters, doc,
                                    this.urlHandler.getAbsoluteURI(
                                        URI.create(doc.getURL("view", this.contextProvider.get()))));
                        } catch (MalformedURLException | URISyntaxException e) {
                            this.logger
                                .warn("A error occurred while sending ActivityPub mentions for [{}]. Cause: [{}].",
                                    mentionNotificationParameters, getRootCauseMessage(e));
                        }
                    });
            }
        }
    }

    private Optional<XWikiDocument> resolveDoc(MentionNotificationParameters mentionNotificationParameters)
    {
        Optional<XWikiDocument> xWikiDocument;
        EntityReference document =
            mentionNotificationParameters.getEntityReference().extractReference(EntityType.DOCUMENT);
        String version = mentionNotificationParameters.getVersion();
        try {
            xWikiDocument =
                Optional.of(this.documentRevisionProvider.getRevision((DocumentReference) document, version));
        } catch (XWikiException e) {
            this.logger
                .warn("Failed to resolve [{}] in version [{}]. Cause: [{}]", document, version, getRootCauseMessage(e));
            xWikiDocument = Optional.empty();
        }
        return xWikiDocument;
    }
}
