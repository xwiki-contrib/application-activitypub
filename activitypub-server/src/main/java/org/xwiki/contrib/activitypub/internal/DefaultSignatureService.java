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
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.httpclient.HttpMethod;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.CryptoService;
import org.xwiki.contrib.activitypub.SignatureService;
import org.xwiki.crypto.pkix.CertifyingSigner;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.signer.CMSSignedDataGenerator;
import org.xwiki.crypto.signer.SignerFactory;
import org.xwiki.crypto.signer.param.CMSSignedDataGeneratorParameters;
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.KeyStoreException;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;

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
    @Named("X509wiki")
    private KeyStore x509WikiKeyStore;

    @Inject
    private CMSSignedDataGenerator cmsSignedDataGenerator;

    @Inject
    @Named("SHA256")
    private SignerFactory signerFactory;

    @Inject
    private XWikiUserBridge userBridge;

    @Inject
    private CryptoService cryptoService;

    @Override
    public void generateSignature(HttpMethod postMethod, URI targetURI, URI actorURI, UserReference user)
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

        byte[] bytess = this.sign(user, signatureStr);
        String signatureB64 = Base64.getEncoder().encodeToString(bytess);
        String actorAPURL = actorURI.toASCIIString();
        postMethod.addRequestHeader("Signature", String.format(
            "keyId=\"%s\",headers=\"(request-target) host date\",signature=\"%s\"", actorAPURL, signatureB64));
        postMethod.addRequestHeader("Date", date);
    }

    private CertifiedKeyPair getCertifiedKeyPair(UserReference user) throws ActivityPubException
    {
        try {
            DocumentReference dr = this.userBridge.getDocumentReference(user);
            CertifiedKeyPair stored = this.x509WikiKeyStore.retrieve(new WikiStoreReference(dr));

            if (stored != null) {
                return stored;
            }

            return this.initKeys(dr);
        } catch (KeyStoreException e) {
            throw new ActivityPubException(String.format("Error while retrieving the private key for user [%s]", user),
                e);
        }
    }

    private byte[] sign(UserReference user, String signedString)
        throws ActivityPubException
    {
        try {
            CertifiedKeyPair keyPair = this.getCertifiedKeyPair(user);
            CMSSignedDataGeneratorParameters parameters = new CMSSignedDataGeneratorParameters().addSigner(
                CertifyingSigner.getInstance(true, keyPair, this.signerFactory));

            return this.cmsSignedDataGenerator.generate(signedString.getBytes(), parameters, false);
        } catch (GeneralSecurityException e) {
            throw new ActivityPubException("Error while signing [" + signedString + "]", e);
        }
    }

    private CertifiedKeyPair initKeys(DocumentReference user) throws ActivityPubException
    {
        try {
            CertifiedKeyPair ret = this.cryptoService.generateCertifiedKeyPair();
            this.x509WikiKeyStore.store(new WikiStoreReference(user), ret);
            return ret;
        } catch (KeyStoreException e) {
            throw new ActivityPubException(
                String.format("Error while initializing the cryptographic keys for [%s]", user), e);
        }
    }

    @Override
    public String getPublicKeyPEM(UserReference user) throws ActivityPubException
    {
        byte[] encoded = this.getCertifiedKeyPair(user).getPublicKey().getEncoded();

        return String.format("-----BEGIN PUBLIC KEY-----\n%s\n-----END PUBLIC KEY-----\n",
            Base64.getEncoder().encodeToString(encoded));
    }
}
