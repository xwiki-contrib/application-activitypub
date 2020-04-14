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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.internal.XWikiUserBridge;
import org.xwiki.contrib.activitypub.webfinger.WebfingerException;
import org.xwiki.contrib.activitypub.webfinger.WebfingerJsonSerializer;
import org.xwiki.contrib.activitypub.webfinger.WebfingerResourceReference;
import org.xwiki.contrib.activitypub.webfinger.WebfingerService;
import org.xwiki.contrib.activitypub.webfinger.entities.JSONResourceDescriptor;
import org.xwiki.contrib.activitypub.webfinger.entities.Link;
import org.xwiki.resource.AbstractResourceReferenceHandler;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceType;

/**
 *
 * Webfinger resource handler.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033">Webfinger RFC</a>
 *
 * @since 1.1
 * @version $Id$
 */
@Component
@Named("webfinger")
@Singleton
public class WebfingerResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
{
    /**
     * Default WebFinger answer if the resource parameter is missing.
     */
    public static final String DEFAULT_ERROR_ANSWER_NO_RESOURCE = "Missing resource parameter.";

    private static final ResourceType TYPE = new ResourceType("webfinger");

    private static final String TEXTPLAIN_CONTENTTYPE = "text/plain";

    private static final String REL_PARAM_KEY = "rel";

    private static final String RESOURCE_PARAM_KEY = "resource";

    private static final String ACCT_PARAM_KEY = "acct:";

    @Inject
    private WebfingerService webfingerService;

    @Inject
    private XWikiUserBridge xWikiUserBridge;

    @Inject
    private Container container;

    @Inject
    private Logger logger;

