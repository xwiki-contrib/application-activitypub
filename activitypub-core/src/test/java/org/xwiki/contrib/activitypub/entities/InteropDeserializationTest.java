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
package org.xwiki.contrib.activitypub.entities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Interoperability regression tests: verify that payloads captured from real fediverse servers (Mastodon, Lemmy, ...)
 * can still be parsed by the current entity model. These tests are the early-warning that fires when a federation
 * partner changes the shape of what it sends. The fixtures live under {@code src/test/resources/interop/}.
 * <p>
 * Refresh them periodically by fetching them again with {@code Accept: application/activity+json} and pretty-printing
 * the result, then re-run the tests: a failure means a partner changed something to adapt to. Re-capture rather than
 * hand-edit, so the fixtures keep reflecting what the servers really send. The sources are:
 * <ul>
 * <li>{@code mastodon/actor.json}: {@code https://mastodon.social/users/Gargron}</li>
 * <li>{@code mastodon/create-note.json}: the first {@code orderedItems} entry of
 * {@code https://mastodon.social/users/Gargron/outbox?page=true}</li>
 * <li>{@code lemmy/actor.json}: {@code https://lemmy.ml/u/nutomic}</li>
 * </ul>
 * Any long-lived public actor of the same server type works as well. GoToSocial actors are not captured since they
 * can only be fetched with a signed request.
 *
 * @version $Id$
 */
class InteropDeserializationTest extends AbstractEntityTest
{
    // The parser logs a warning for every unknown type and an info for every ignored JSON-LD context object; real
    // payloads legitimately contain both, so we only enforce that nothing is logged at ERROR level.
    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    private static Stream<Arguments> actorFixtures()
    {
        return Stream.of(
            arguments("mastodon/actor.json", Person.class),
            arguments("lemmy/actor.json", Person.class)
        );
    }

    private static Stream<Arguments> activityFixtures()
    {
        return Stream.of(
            arguments("mastodon/create-note.json", Create.class)
        );
    }

    @ParameterizedTest
    @MethodSource("actorFixtures")
    void parseRealActor(String fixture, Class<? extends AbstractActor> expectedType) throws Exception
    {
        ActivityPubObject parsed = this.parser.parse(readInteropResource(fixture));

        assertTrue(expectedType.isInstance(parsed),
            String.format("[%s] parsed as [%s] but expected [%s]", fixture, parsed.getClass(), expectedType));
        AbstractActor actor = (AbstractActor) parsed;
        assertNotNull(actor.getId(), fixture + ": actor id must be resolved");
        assertNotNull(actor.getInbox(), fixture + ": actor inbox must be resolved");
        assertNotNull(actor.getOutbox(), fixture + ": actor outbox must be resolved");
        assertNotNull(actor.getPublicKey(), fixture + ": actor public key must be resolved");
    }

    @ParameterizedTest
    @MethodSource("activityFixtures")
    void parseRealActivity(String fixture, Class<? extends AbstractActivity> expectedType) throws Exception
    {
        ActivityPubObject parsed = this.parser.parse(readInteropResource(fixture));

        assertTrue(expectedType.isInstance(parsed),
            String.format("[%s] parsed as [%s] but expected [%s]", fixture, parsed.getClass(), expectedType));
        AbstractActivity activity = (AbstractActivity) parsed;
        assertNotNull(activity.getId(), fixture + ": activity id must be resolved");
        assertNotNull(activity.getActor(), fixture + ": activity actor must be resolved");
        assertNotNull(activity.getObject(), fixture + ": activity object must be resolved");
    }

    private String readInteropResource(String fixture) throws IOException
    {
        try (InputStream is = getClass().getResourceAsStream("/interop/" + fixture)) {
            assertNotNull(is, "Missing interop fixture [" + fixture + "]");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }
}
