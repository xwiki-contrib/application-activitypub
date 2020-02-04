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
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubStore;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.entities.Actor;
import org.xwiki.contrib.activitypub.ActivityPubJsonParser;
import org.xwiki.contrib.activitypub.entities.activities.Activity;
import org.xwiki.contrib.activitypub.entities.Object;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Component
@Named("activitypub")
@Singleton
public class ActivityPubResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    private static final ResourceType TYPE = new ResourceType("activitypub");

    @Inject
    private Logger logger;

    @Inject
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
        Object entity = this.activityPubStorage.retrieveEntity(resourceReference.getUuid());

        try {
            if ("POST".equalsIgnoreCase(request.getMethod())) {
                if ("inbox".equalsIgnoreCase(resourceReference.getEntityType())) {
                    this.handleBox(entity, BOX_TYPE.INBOX);
                } else if ("outbox".equalsIgnoreCase(resourceReference.getEntityType())) {
                    this.handleBox(entity, BOX_TYPE.OUTBOX);
                } else {
                    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    response.setContentType("text/plain");
                    response.getOutputStream()
                        .write("You cannot post anything outside an inbox our an outbox."
                            .getBytes(StandardCharsets.UTF_8));
                }
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("application/activity+json");

                // FIXME: This should be more complicated, we'd need to check authorization etc.
                // We probably need an entity handler component to manage the various kind of entities to retrieve.
                this.activityPubJsonSerializer.serialize(response.getOutputStream(), entity);
            }
        } catch (IOException e) {
            this.logger.error("Error while handling [{}]", resourceReference, e);
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private void handleBox(Object entity, BOX_TYPE boxType) throws IOException
    {
        ActivityRequest<Activity> activityRequest = this.parseRequest(entity);
        if (activityRequest != null && activityRequest.getActor() != null) {
            ActivityHandler<Activity> handler = this.getHandler(activityRequest);
            if (handler != null) {
                if (boxType == BOX_TYPE.INBOX) {
                    handler.handleInboxRequest(activityRequest);
                } else {
                    handler.handleOutboxRequest(activityRequest);
                }
            } else {
                logger.error("Error while looking for an handler for activity [{}]",
                    activityRequest.getActivity().getType());
            }
        } else {
            logger.error("Error while parsing the request: the activity or its actor cannot be retrieved.");
        }
    }

    private <T extends Activity> ActivityRequest<T> parseRequest(Object entity)
    {
        HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        try {
            String requestBody = IOUtils.toString(request.getReader());
            Object object = this.activityPubJsonParser.parseRequest(requestBody);
            if (object != null) {
                T activity = getActivity(object);
                // We consider that every inbox has an attributedTo field fill to retrieve the actor it's linked to...
                // Not sure if that's good, but looks better than having distinct resource references for actions and
                // entities such as POST on /bin/activitypub/XWiki/Foobar/inbox and GET on /activitypub/note/4242
                // Right now we can POST and GET on /activitypub/inbox/Foobar which represents both action and entity.
                // This really needs to be discussed.
                Actor actor = entity.getAttributedTo().get(0).getObject(this.activityPubJsonParser);
                return new ActivityRequest<T>(actor, activity, request, response);
            } else {
                this.logger.warn("Parsing of ActivityPub request body returned a null object.");
            }
        } catch (IOException e) {
            this.logger.error("Error while getting body from current request.", e);
        }
        return null;
    }

    private <T extends Activity> T getActivity(Object object)
    {
        if (Activity.class.isAssignableFrom(object.getClass())) {
            return (T) object;
        } else {
            // TODO: handle wrapping object in a create activity
            throw new NotImplementedException();
        }
    }

    private <T extends Activity> ActivityHandler<T> getHandler(ActivityRequest<T> activityRequest)
    {
        try {
            return this.componentManager.getInstance(ActivityHandler.class, activityRequest.getActivity().getType());
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the ActivityHandler for activity [{}]",
                activityRequest.getActivity().getType(), e);
        }
        return null;
    }
}
