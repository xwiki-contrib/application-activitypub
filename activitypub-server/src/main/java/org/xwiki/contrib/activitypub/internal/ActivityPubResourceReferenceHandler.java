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
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.ActivityHandler;
import org.xwiki.contrib.activitypub.ActivityRequest;
import org.xwiki.contrib.activitypub.parser.ActivityPubParser;
import org.xwiki.contrib.activitystream.entities.Activity;
import org.xwiki.contrib.activitystream.entities.Object;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.entity.EntityResourceAction;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

@Component
@Named("activitypub")
@Singleton
public class ActivityPubResourceReferenceHandler extends AbstractResourceReferenceHandler<EntityResourceAction>
{
    private static final EntityResourceAction TYPE = new EntityResourceAction("activitypub");

    @Inject
    private Logger logger;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ActivityPubParser activityPubParser;

    @Inject
    private Container container;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public List<EntityResourceAction> getSupportedResourceReferences()
    {
        return Arrays.asList(TYPE);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        ActivityPubResourceReference resourceReference = (ActivityPubResourceReference) reference;
        EntityReference entityReference = resourceReference.getEntityReference();
        ActivityRequest<Activity> activityRequest = this.parseRequest(resourceReference);

        if (isUserPage(entityReference) && activityRequest != null) {
            ActivityHandler<Activity> handler = this.getHandler(activityRequest);
            if (handler != null && resourceReference.targetInbox()) {
                handler.handleInboxRequest(activityRequest);
            } else if (handler != null && resourceReference.targetOutbox()) {
                handler.handleOutboxRequest(activityRequest);
            }
        }

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private <T extends Activity> ActivityRequest<T> parseRequest(ActivityPubResourceReference resourceReference)
    {
        HttpServletRequest request = ((ServletRequest) this.container.getRequest()).getHttpServletRequest();
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();
        try {
            String requestBody = IOUtils.toString(request.getReader());
            Object object = this.activityPubParser.parseRequest(requestBody);
            if (object != null) {
                T activity = getActivity(object);
                return new ActivityRequest<T>(resourceReference.getEntityReference(), activity, request, response);
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

    private boolean isUserPage(EntityReference entityReference)
    {
        // TODO: Check if the entity reference document contains a XWiki.XWikiUser object
        return true;
    }
}
