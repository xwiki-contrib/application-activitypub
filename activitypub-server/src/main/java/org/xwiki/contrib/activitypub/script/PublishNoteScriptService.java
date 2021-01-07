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
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.internal.ActivityPubDiscussionsService;
import org.xwiki.contrib.activitypub.internal.DateProvider;
import org.xwiki.contrib.activitypub.internal.script.ActivityPubScriptServiceActor;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;
import org.xwiki.wysiwyg.converter.HTMLConverter;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Component dedicated to the note sending operations for {@link ActivityPubScriptService}.
 *
 * @version $Id$
 * @since 1.5
 */
@Component(roles = { PublishNoteScriptService.class })
@Singleton
@Unstable
public class PublishNoteScriptService
{
    @Inject
    private ActivityPubScriptServiceActor activityPubScriptServiceActor;

    @Inject
    private DateProvider dateProvider;

    @Inject
    private ActivityPubStorage activityPubStorage;

    @Inject
    private Logger logger;

    @Inject
    private HTMLConverter htmlConverter;

    @Inject
    private ActivityPubDiscussionsService activityPubDiscussionsService;

    /**
     * Publish the given content as a note to be send to the addressed target. The given targets can take different
     * values:
     * <ul>
     * <li>followers: means that the note will be sent to the followers</li>
     * <li>
     *     an URI qualifying an actor: means that the note will be sent to that actor If the list of targets is empty
     *     or null, it means the note will be private only.</li>
     * </ul>
     *
     * @param targets the list of targets for the note (see below for information about accepted values)
     * @param content the actual content of the note
     * @return {@code true} if everything went well, else return false
     */
    public boolean publishNote(List<String> targets, String content)
    {
        return publishNote(targets, content, null);
    }

    /**
     * Publish the given content as a note to be send to the addressed target. The given targets can take different
     * values:
     * <ul>
     * <li>followers: means that the note will be sent to the followers</li>
     * <li>
     *     an URI qualifying an actor: means that the note will be sent to that actor If the list of targets is empty
     *     or null, it means the note will be private only.</li>
     * </ul>
     *
     * @param targets the list of targets for the note (see below for information about accepted values)
     * @param content the actual content of the note
     * @param sourceActor the actor responsible from this message: if null, the current actor will be used
     * @return {@code true} if everything went well, else return false
     * @since 1.2
     */
    @Unstable
    public boolean publishNote(List<String> targets, String content, AbstractActor sourceActor)
    {
        return publishNote(targets, content, sourceActor, null);
    }

    /**
     * Publish the given content as a note to be send to the addressed target. The given targets can take different
     * values:
     * <ul>
     * <li>followers: means that the note will be sent to the followers</li>
     * <li>
     *     an URI qualifying an actor: means that the note will be sent to that actor If the list of targets is empty
     *     or null, it means the note will be private only.</li>
     * </ul>
     *
     * @param targets the list of targets for the note (see below for information about accepted values)
     * @param rawContent the actual content of the note
     * @param sourceActor the actor responsible from this message: if null, the current actor will be used
     * @param syntax the syntax of the rawContent
     * @return {@code true} if everything went well, else return false
     * @since 1.5
     */
    @Unstable
    public boolean publishNote(List<String> targets, String rawContent, AbstractActor sourceActor, String syntax)
    {
        try {

            AbstractActor currentActor = this.activityPubScriptServiceActor.getSourceActor(sourceActor);

            Date currentTime = this.dateProvider.currentTime();
            Note note = new Note()
                .setAttributedTo(singletonList(currentActor.getReference()))
                .setContent(convertContentToHtml(rawContent, syntax))
                .setPublished(currentTime);
            this.activityPubScriptServiceActor.fillRecipients(targets, currentActor, note);
            this.activityPubStorage.storeEntity(note);

            Create create = new Create()
                .setActor(currentActor)
                .setObject(note)
                .setAttributedTo(note.getAttributedTo())
                .setTo(note.getTo())
                .setPublished(currentTime);
            this.activityPubStorage.storeEntity(create);

            create.getObject().setExpand(true);
            this.activityPubScriptServiceActor.getActivityHandler(create)
                .handleOutboxRequest(new ActivityRequest<>(currentActor, create));
            this.activityPubDiscussionsService.handleActivity(create, false);
            return true;
        } catch (IOException | ActivityPubException e) {
            this.logger.error("Error while posting a note.", e);
            return false;
        }
    }

    private String convertContentToHtml(String rawContent, String syntax)
    {
        String content;
        if (syntax == null) {
            content = rawContent;
        } else {
            try {
                content = this.htmlConverter.toHTML(rawContent, Syntax.valueOf(syntax), null);
            } catch (ParseException e) {
                this.logger.debug("Unknwon syntax [{}]. Cause: [{}].", syntax, getRootCauseMessage(e));
                content = rawContent;
            }
        }
        return content;
    }
}
