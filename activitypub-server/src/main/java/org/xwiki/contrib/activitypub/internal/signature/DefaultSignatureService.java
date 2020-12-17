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
package org.xwiki.contrib.activitypub.internal.signature;

import java.io.File;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.ActorHandler;
import org.xwiki.contrib.activitypub.CryptoService;
import org.xwiki.contrib.activitypub.SignatureService;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.contrib.activitypub.internal.DateProvider;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.store.FileStoreReference;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;

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
    @Inject
    private DateProvider dateProvider;

    @Inject
    @Named("X509file")
    private KeyStore keyStore;

    @Inject
    private Provider<ActorHandler> actorHandlerProvider;

    @Inject
    private CryptoService cryptoService;

    @Inject
    private Environment environment;

    @Override
    public void generateSignature(HttpMethod postMethod, AbstractActor actor, String content)
        throws ActivityPubException
    {
        String date = this.dateProvider.getFormattedDate();

        try {
            URI postMethodURI = postMethod.getURI();
            String uriPath = postMethodURI.getPath();
            String host = postMethodURI.getHost();
            String digest = Base64.getEncoder().encodeToString(DigestUtils.sha256(content));
            String signatureStr =
                String.format("(request-target): post %s\nhost: %s\ndate: %s\ndigest: SHA-256=%s", uriPath, host, date,
                    digest);

            byte[] bytess = this.sign(actor, signatureStr);
            String signatureB64 = Base64.getEncoder().encodeToString(bytess);
            String actorAPURL = actor.getId().toASCIIString();
            String signature =
                String.format("keyId=\"%s\",headers=\"(request-target) host date digest\",signature=\"%s\"",
                    actorAPURL, signatureB64);
            postMethod.addRequestHeader("Signature", signature);
            postMethod.addRequestHeader("Date", date);
        } catch (URIException e) {
            throw new ActivityPubException("Error while retrieving the URI from post method", e);
        }
    }

    private CertifiedKeyPair getCertifiedKeyPair(AbstractActor actor) throws ActivityPubException
    {
        DocumentReference dr = this.actorHandlerProvider
            .get()
            .getStoreDocument(actor);

        CertifiedKeyPair stored;
        try {
            stored = this.keyStore.retrieve(this.buildFileStoreReference(dr));
        } catch (KeyStoreException e) {
            stored = null;
        }

        if (stored != null) {
            return stored;
        }

        return this.initKeys(dr);
    }

    private byte[] sign(AbstractActor actor, String signedString)
        throws ActivityPubException
    {
        try {
            PrivateKeyParameters pk = this.getCertifiedKeyPair(actor).getPrivateKey();
            byte[] encoded = pk.getEncoded();
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            PrivateKey key = rsa.generatePrivate(new PKCS8EncodedKeySpec(encoded));
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(key);
            sign.update(signedString.getBytes(UTF_8));
            return sign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException | InvalidKeySpecException e) {
            throw new ActivityPubException(String.format("Error while signing [%s] for [%s]", signedString, actor),
                e);
        }
    }

    private CertifiedKeyPair initKeys(DocumentReference user) throws ActivityPubException
    {
        try {
            CertifiedKeyPair ret = this.cryptoService.generateCertifiedKeyPair();
            this.keyStore.store(this.buildFileStoreReference(user), ret);
            return ret;
        } catch (KeyStoreException e) {
            throw new ActivityPubException(
                String.format("Error while initializing the cryptographic keys for [%s]", user), e);
        }
    }

    private FileStoreReference buildFileStoreReference(DocumentReference user)
    {
        File permDir = this.environment.getPermanentDirectory();
        File apKeysDir = new File(permDir, "activitypub");
        apKeysDir.mkdirs();
        File activitypub = new File(apKeysDir, String.format("%s.key", user.toString()));
        return new FileStoreReference(activitypub);
    }

    @Override
    public String getPublicKeyPEM(AbstractActor actor) throws ActivityPubException
    {
        byte[] encoded = this
            .getCertifiedKeyPair(actor)
            .getPublicKey()
            .getEncoded();

        return String.format("-----BEGIN PUBLIC KEY-----\n"
                + "%s\n"
                + "-----END PUBLIC KEY-----\n",
            Base64.getEncoder().encodeToString(encoded));
    }
}
