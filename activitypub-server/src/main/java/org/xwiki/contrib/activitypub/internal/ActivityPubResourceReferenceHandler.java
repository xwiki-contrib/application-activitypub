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

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubResourceReference;
import org.xwiki.contrib.activitypub.ActivityPubStore;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.Activity;
import org.xwiki.contrib.activitypub.entities.ActivityPubObject;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.annotations.Authenticate;

import com.xpn.xwiki.XWikiContext;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Component
@Named("activitypub")
@Singleton
@Authenticate
public class ActivityPubResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    private static final ResourceType TYPE = new ResourceType("activitypub");

    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private ActivityPubJsonParser activityPubJsonParser;

    @Inject
    private ActivityPubJsonSerializer activityPubJsonSerializer;

    @Inject
    private Container container;

    @Inject
    private ActorHandler actorHandler;

    @Inject
    private ActivityPubStore activityPubStorage;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(TYPE);
    }

    private enum BOX_TYPE {
        INBOX, OUTBOX;
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        ActivityPubResourceReference resourceReference = (ActivityPubResourceReference) reference;
        HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        ActivityPubObject entity = this.activityPubStorage
            .retrieveEntity(resourceReference.getEntityType(), resourceReference.getUuid());

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                try {
                    Actor actor = getActor(resourceReference);
                    if (actor != null) {
                        if ("inbox".equalsIgnoreCase(resourceReference.getEntityType())) {
                            this.handleBox(actor, BOX_TYPE.INBOX);
                        } else if ("outbox".equalsIgnoreCase(resourceReference.getEntityType())) {
                            this.handleBox(actor, BOX_TYPE.OUTBOX);
                        } else {
                            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                            response.setContentType("text/plain");
                            response.getOutputStream()
                                .write("You cannot post anything outside an inbox our an outbox."
                                    .getBytes(StandardCharsets.UTF_8));
                        }
                    }
                } catch (ActivityPubException e) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("text/plain");
                    e.printStackTrace(response.getWriter());
                }
            } else {
                if (entity == null && "person".equalsIgnoreCase(resourceReference.getEntityType())
                    || "actor".equalsIgnoreCase(resourceReference.getEntityType())) {
                    if (this.actorHandler.isExistingUser(resourceReference.getUuid())) {
                        try {
                            entity = this.actorHandler.getActor(resourceReference.getUuid());
                        } catch (ActivityPubException e) {
                            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            response.setContentType("text/plain");
                            e.printStackTrace(response.getWriter());
                        }
                    }
                }
                if (entity != null) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/activity+json");
                    response.setCharacterEncoding(StandardCharsets.UTF_8.toString());

                    // FIXME: This should be more complicated, we'd need to check authorization etc.
                    // We probably need an entity handler component to manage the various kind of entities to retrieve.
                    this.activityPubJsonSerializer.serialize(response.getOutputStream(), entity);
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.setContentType("text/plain");
                    response.getOutputStream().write(
                        String.format("The entity of type [%s] and uid [%s] cannot be found.",
                            resourceReference.getEntityType(), resourceReference.getUuid())
                            .getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw
                new ResourceReferenceHandlerException(String.format("Error while handling [%s]", resourceReference), e);
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private Actor getActor(ActivityPubResourceReference resourceReference)
        throws IOException, ActivityPubException
    {
        if (this.actorHandler.isExistingUser(resourceReference.getUuid())) {
            return this.actorHandler.getActor(resourceReference.getUuid());
        } else {
            this.sendErrorResponse(HttpServletResponse.SC_NOT_FOUND,
                String.format("User [%s] cannot be found.", resourceReference.getUuid()));
        }
        return null;
    }

    private void sendErrorResponse(int statusCode, String message) throws IOException
    {
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        response.setStatus(statusCode);
        response.setContentType("text/plain");
        response.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8));
    }

    private void handleBox(Actor actor, BOX_TYPE boxType)
        throws IOException, ActivityPubException
    {
        ActivityRequest<Activity> activityRequest = this.parseRequest(actor);
        if (activityRequest != null && activityRequest.getActor() != null) {
            ActivityHandler<Activity> handler = this.getHandler(activityRequest);
            if (handler != null) {
                if (boxType == BOX_TYPE.INBOX) {
                    handler.handleInboxRequest(activityRequest);
                } else {
                    DocumentReference sessionUserReference = this.contextProvider.get().getUserReference();
                    EntityReference xWikiUserReference = this.actorHandler.getXWikiUserReference(actor);
                    if (xWikiUserReference != null && xWikiUserReference.equals(sessionUserReference)) {
                        handler.handleOutboxRequest(activityRequest);
                    } else {
                        this.sendErrorResponse(HttpServletResponse.SC_FORBIDDEN,
                            String.format("The session user [%s] cannot post to [%s] outbox.",
                                sessionUserReference, xWikiUserReference));
                    }
                }
            } else {
                throw new ActivityPubException(String.format("Error while looking for an handler for activity [%s]",
                    activityRequest.getActivity().getType()));
            }
        } else {
            throw new ActivityPubException("Error while parsing the request: the activity or its actor "
                + "cannot be retrieved.");
        }
    }

    private <T extends Activity> ActivityRequest<T> parseRequest(Actor actor)
    {
        HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        try {
            String requestBody = IOUtils.toString(request.getReader());
            ActivityPubObject object = this.activityPubJsonParser.parseRequest(requestBody);
            if (object != null) {
                T activity = getActivity(object);
                return new ActivityRequest<T>(actor, activity, request, response);
            } else {
                this.logger.warn("Parsing of ActivityPub request body returned a null object.");
            }
        } catch (IOException e) {
            this.logger.error("Error while getting body from current request.", e);
        }
        return null;
    }

    private <T extends Activity> T getActivity(ActivityPubObject object)
    {
        if (Activity.class.isAssignableFrom(object.getClass())) {
            return (T) object;
        } else {
            // TODO: handle wrapping object in a create activity
            throw new NotImplementedException();
        }
    }

    private <T extends Activity> Class<T> getActivityClass(T object)
    {
        return (Class<T>) object.getClass();
    }

    private <T extends Activity> ActivityHandler<T> getHandler(ActivityRequest<T> activityRequest)
    {
        try {
            Type activityHandlerType = new DefaultParameterizedType(null, ActivityHandler.class,
                getActivityClass(activityRequest.getActivity()));
            return this.componentManager.getInstance(activityHandlerType);
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the ActivityHandler for activity [{}]",
                activityRequest.getActivity().getType(), e);
        }
        return null;
    }
}
