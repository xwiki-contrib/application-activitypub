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

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.httpclient.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubClient;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.ActivityPubStorage;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.HTMLRenderer;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.contrib.activitypub.entities.ActivityPubObjectReference;
import org.xwiki.contrib.activitypub.entities.Announce;
import org.xwiki.contrib.activitypub.entities.Create;
import org.xwiki.contrib.activitypub.entities.Like;
import org.xwiki.contrib.activitypub.entities.Note;
import org.xwiki.contrib.activitypub.entities.OrderedCollection;
import org.xwiki.contrib.activitypub.entities.Page;
import org.xwiki.contrib.activitypub.entities.Person;
import org.xwiki.contrib.activitypub.entities.ProxyActor;
import org.xwiki.contrib.activitypub.entities.Service;
import org.xwiki.contrib.activitypub.internal.DefaultURLHandler;
import org.xwiki.contrib.activitypub.internal.InternalURINormalizer;
import org.xwiki.contrib.activitypub.internal.activities.CreateActivityHandler;
import org.xwiki.contrib.activitypub.internal.activities.LikeActivityHandler;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.contrib.activitypub.script.ActivityPubScriptService.DateProvider;
import static org.xwiki.test.LogLevel.ERROR;

/**
 * Tests for {@link ActivityPubScriptService}.
 *
 * @version $Id$
 * @since 1.1
 */
@ComponentTest
class ActivityPubScriptServiceTest
{
    private static final DocumentReference GUEST_USER =
        new DocumentReference("xwiki", "XWiki", XWikiRightService.GUEST_USER);

    @InjectMockComponents
    private ActivityPubScriptService scriptService;

    @MockComponent
    private ActorHandler actorHandler;

    @MockComponent
    private ActivityPubClient activityPubClient;

    @MockComponent
    private ActivityPubStorage activityPubStorage;

    @MockComponent
    private ActivityPubObjectReferenceResolver activityPubObjectReferenceResolver;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> userReferenceResolver;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private HTMLRenderer htmlRenderer;

    @MockComponent
    private DefaultURLHandler urlHandler;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @MockComponent
    private InternalURINormalizer internalURINormalizer;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(ERROR);

    @Test
    void follow() throws Exception
    {
        Person remoteActor = mock(Person.class);
        Person currentActor = mock(Person.class);

        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(remoteActor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(currentActor);

        AbstractActor targetActor = mock(AbstractActor.class);
        ActivityPubObjectReference targetActorReference = mock(ActivityPubObjectReference.class);
        when(targetActor.getReference()).thenReturn(targetActorReference);
        when(this.actorHandler.getActor("test")).thenReturn(targetActor);

        when(this.activityPubClient.postInbox(any(), any())).thenReturn(mock(HttpMethod.class));
        FollowResult actual = this.scriptService.follow(remoteActor);
        assertTrue(actual.isSuccess());
        assertEquals("activitypub.follow.followRequested", actual.getMessage());
        verify(this.activityPubStorage, times(1)).storeEntity(any());
        verify(this.activityPubClient, times(1)).postInbox(eq(remoteActor), any());
        verify(this.activityPubClient, times(1)).checkAnswer(any());
    }

    @Test
    void followYourself() throws Exception
    {
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        AbstractActor targetActor = mock(AbstractActor.class);
        ActivityPubObjectReference targetActorReference = mock(ActivityPubObjectReference.class);
        when(targetActor.getReference()).thenReturn(targetActorReference);
        when(this.actorHandler.getActor("test")).thenReturn(targetActor);

        when(this.activityPubClient.postInbox(any(), any())).thenReturn(mock(HttpMethod.class));
        FollowResult actual = this.scriptService.follow(actor);
        assertFalse(actual.isSuccess());
        assertEquals("activitypub.follow.followYourself", actual.getMessage());
        verify(this.activityPubStorage, times(0)).storeEntity(any());
        verify(this.activityPubClient, times(0)).postInbox(eq(actor), any());
        verify(this.activityPubClient, times(0)).checkAnswer(any());
    }

    @Test
    void followAlreadyFollowing() throws Exception
    {
        Person actor = mock(Person.class);
        Person current = mock(Person.class);
        ActivityPubObjectReference mock = mock(ActivityPubObjectReference.class);
        when(current.getFollowing()).thenReturn(mock);
        OrderedCollection orderedCollection = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(mock)).thenReturn(orderedCollection);
        List list1 = mock(List.class);
        when(orderedCollection.getOrderedItems()).thenReturn(list1);
        Stream stream1 = mock(Stream.class);
        when(list1.stream()).thenReturn(stream1);
        Stream stream2 = mock(Stream.class);
        when(stream1.map(any())).thenReturn(stream2);
        when(stream2.anyMatch(any())).thenReturn(true);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(current);

        AbstractActor targetActor = mock(AbstractActor.class);
        ActivityPubObjectReference targetActorReference = mock(ActivityPubObjectReference.class);
        when(targetActor.getReference()).thenReturn(targetActorReference);
        when(this.actorHandler.getActor("test")).thenReturn(targetActor);

        when(this.activityPubClient.postInbox(any(), any())).thenReturn(mock(HttpMethod.class));
        FollowResult actual = this.scriptService.follow(actor);
        assertFalse(actual.isSuccess());
        assertEquals("activitypub.follow.alreadyFollowed", actual.getMessage());
        verify(this.activityPubStorage, times(0)).storeEntity(any());
        verify(this.activityPubClient, times(0)).postInbox(eq(actor), any());
        verify(this.activityPubClient, times(0)).checkAnswer(any());
    }

