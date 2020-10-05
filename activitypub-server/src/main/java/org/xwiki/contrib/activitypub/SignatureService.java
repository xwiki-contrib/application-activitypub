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
package org.xwiki.contrib.activitypub;

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Role;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.stability.Unstable;

/**
 * Deal with the concerns related to the management, storage and use of asymetric keys.
 *
 * @version $Id$
 * @since 1.1
 */
@Unstable
@Role
public interface SignatureService
{
    /**
     * Generate the signature of a message send to an external ActivityPub inbox.
     *
     * @param postMethod the post method to sign
     * @param actor the actor who posts the message
     * @param content the content of the body of the request
     * @throws ActivityPubException in case of error when signing the request
     */
    void generateSignature(HttpMethod postMethod, AbstractActor actor, String content)
        throws ActivityPubException;

    /**
     * Retrieve the public key of an actor.
     *
     * @param actor the actor from which to retrieve the public key.
     * @return The public key of the actor.
     * @throws ActivityPubException in case of error during the key generation.
     */
    String getPublicKeyPEM(AbstractActor actor) throws ActivityPubException;
}