    @Inject
    private WebfingerJsonSerializer webfingerJsonSerializer;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(TYPE);
    }

    @Override
    public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain)
        throws ResourceReferenceHandlerException
    {
        WebfingerResourceReference resourceReference = (WebfingerResourceReference) reference;
        HttpServletResponse response = ((ServletResponse) this.container.getResponse()).getHttpServletResponse();

        this.proceed(resourceReference, response);

        // Be a good citizen, continue the chain, in case some lower-priority Handler has something to do for this
        // Resource Reference.
        chain.handleNext(reference);
    }

    private <T extends AbstractActor> void proceed(WebfingerResourceReference reference, HttpServletResponse response)
    {
        try {
            this.validateReference(reference);

            String resource = reference.getParameterValue(RESOURCE_PARAM_KEY);
            List<String> rels = reference.getParameterValues(REL_PARAM_KEY);

            URI resourceURI = this.convertResourceToURI(resource);

            String username = resourceURI.getUserInfo();

            /*
             * https://tools.ietf.org/html/rfc7033#section-4.2
             * If the "resource" parameter is a value for which the server has no information, the server MUST indicate
             * that it was unable to match the request as per Section 10.4.5 of RFC 2616.
             */
            if (!this.xWikiUserBridge.isExistingUser(username)) {
                throw new WebfingerException(String.format("There's no known user with username [%s].", username), 404);
            }

            AbstractActor actor = this.webfingerService.resolveActivityPubUser(username);
            URI apUserURI = actor.getId();

            /*
             * Check if the request domain matches the domain of the server:
             * https://tools.ietf.org/html/rfc7033#section-4 "The host to which a WebFinger query is issued is
             * significant.  If the query target contains a "host" portion (Section 3.2.2 of RFC 3986), then the host
             * to which the WebFinger query is issued SHOULD be the same as the "host" portion of the query target,
             * unless the client receives instructions through some out-of-band mechanism to send the query to another
             * host.  If the query target does not contain a "host" portion, then the client chooses a host to which
             * it directs the query using additional information it has."
             */
            if (!(Objects.equals(apUserURI.getHost(), resourceURI.getHost())
                      && this.normalizePort(apUserURI.getPort()) == this.normalizePort(resourceURI.getPort())))
            {
                throw new WebfingerException("No user found for the given domain", 404);
            }

            this.sendValidResponse(response, actor, resource, rels);
        } catch (WebfingerException e) {
            this.handleException(response, e);
        } catch (URISyntaxException | IOException e) {
            this.handleError(response, 500, e.getMessage());
        }
    }

    private int normalizePort(int port)
    {
        return port == -1 ? 80 : port;
    }

    /**
     * Checks if the webfinger resource reference is wellformed.
     * @param reference the checked resource reference.
     */
    private void validateReference(WebfingerResourceReference reference) throws WebfingerException
    {

        /*
         * https://tools.ietf.org/html/rfc7033#section-4.2
         * If the "resource" parameter is a value for which the server has no information, the server MUST indicate that
         * it was unable to match the request as per Section 10.4.5 of RFC 2616.
         */
        if (reference.getParameterValues(RESOURCE_PARAM_KEY) == null
                || reference.getParameterValues(RESOURCE_PARAM_KEY).size() != 1)
        {
            throw new WebfingerException(DEFAULT_ERROR_ANSWER_NO_RESOURCE, 400);
        }

        // checks is unexpected parameters exist
        Set<String> parametersKeys = reference.getParameters().keySet();
        Predicate<String> filterLambda =
            it -> !Objects.equals(it, REL_PARAM_KEY) && !Objects.equals(it, RESOURCE_PARAM_KEY);
        if (parametersKeys.stream().anyMatch(filterLambda)) {
            String unknownParameterNames =
                parametersKeys.stream().filter(filterLambda).collect(Collectors.joining(", "));
            this.logger.info(String.format("[%s] contains unknown parameters [%s]", reference, unknownParameterNames));
        }
    }

    /**
     * Converts a resource to an {@link URI}.
     * @param resource the converted resource.
     * @return The corresponding URI.
     * @throws URISyntaxException in case of invalid URI
     */
    private URI convertResourceToURI(String resource) throws URISyntaxException
    {
        // any scheme can be used, cf https://tools.ietf.org/html/rfc7033#section-4.5
        String cleanedResource;
        if (resource.startsWith(ACCT_PARAM_KEY)) {
            cleanedResource = resource.replaceAll("^" + ACCT_PARAM_KEY, "");
        } else {
            cleanedResource = resource;
        }
        return new URI(ACCT_PARAM_KEY + "//" + cleanedResource);
    }

    private void sendValidResponse(HttpServletResponse response, AbstractActor actor, String resource,
        List<String> rels) throws IOException, WebfingerException
    {
        response.setContentType("application/jrd+json");

        URI xWikiUserURI = this.webfingerService.resolveXWikiUserUrl(actor);
        Link xWikiUserLink = new Link()
                                 .setRel("http://webfinger.net/rel/profile-page")
                                 .setType("text/html")
                                 .setHref(xWikiUserURI);

        Link apUserLink = new Link()
                              .setRel("self")
                              .setType("application/activity+json")
                              .setHref(actor.getId());

        List<Link> links = this.filterLinks(rels, xWikiUserLink, apUserLink);

        JSONResourceDescriptor object =
            new JSONResourceDescriptor().setSubject(normalizeSubject(resource)).setLinks(links);
        this.webfingerJsonSerializer.serialize(response.getOutputStream(), object);
    }

    private String normalizeSubject(String resource)
    {
        if (resource.startsWith(ACCT_PARAM_KEY)) {
            return resource;
        }
        return ACCT_PARAM_KEY + resource;
    }

    /**
     * Filter the included links according to the rel parameter.
     * @param relParameter the list of rel passed as parameter.
     * @param links The link of possibly included links.
     * @return The filtered list of rel link to include.
     */
    private List<Link> filterLinks(List<String> relParameter, Link... links)
    {
        List<Link> ret;
        if (relParameter != null && !relParameter.isEmpty()) {
            // if at least one rel parameter exists, only the requested rel are included.
            ret = new ArrayList<>();
            for (Link link : links) {
                if (relParameter.contains(link.getRel())) {
                    ret.add(link);
                }
            }
        } else {
            // if no rel parameter is passed, all the ret are included.
            ret = Arrays.asList(links);
        }
        return ret;
    }

    /**
     *
     * Utility method to send an error message in case of exception.
     * @param response the servlet response to use
     * @param e the exception to handle.
     */
    private void handleException(HttpServletResponse response, WebfingerException e)
    {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();
        this.handleError(response, errorCode, message);
    }

    private void handleError(HttpServletResponse response, int errorCode, String errorMessage)
    {
        response.setStatus(errorCode);
        if (errorMessage != null) {
            response.setContentType(TEXTPLAIN_CONTENTTYPE);
            try (OutputStreamWriter o = new OutputStreamWriter(response.getOutputStream())) {
                o.write(errorMessage);
            } catch (IOException e) {
                this.logger.error(e.getMessage());
            }
        }
    }
}
