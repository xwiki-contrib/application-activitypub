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

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Link;
import org.xwiki.contrib.activitypub.entities.Mention;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.mentions.MentionsFormatter;
import org.xwiki.mentions.internal.MentionFormatterProvider;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.contrib.activitypub.ActivityPubConfiguration.ACTIVITYPUB_MENTION_TYPE;

/**
 * Internal component dedicated to the emission of mentions to the fediverse.
 *
 * @version $Id$
 * @since 1.4
 */
@Component(roles = { ActivityPubMentionsSender.class })
@Singleton
public class ActivityPubMentionsSender
{
    @Inject
    private ActivityHandler<Create> createActivityHandler;

    @Inject
    private ActivityHandler<Update> updateActivityHandler;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private HTMLRenderer htmlRenderer;

    @Inject
    private MentionFormatterProvider mentionFormatterProvider;

    @Inject
    private Logger logger;

    /**
     * Send the notifications of the mentions to the fediverse actors.
     *
     * @param mentionNotificationParameters the list of mentions to notify.
     * @param doc the document where the mentions occurred
     * @param documentUrl the url of the document where the mentions occurred
     */
    public void sendNotification(MentionNotificationParameters mentionNotificationParameters,
        XWikiDocument doc, URI documentUrl)
    {
        MentionsFormatter mentionsFormatter = this.mentionFormatterProvider.get(ACTIVITYPUB_MENTION_TYPE);

        Set<MentionNotificationParameter> mentionsForActivityPub =
            mentionNotificationParameters.getNewMentions().get(ACTIVITYPUB_MENTION_TYPE);

        if (mentionsForActivityPub != null) {

            try {
                UserReference authorUserReference =
                    this.userReferenceResolver.resolve(mentionNotificationParameters.getAuthorReference());
                AbstractActor authorAbstractActor = this.actorHandler.getActor(authorUserReference);

                List<ProxyActor> to = mentionsForActivityPub
                    .stream()
                    .map(MentionNotificationParameter::getReference)
                    .map(it -> {
                        try {
                            return this.actorHandler.getActor(it);
                        } catch (ActivityPubException e) {
                            this.logger.warn("Cannot resolve actor [{}]. Cause: [{}].", it, getRootCauseMessage(e));
                            return null;
                        }
                    }).filter(Objects::nonNull)
                    .map(AbstractActor::getProxyActor)
                    .collect(Collectors.toList());

                DocumentReference documentReference =
                    (DocumentReference) mentionNotificationParameters.getEntityReference()
                        .extractReference(EntityType.DOCUMENT);
                XDOM xdom = doc.getXDOM();
                List<ActivityPubObjectReference<?>> mentions =
                    mentionsForActivityPub
                        .stream()
                        .map(mentionNotificationParameter -> {
                            try {
                                String reference = mentionNotificationParameter.getReference();
                                AbstractActor actor = this.actorHandler.getActor(reference);
                                return new Mention()
                                    .setHref(actor.getReference().getLink())
                                    .<Link>setName(mentionsFormatter
                                        .formatMention(mentionNotificationParameter.getReference(),
                                            mentionNotificationParameter.getDisplayStyle()));
                            } catch (ActivityPubException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .map(ActivityPubObject::getReference)
                        .collect(Collectors.toList());
                String content = this.htmlRenderer.render(xdom, documentReference);
                AbstractActivity abstractActivity = initActivity(doc)
                    .setActor(authorAbstractActor)
                    .<AbstractActivity>setTo(to)
                    .setObject(new Page()
                        .setName(doc.getTitle())
                        .setUrl(singletonList(documentUrl))
                        .setAttributedTo(singletonList(authorAbstractActor.getReference()))
                        .setTo(to)
                        .setContent(content)
                        .<Page>setTag(mentions));
                if (abstractActivity instanceof Create) {
                    this.createActivityHandler
                        .handleOutboxRequest(
                            new ActivityRequest<>(authorAbstractActor, (Create) abstractActivity));
                } else if (abstractActivity instanceof Update) {
                    this.updateActivityHandler
                        .handleOutboxRequest(
                            new ActivityRequest<>(authorAbstractActor, (Update) abstractActivity));
                }
            } catch (Exception e) {
                this.logger.warn("A error occurred while sending ActivityPub mentions for [{}]. Cause: [{}].",
                    mentionNotificationParameters, getRootCauseMessage(e));
            }
        }
    }

    private AbstractActivity initActivity(XWikiDocument doc)
    {
        AbstractActivity abstractActivity;
        if (doc.getPreviousVersion() != null) {
            abstractActivity = new Update();
        } else {
            abstractActivity = new Create();
        }
        return abstractActivity;
    }
}
