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
package org.xwiki.contrib.activitypub.webfinger.entities;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.contrib.activitypub.webfinger.internal.json.DefaultWebfingerJsonParser;
import org.xwiki.contrib.activitypub.webfinger.internal.json.ObjectMapperConfiguration;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Interoperability regression tests for WebFinger discovery: verify that responses captured from real fediverse servers
 * still parse and still expose the {@code rel="self"} actor link the extension relies on (see
 * {@code DefaultActorHandler}). Fixtures live under {@code src/test/resources/interop/}.
 * <p>
 * Refresh them periodically by fetching them again with {@code Accept: application/jrd+json} and pretty-printing the
 * result, rather than hand-editing them. The sources are:
 * <ul>
 * <li>{@code mastodon-webfinger.json}:
 * {@code https://mastodon.social/.well-known/webfinger?resource=acct:Gargron@mastodon.social}</li>
 * <li>{@code lemmy-webfinger.json}: {@code https://lemmy.ml/.well-known/webfinger?resource=acct:nutomic@lemmy.ml}</li>
 * <li>{@code gotosocial-webfinger.json}: {@code https://gts.superseriousbusiness.org/.well-known/webfinger?resource=
 * acct:dumpsterqueer@gts.superseriousbusiness.org}</li>
 * </ul>
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ ObjectMapperConfiguration.class })
class WebfingerInteropTest
{
    @InjectMockComponents
    private DefaultWebfingerJsonParser parser;

    @ParameterizedTest
    @ValueSource(strings = {
        "interop/mastodon-webfinger.json",
        "interop/lemmy-webfinger.json",
        "interop/gotosocial-webfinger.json"
    })
    void parseRealWebfinger(String fixture) throws Exception
    {
        JSONResourceDescriptor jrd = this.parser.parse(readResource(fixture));

        assertNotNull(jrd.getSubject(), fixture + ": subject must be present");
        assertNotNull(jrd.getLinks(), fixture + ": links must be present");

        // Mirror exactly how the extension resolves an actor from a WebFinger response.
        Optional<URI> self = jrd.getLinks().stream()
            .filter(link -> Objects.equals(link.getRel(), "self"))
            .map(Link::getHref)
            .filter(Objects::nonNull)
            .findFirst();
        assertTrue(self.isPresent(), fixture + ": a rel=self link with an href is required to resolve the actor");
    }

    private String readResource(String path) throws Exception
    {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(is, "Missing interop fixture [" + path + "]");
            return IOUtils.toString(is, "UTF-8");
        }
    }
}