    @Test
    void following() throws Exception
    {
        AbstractActor actor = mock(AbstractActor.class);
        ActivityPubObjectReference apor = mock(ActivityPubObjectReference.class);
        when(actor.getFollowing()).thenReturn(apor);
        when(this.activityPubObjectReferenceResolver.resolveReference(apor)).thenReturn(mock(OrderedCollection.class));
        List<AbstractActor> res = this.scriptService.following(actor);
        assertTrue(res.isEmpty());
    }

    @Test
    void followers() throws Exception
    {
        AbstractActor aa = mock(AbstractActor.class);
        ActivityPubObjectReference apor = mock(ActivityPubObjectReference.class);
        when(aa.getFollowers()).thenReturn(apor);
        when(this.actorHandler.getActor("User.Test")).thenReturn(aa);
        when(this.activityPubObjectReferenceResolver.resolveReference(apor)).thenReturn(mock(
            OrderedCollection.class));
        List<AbstractActor> res = this.scriptService.followers(aa);
        assertTrue(res.isEmpty());
    }

    @Test
    public void publishNoteNoTarget() throws Exception
    {
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.componentManager.getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Create.class)))
            .thenReturn(activityHandler);
        String noteContent = "some content";
        this.scriptService.publishNote(null, noteContent);
        Note note = new Note()
            .setContent(noteContent)
            .setAttributedTo(Collections.singletonList(actor.getReference()));

        ArgumentCaptor<ActivityPubObject> argumentCaptor = ArgumentCaptor.forClass(ActivityPubObject.class);
        verify(this.activityPubStorage, times(2)).storeEntity(argumentCaptor.capture());
        List<ActivityPubObject> allValues = argumentCaptor.getAllValues();
        assertEquals(2, allValues.size());
        assertTrue(allValues.get(0) instanceof Note);
        assertTrue(allValues.get(1) instanceof Create);
        Note obtainedNote = (Note) allValues.get(0);
        assertNotNull(obtainedNote.getPublished());
        obtainedNote.setPublished(null);
        assertEquals(note, allValues.get(0));

        Create create = (Create) allValues.get(1);
        assertEquals(note, create.getObject().getObject());
        assertSame(actorReference, create.getActor());
        assertEquals(note.getAttributedTo(), create.getAttributedTo());
        assertEquals(note.getTo(), create.getTo());
        assertNotNull(create.getPublished());
        verify(activityHandler).handleOutboxRequest(new ActivityRequest<>(actor, create));
    }

    @Test
    public void publishNoteFollowersAndActor() throws Exception
    {
        Person actor = mock(Person.class);
        ActivityPubObjectReference actorReference = mock(ActivityPubObjectReference.class);
        when(actor.getReference()).thenReturn(actorReference);

        ActivityPubObjectReference followersReference = mock(ActivityPubObjectReference.class);
        when(followersReference.getLink()).thenReturn(new URI("http://followers"));
        when(actor.getFollowers()).thenReturn(followersReference);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.componentManager.getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Create.class)))
            .thenReturn(activityHandler);

        AbstractActor targetActor = mock(AbstractActor.class);
        ProxyActor targetProxyActor = mock(ProxyActor.class);
        when(targetActor.getProxyActor()).thenReturn(targetProxyActor);
        when(actorHandler.getActor("@targetActor")).thenReturn(targetActor);

        String noteContent = "some content";
        this.scriptService.publishNote(Arrays.asList("followers", "@targetActor"), noteContent);

        Note note = new Note()
            .setContent(noteContent)
            .setAttributedTo(Collections.singletonList(actor.getReference()))
            .setTo(Arrays.asList(new ProxyActor(followersReference.getLink()), targetProxyActor));

        ArgumentCaptor<ActivityPubObject> argumentCaptor = ArgumentCaptor.forClass(ActivityPubObject.class);
        verify(this.activityPubStorage, times(2)).storeEntity(argumentCaptor.capture());
        List<ActivityPubObject> allValues = argumentCaptor.getAllValues();
        assertEquals(2, allValues.size());
        assertTrue(allValues.get(0) instanceof Note);
        assertTrue(allValues.get(1) instanceof Create);
        Note obtainedNote = (Note) allValues.get(0);
        assertNotNull(obtainedNote.getPublished());
        obtainedNote.setPublished(null);
        assertEquals(note, allValues.get(0));

        Create create = (Create) allValues.get(1);
        assertEquals(note, create.getObject().getObject());
        assertSame(actorReference, create.getActor());
        assertEquals(note.getAttributedTo(), create.getAttributedTo());
        assertEquals(note.getTo(), create.getTo());
        assertNotNull(create.getPublished());
        verify(activityHandler).handleOutboxRequest(new ActivityRequest<>(actor, create));
    }

    @Test
    public void isCurrentUser() throws ActivityPubException
    {
        Person actor = mock(Person.class);
        UserReference userReference = mock(UserReference.class);

        when(this.userReferenceResolver.resolve(null)).thenReturn(userReference);
        when(this.actorHandler.getXWikiUserReference(actor)).thenReturn(userReference);
        assertTrue(this.scriptService.isCurrentUser(actor));

        when(this.userReferenceResolver.resolve(null)).thenReturn(mock(UserReference.class));
        assertFalse(this.scriptService.isCurrentUser(actor));
    }

    @Test
    public void currentUserCanActFor() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        Person person = mock(Person.class);

        when(this.userReferenceResolver.resolve(null)).thenReturn(userReference);
        when(this.actorHandler.isAuthorizedToActFor(userReference, person)).thenReturn(true);
        assertTrue(this.scriptService.currentUserCanActFor(person));

        when(this.actorHandler.isAuthorizedToActFor(userReference, person)).thenReturn(false);
        assertFalse(this.scriptService.currentUserCanActFor(person));

        when(this.userReferenceResolver.resolve(null)).thenReturn(GuestUserReference.INSTANCE);
        assertFalse(this.scriptService.currentUserCanActFor(person));
        verify(this.actorHandler, times(2)).isAuthorizedToActFor(any(), any());
    }

    @Test
    public void getCurrentWikiActor() throws Exception
    {
        XWikiContext context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(context);
        WikiReference wikiReference = new WikiReference("foobar");
        when(context.getWikiReference()).thenReturn(wikiReference);
        Service service = mock(Service.class);
        when(this.actorHandler.getActor(wikiReference)).thenReturn(service);

        assertSame(service, this.scriptService.getCurrentWikiActor());
    }

    @Test
    void sharePageSuccess() throws Exception
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.MyPage"))
            .thenReturn(documentReference);

        Person currentActor = new Person();
        when(this.actorHandler.getCurrentActor()).thenReturn(currentActor);

        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.componentManager
            .getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Announce.class)))
            .thenReturn(activityHandler);

        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        XWiki xWiki = mock(XWiki.class);
        when(xWikiContext.getWiki()).thenReturn(xWiki);
        XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        when(xWiki.getDocument(documentReference, xWikiContext)).thenReturn(xwikiDoc);
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);
        when(xwikiDoc.getTitle()).thenReturn("Doc Title");
        when(xwikiDoc.getURL("view", xWikiContext)).thenReturn("http://wiki/view/page");
        when(xwikiDoc.getContentUpdateDate()).thenReturn(new Date(200));
        when(this.urlHandler.getAbsoluteURI(URI.create("http://wiki/view/page"))).thenReturn(URI.create(
            "http://wiki/view/page"));
        when(this.htmlRenderer.render(xwikiDoc.getXDOM(), documentReference)).thenReturn("<div>content</div>");
        AbstractActor u1 = new Person().setName("U1").setId(URI.create("http://wiki/person/u1"));
        when(this.actorHandler.getActor("U1")).thenReturn(u1);

        DateProvider dateProvider = mock(DateProvider.class);
        this.scriptService.setDateProvider(dateProvider);
        // fix the date to make it deterministic
        Date currentDate = new Date();
        when(dateProvider.currentTime()).thenReturn(currentDate);

        boolean actual = this.scriptService.sharePage(Collections.singletonList("U1"), "xwiki:XWiki.MyPage");

        assertTrue(actual);
        ActivityPubObject page = new Page()
            .setName("Doc Title")
            .setAttributedTo(Collections.singletonList(currentActor.getReference()))
            .setUrl(Collections.singletonList(URI.create("http://wiki/view/page")))
            .setContent("<div>content</div>")
            .setTo(Collections.singletonList(u1.getProxyActor()))
            .setPublished(new Date(200));
        verify(this.activityPubStorage).storeEntity(page);
        verify(this.activityPubStorage).storeEntity(new Announce()
            .setActor(currentActor)
            .setObject(page)
            .setAttributedTo(Collections.singletonList(currentActor.getReference()))
            .setPublished(currentDate)
            .<Announce>setTo(Collections.singletonList(u1.getProxyActor())));
        verify(activityHandler).handleOutboxRequest(any(ActivityRequest.class));
    }

    @Test
    void sharePageError() throws Exception
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.MyPage"))
            .thenReturn(documentReference);

        when(this.actorHandler.getCurrentActor()).thenReturn(new Person());

        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.componentManager
            .getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Announce.class)))
            .thenReturn(activityHandler);

        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        XWiki xWiki = mock(XWiki.class);
        when(xWikiContext.getWiki()).thenReturn(xWiki);
        XWikiDocument xwikiDoc = mock(XWikiDocument.class);
        when(xWiki.getDocument(documentReference, xWikiContext)).thenReturn(xwikiDoc);
        when(this.activityPubStorage.storeEntity(any())).thenThrow(new ActivityPubException("ERR"));
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);
        when(xwikiDoc.getURL("view", xWikiContext)).thenReturn("http://wiki/view/page");

        boolean actual = this.scriptService.sharePage(Collections.singletonList("U1"), "xwiki:XWiki.MyPage");

        assertFalse(actual);
        verify(activityHandler, never()).handleOutboxRequest(any(ActivityRequest.class));
        assertEquals(1, this.logCapture.size());
        assertEquals("Error while sharing a page.", this.logCapture.getMessage(0));
    }

    @Test
    void shareNotLoggedIn() throws Exception
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.MyPage"))
            .thenReturn(documentReference);

        when(this.actorHandler.getCurrentActor()).thenReturn(null);

        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.componentManager
            .getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Announce.class)))
            .thenReturn(activityHandler);

        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        XWiki xWiki = mock(XWiki.class);
        when(xWikiContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(documentReference, xWikiContext)).thenReturn(mock(XWikiDocument.class));
        when(this.activityPubStorage.storeEntity(any())).thenThrow(new ActivityPubException("ERR"));
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(true);

        boolean actual = this.scriptService.sharePage(Collections.singletonList("U1"), "xwiki:XWiki.MyPage");

        assertFalse(actual);
        verify(this.activityPubStorage, never()).storeEntity(any(Page.class));
        verify(this.activityPubStorage, never()).storeEntity(any(Announce.class));
        verify(activityHandler, never()).handleOutboxRequest(any(ActivityRequest.class));
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void shareNoGuestViewRight() throws Exception
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve("xwiki:XWiki.MyPage"))
            .thenReturn(documentReference);

        when(this.actorHandler.getCurrentActor()).thenReturn(new Person());
        ActivityHandler activityHandler = mock(ActivityHandler.class);
        when(this.componentManager
            .getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Announce.class)))
            .thenReturn(activityHandler);

        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        XWiki xWiki = mock(XWiki.class);
        when(xWikiContext.getWiki()).thenReturn(xWiki);
        when(xWiki.getDocument(documentReference, xWikiContext)).thenReturn(mock(XWikiDocument.class));
        when(this.activityPubStorage.storeEntity(any())).thenThrow(new ActivityPubException("ERR"));
        when(this.authorizationManager.hasAccess(Right.VIEW, GUEST_USER, documentReference)).thenReturn(false);

        boolean actual = this.scriptService.sharePage(Collections.singletonList("U1"), "xwiki:XWiki.MyPage");

        assertFalse(actual);
        verify(this.activityPubStorage, never()).storeEntity(any(Page.class));
        verify(this.activityPubStorage, never()).storeEntity(any(Announce.class));
        verify(activityHandler, never()).handleOutboxRequest(any(ActivityRequest.class));
        assertEquals(0, this.logCapture.size());
    }

    @Test
    void isLiked() throws Exception
    {
        Person actor = mock(Person.class);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        String activityId = "http://xwiki/AP/activity/Foo";
        ActivityPubObjectReference<OrderedCollection<ActivityPubObject>> liked = mock(ActivityPubObjectReference.class);
        when(actor.getLiked()).thenReturn(liked);
        OrderedCollection likedCollection = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(liked)).thenReturn(likedCollection);

        AbstractActivity activity = mock(AbstractActivity.class);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(new ActivityPubObjectReference<>().setLink(URI.create(activityId))))
            .thenReturn(activity);
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        when((ActivityPubObjectReference<ActivityPubObject>) activity.getObject())
            .thenReturn(new ActivityPubObjectReference<>().setObject(activityPubObject));
        when(likedCollection.contains(new ActivityPubObjectReference<>().setObject(activityPubObject)))
            .thenReturn(true);

        assertTrue(this.scriptService.isLiked(activityId));
    }

    @Test
    void likeActivityAlreadyLiked() throws Exception
    {
        Person actor = mock(Person.class);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        String activityId = "http://xwiki/AP/activity/Foo";
        ActivityPubObjectReference<OrderedCollection<ActivityPubObject>> liked = mock(ActivityPubObjectReference.class);
        when(actor.getLiked()).thenReturn(liked);
        OrderedCollection likedCollection = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(liked)).thenReturn(likedCollection);
        AbstractActivity activity = mock(AbstractActivity.class);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(new ActivityPubObjectReference<>().setLink(URI.create(activityId))))
            .thenReturn(activity);
        ActivityPubObject activityPubObject = mock(ActivityPubObject.class);
        when((ActivityPubObjectReference<ActivityPubObject>) activity.getObject())
            .thenReturn(new ActivityPubObjectReference<>().setObject(activityPubObject));
        when(likedCollection.contains(new ActivityPubObjectReference<>().setObject(activityPubObject)))
            .thenReturn(true);
        assertFalse(this.scriptService.likeActivity(activityId));
        verify(this.activityPubStorage, never()).storeEntity(any());
    }

    @Test
    void likeNewActivity() throws Exception
    {
        Person actor = mock(Person.class);
        when(this.actorHandler.getCurrentActor()).thenReturn(actor);

        String activityId = "http://xwiki/AP/activity/Foo";
        ActivityPubObjectReference<OrderedCollection<ActivityPubObject>> liked = mock(ActivityPubObjectReference.class);
        when(actor.getLiked()).thenReturn(liked);
        OrderedCollection likedCollection = mock(OrderedCollection.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(liked)).thenReturn(likedCollection);
        AbstractActivity activity = mock(AbstractActivity.class);
        when(this.activityPubObjectReferenceResolver
            .resolveReference(new ActivityPubObjectReference<>().setLink(URI.create(activityId))))
            .thenReturn(activity);
        ActivityPubObjectReference activityPubObjectReference = mock(ActivityPubObjectReference.class);
        when(activity.getObject()).thenReturn(activityPubObjectReference);
        when(likedCollection.contains(activityPubObjectReference))
            .thenReturn(false);
        ActivityPubObject activityObject = mock(ActivityPubObject.class);
        when(this.activityPubObjectReferenceResolver.resolveReference(activityPubObjectReference))
            .thenReturn(activityObject);
        Like likeActivity = new Like().setActor(actor).setObject(activityObject);
        LikeActivityHandler likeActivityHandler = mock(LikeActivityHandler.class);
        when(this.componentManager.getInstance(new DefaultParameterizedType(null, ActivityHandler.class, Like.class)))
            .thenReturn(likeActivityHandler);

        assertTrue(this.scriptService.likeActivity(activityId));
        verify(this.activityPubStorage).storeEntity(likeActivity);
        verify(likeActivityHandler).handleOutboxRequest(new ActivityRequest<>(actor, likeActivity));
    }

    @Test
    void escapeXWikiSyntaxNull()
    {
        assertNull(this.scriptService.escapeXWikiSyntax(null));
    }

    @Test
    void escapeXWikiSyntax()
    {
        assertEquals("abcd&#123;&#123;edf", this.scriptService.escapeXWikiSyntax("abcd{{edf"));
    }

    @Test
    void getSentMessages() throws Exception
    {
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(null)).thenReturn(userReference);
        AbstractActor targetActor = mock(AbstractActor.class);
        when(this.actorHandler.isAuthorizedToActFor(userReference, targetActor)).thenReturn(true);
        URI inputURI = URI.create("https://server/actor");
        when(targetActor.getId()).thenReturn(inputURI);
        when(this.internalURINormalizer.relativizeURI(inputURI)).thenReturn(inputURI);

        List<Note> value = Arrays.asList();
        when(this.activityPubStorage.query(Note.class, "filter(authors:https\\:\\/\\/server\\/actor)", 10)).thenReturn(
            value);
        List<Note> sentMessages = this.scriptService.getSentMessages(targetActor, 10);
        assertSame(value, sentMessages);
    }
}