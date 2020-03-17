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

import org.apache.commons.httpclient.HttpMethod;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultSignatureService}.
 *
 * @version $Id$
 * @since 1.1
 */
@ComponentTest
class DefaultSignatureServiceTest
{
    @InjectMockComponents
    private DefaultSignatureService actorHandler;

    @Test
    void generateSignature() throws Exception
    {
        HttpMethod postMethod = mock(HttpMethod.class);
        URI targetURI = URI.create("http://targeturi/");
        URI actorURI = URI.create("http://actoruri/");
        AbstractActor actor = mock(AbstractActor.class);
        when(actor.getPreferredUsername()).thenReturn("tmp");
        this.actorHandler.initKey("tmp");
        this.actorHandler.generateSignature(postMethod, targetURI, actorURI, actor);
        InOrder inOrder = inOrder(postMethod, postMethod);
        inOrder.verify(postMethod).addRequestHeader(eq("Signature"), matches(
            "keyId=\"http:\\/\\/actoruri\\/\",headers=\"\\(request-target\\) host date\","
                + "signature=\"[^\"]+\",algorithm=\"rsa-sha256\""));
        inOrder.verify(postMethod).addRequestHeader(eq("Date"), anyString());
    }
}