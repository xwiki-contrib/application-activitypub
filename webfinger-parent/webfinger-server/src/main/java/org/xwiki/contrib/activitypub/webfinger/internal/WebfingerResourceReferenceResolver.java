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
package org.xwiki.contrib.activitypub.webfinger.internal;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.webfinger.WebfingerResourceReference;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Resolve an {@link ExtendedURL} to an {@link WebfingerResourceReference}.
 *
 * @since 1.1
 * @version $Id$
 */
@Component
@Named("webfinger")
@Singleton
public class WebfingerResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    private static final String RESOURCE_KEY = "resource";

    private static final String ERROR_MESSAGE_TEMPLATE = "Invalid Webfinger URL form [%s]";

    @Override
    public WebfingerResourceReference resolve(ExtendedURL representation, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {

        /*
         * TODO Add support for the rel parameter.
         */

        if (!representation.getParameters().containsKey(RESOURCE_KEY)) {
            this.error(representation);
        }

        List<String> resources = representation.getParameters().get(RESOURCE_KEY);

        if (resources.size() != 1) {
            this.error(representation);
        }

        WebfingerResourceReference ret = new WebfingerResourceReference(resources.get(0));
        this.copyParameters(representation, ret);
        return ret;
    }

    /**
     * FIXME: must conform to https://tools.ietf.org/html/rfc2616#section-10.4.1.
     * @param representation the received URL.
     * @throws CreateResourceReferenceException the resolution error.
     */
    private void error(ExtendedURL representation) throws CreateResourceReferenceException
    {
        throw new CreateResourceReferenceException(
            String.format(ERROR_MESSAGE_TEMPLATE, representation.toString()));
    }
}
