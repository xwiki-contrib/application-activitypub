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
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Mention;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Update;
import org.xwiki.mentions.DisplayStyle;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionsFormatter;
import org.xwiki.mentions.internal.MentionFormatterProvider;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.activitypub.ActivityPubConfiguration.ACTIVITYPUB_MENTION_TYPE;

/**
 * Test of {@link ActivityPubMentionsSender}.
 *
 * @version $Id$
 * @since 1.4
 */
@ComponentTest
class ActivityPubMentionsSenderTest
{
    public static final String AUTHOR_REFERENCE = "xwiki:XWiki.Author";

    public static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    @InjectMockComponents
    private ActivityPubMentionsSender activityPubMentionsSender;

    @MockComponent
    private ActivityHandler<Create> createActivityHandler;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private UserReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private MentionFormatterProvider mentionFormatterProvider;

    @MockComponent
    private ActivityPubXDOMService activityPubXDOMService;

    @MockComponent
    private DateProvider dateProvider;

    @Mock
    private MentionsFormatter mentionsFormatter;

    private Date currentTime;

    @BeforeEach
    void setUp()
    {
        this.currentTime = new Date();
        when(this.dateProvider.currentTime()).thenReturn(this.currentTime);
    }

    @Test
    void sendNotificationNoActivityPubNewMentions()
    {
        when(this.mentionFormatterProvider.get(ACTIVITYPUB_MENTION_TYPE)).thenReturn(this.mentionsFormatter);

        this.activityPubMentionsSender.sendNotification(
            new MentionNotificationParameters("xwiki:XWiki.Author", new DocumentReference("xwiki", "XWiki", "Doc"),
                MentionLocation.DOCUMENT, "1.6"), mock(XWikiDocument.class),
            URI.create("http://wiki/page/1"));

        verifyNoInteractions(this.createActivityHandler);
        verifyNoInteractions(this.actorHandler);
        verifyNoInteractions(this.userReferenceResolver);
        verifyNoInteractions(this.mentionsFormatter);
    }

    @Test
    void sendNotificationCreatedDocument() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        XWikiDocument doc = mock(XWikiDocument.class);
        URI documentUrl = URI.create("http://wiki/page/1");
        String mentionedActorReference = "@U1@ap.tld";
        MentionNotificationParameters mentionNotificationParameters =
            new MentionNotificationParameters(AUTHOR_REFERENCE, DOCUMENT_REFERENCE, MentionLocation.DOCUMENT, "1.6")
                .addNewMention(ACTIVITYPUB_MENTION_TYPE,
                    new MentionNotificationParameter(mentionedActorReference, "anchor0", DisplayStyle.FIRST_NAME));
        URI u1URI = URI.create("http://s1.org/U1");
        AbstractActor u1Actor = new Person().setName("U1").setId(u1URI);
        URI actorURI = URI.create("http://wiki/Author");
        Person authorActor = new Person().setName("Author").setId(actorURI);

        when(this.mentionFormatterProvider.get(ACTIVITYPUB_MENTION_TYPE)).thenReturn(this.mentionsFormatter);
        when(this.userReferenceResolver.resolve(AUTHOR_REFERENCE)).thenReturn(userReference);
        when(this.actorHandler.getActor(mentionedActorReference)).thenReturn(u1Actor);
        when(this.actorHandler.getActor(userReference)).thenReturn(authorActor);
        when(this.mentionsFormatter.formatMention(mentionedActorReference, DisplayStyle.FIRST_NAME))
            .thenReturn("User1");
        XDOM xdom = new XDOM(asList());
        when(doc.getXDOM()).thenReturn(xdom);
        when(doc.getPreviousVersion()).thenReturn(null);
        this.activityPubMentionsSender.sendNotification(mentionNotificationParameters, doc, documentUrl);

        Mention mention = new Mention()
            .setName("User1");
        List<ProxyActor> to = asList(new ProxyActor(u1URI));
        Page page = new Page()
            .setTo(to)
            .setPublished(this.currentTime)
            .setContent("rendered content")
            .setTag(asList(new ActivityPubObjectReference<>().setObject(mention)))
            .setAttributedTo(asList(new ActivityPubObjectReference<AbstractActor>().setObject(authorActor)))
            .setUrl(asList(documentUrl));
        Create create = new Create()
            .setActor(authorActor)
            .<Create>setPublished(this.currentTime)
            .setObject(page)
            .setTo(to);
        ActivityRequest<Create> activityRequest = new ActivityRequest<>(authorActor, create);

        verify(this.createActivityHandler).handleOutboxRequest(activityRequest);
    }
}
