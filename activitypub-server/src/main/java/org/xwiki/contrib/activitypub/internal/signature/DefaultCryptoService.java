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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.EnumSet;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubException;
import org.xwiki.contrib.activitypub.CryptoService;
import org.xwiki.crypto.KeyPairGenerator;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.pkix.CertificateGenerator;
import org.xwiki.crypto.pkix.CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.X509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateGenerationParameters;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertificateParameters;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.crypto.signer.Signer;
import org.xwiki.crypto.signer.SignerFactory;

/**
 * Default implementation of {@link CryptoService}.
 * 
 * @version $Id$
 * @since 1.1
 */
@Component
@Singleton
public class DefaultCryptoService implements CryptoService
{
    @Inject
    private Provider<X509ExtensionBuilder> extensionBuilder;

    @Inject
    @Named("RSA")
    private KeyPairGenerator keyPairGenerator;

    @Inject
    @Named("X509")
    private CertificateGeneratorFactory certificateGeneratorFactory;

    @Inject
    @Named("SHA256withRSAEncryption")
    private SignerFactory signerFactory;

    @Override
    public CertifiedKeyPair generateCertifiedKeyPair() throws ActivityPubException
    {
        AsymmetricKeyPair keys = this.keyPairGenerator.generate();
        Signer signer = this.signerFactory.getInstance(true, keys.getPrivate());
        X509CertificateGenerationParameters
            parameters = new X509CertificateGenerationParameters(0,
            this.extensionBuilder.get().addBasicConstraints(true).addKeyUsage(true, EnumSet
                                                                                        .of(KeyUsage.keyCertSign,
                                                                                            KeyUsage.cRLSign)).build());
        CertificateGenerator certificateGenerator = this.certificateGeneratorFactory.getInstance(signer, parameters);
        try {
            CertifiedPublicKey certifiedPublicKey = certificateGenerator
                                                        .generate(new DistinguishedName("O=XWiki"), keys.getPublic(),
                                                            new X509CertificateParameters());
            return new CertifiedKeyPair(keys.getPrivate(), certifiedPublicKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new ActivityPubException("Error during the asymetric key generation.", e);
        }
    }
}
