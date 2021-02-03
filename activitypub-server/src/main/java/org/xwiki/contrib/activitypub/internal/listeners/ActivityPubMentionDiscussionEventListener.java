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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.discussions.DiscussionContextService;
import org.xwiki.contrib.discussions.DiscussionService;
import org.xwiki.contrib.discussions.DiscussionsRightService;
import org.xwiki.contrib.discussions.domain.DiscussionContext;
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static java.util.Collections.singletonList;

/**
 * Listen to the wiki mentions and add the mentioned users as member of the activitypub discussion if mention occurs
 * inside an activitypub discussion.
 *
 * @version $Id$
 * @since 1.5
 */
@Component
@Named(ActivityPubMentionDiscussionEventListener.TYPE)
@Singleton
public class ActivityPubMentionDiscussionEventListener extends AbstractActivityPubMentionEventListener
{
    /**
     * Type of the component.
     */
    public static final String TYPE = "ActivityPubMentionDiscussionEventListener";

    private static final String ACTIVITYPUB = "activitypub";

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<DiscussionService> discussionService;

    @Inject
    private Provider<DiscussionContextService> discussionContextService;

    @Inject
    private Provider<DiscussionsRightService> discussionsRightService;

    @Inject
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @Inject
    private ActorHandler actorHandler;

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
                        List<BaseObject> xObjects =
                            doc.getXObjects(new DocumentReference(this.contextProvider.get().getWikiId(),
                                Arrays.asList("Discussions", "Code"), "MessageClass"));
                        if (!xObjects.isEmpty()) {
                            mentionNotificationParameters.getMentions().entrySet().stream()
                                .filter(it -> it.getKey().equals(ACTIVITYPUB))
                                .forEach(e -> e.getValue().forEach(mrf -> addMentionedUserToDiscussion(
                                    xObjects.get(0).getStringValue("discussionReference"), mrf.getReference())));
                        }
                    });
            }
        }
    }

    private void addMentionedUserToDiscussion(String discussionReference, String mentionedRef)
    {
        getDiscussionService().get(discussionReference).ifPresent(discussion -> {
            List<DiscussionContext> byDiscussionReference =
                getDiscussionContextService().findByDiscussionReference(discussion.getReference());
            boolean isActivitypub = byDiscussionReference.stream()
                .anyMatch(it -> it.getEntityReference().getType().startsWith(ACTIVITYPUB));
            if (isActivitypub) {
                try {
                    AbstractActor actor = this.actorHandler.getActor(mentionedRef);
                    ActivityPubObject activityPubObject =
                        this.activityPubObjectReferenceResolver.resolveReference(actor.getReference());
                    String actorId = activityPubObject.getId().toASCIIString();
                    getDiscussionContextService().getOrCreate("", "", "activitypub-actor", actorId).ifPresent(dc -> {
                            getDiscussionContextService().link(dc, discussion);
                            if (this.actorHandler.isLocalActor(actor)) {
                                try {
                                    DocumentReference storeDocument = this.actorHandler.getStoreDocument(actor);
                                    getDiscussionsRightService().setRead(discussion, storeDocument);
                                    getDiscussionsRightService().setWrite(discussion, storeDocument);
                                } catch (ActivityPubException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    );
                } catch (ActivityPubException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private DiscussionsRightService getDiscussionsRightService()
    {
        return this.discussionsRightService.get();
    }

    private DiscussionContextService getDiscussionContextService()
    {
        return this.discussionContextService.get();
    }

    private DiscussionService getDiscussionService()
    {
        return this.discussionService.get();
    }
}
