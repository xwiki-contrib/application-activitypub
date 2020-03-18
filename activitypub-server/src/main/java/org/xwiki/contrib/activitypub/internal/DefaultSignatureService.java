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
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.SignatureService;
import org.xwiki.contrib.activitypub.entities.AbstractActor;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Default implementation of the signature service.
 *
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
public class DefaultSignatureService implements SignatureService
{
    /**
     * Store the public keys of the actors.
     */
    private Map<String, PublicKey> pubKeyStore;

    /**
     * Store the private keys of the actors.
     */
    private Map<String, PrivateKey> privKeyStore;

    /**
     *  Default constructor of {@link DefaultSignatureService}.
     */
    public DefaultSignatureService()
    {
        this.pubKeyStore = new HashMap<>();
        this.privKeyStore = new HashMap<>();
    }

    @Override
    public void generateSignature(HttpMethod postMethod, URI targetURI, URI actorURI, AbstractActor actor)
        throws ActivityPubException
    {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        String date = dateFormat.format(calendar.getTime());
        String uriPath = targetURI.getPath();
        String host = targetURI.getHost();
        String signatureStr = String.format("(request-target): post %s\nhost: %s\ndate: %s", uriPath, host, date);

        byte[] bytess = this.sign(actor.getPreferredUsername(), signatureStr);
        String signatureB64 =
            Base64.getEncoder().encodeToString(
                bytess);
        String actorAPURL = actorURI.toASCIIString();
        postMethod.addRequestHeader("Signature", String.format(
            "keyId=\"%s\",headers=\"(request-target) host date\",signature=\"%s\",algorithm=\"rsa-sha256\"", actorAPURL,
            signatureB64));
        postMethod.addRequestHeader("Date", date);
    }

    private void storeKeyPair(String userId, PublicKey pubk, PrivateKey privk)
    {
        this.pubKeyStore.put(userId, pubk);
        this.privKeyStore.put(userId, privk);
    }

    private PrivateKey getPrivKey(String actorId) throws ActivityPubException
    {
        PrivateKey privateKey = this.privKeyStore.get(actorId);
        if (privateKey == null) {
            this.initKey(actorId);
            return this.privKeyStore.get(actorId);
        }
        return privateKey;
    }

    private byte[] sign(String actorId, String signedString)
        throws ActivityPubException
    {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            PrivateKey key = this.getPrivKey(actorId);
            sign.initSign(key);
            sign.update(signedString.getBytes(UTF_8));
            return sign.sign();
        } catch (NoSuchAlgorithmException | ActivityPubException | InvalidKeyException | SignatureException e) {
            throw new ActivityPubException(String.format("Error while signing [%s] for [%s]", signedString, actorId),
                e);
        }
    }

    @Override
    public PublicKey initKey(String actorId) throws ActivityPubException
    {
        PublicKey pubKey;
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024, new SecureRandom());

            KeyPair pair = generator.generateKeyPair();
            pubKey = pair.getPublic();
            PrivateKey privKey = pair.getPrivate();
            this.storeKeyPair(actorId, pubKey, privKey);
        } catch (NoSuchAlgorithmException e) {
            throw new ActivityPubException("Error while generating the user key pair ", e);
        }
        return pubKey;
    }
}
