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

import java.util.EnumSet;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.crypto.KeyPairGenerator;
import org.xwiki.crypto.params.cipher.asymmetric.AsymmetricKeyPair;
import org.xwiki.crypto.pkix.CertificateGenerator;
import org.xwiki.crypto.pkix.CertificateGeneratorFactory;
import org.xwiki.crypto.pkix.X509ExtensionBuilder;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.x509certificate.extension.KeyUsage;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultCryptoService}.
 *
 * @version $Id$
 * @since 1.1
 */
@ComponentTest
class DefaultCryptoServiceTest
{
    @InjectMockComponents
    private DefaultCryptoService actorHandler;

    @MockComponent
    @Named("RSA")
    private KeyPairGenerator keyPairGenerator;

    @MockComponent
    @Named("X509")
    private CertificateGeneratorFactory certificateGeneratorFactory;

    @MockComponent
    private Provider<X509ExtensionBuilder> extensionBuilder;

    @Test
    void generateCertifiedKeyPair() throws Exception
    {

        AsymmetricKeyPair asymmetricKeyPair = mock(AsymmetricKeyPair.class);
        when(this.keyPairGenerator.generate()).thenReturn(asymmetricKeyPair);
        X509ExtensionBuilder x509ExtensionBuilder = mock(X509ExtensionBuilder.class);
        when(this.extensionBuilder.get()).thenReturn(x509ExtensionBuilder);
        when(x509ExtensionBuilder.addBasicConstraints(true)).thenReturn(x509ExtensionBuilder);
        when(x509ExtensionBuilder.addBasicConstraints(true)).thenReturn(x509ExtensionBuilder);
        when(x509ExtensionBuilder.addKeyUsage(true, EnumSet.of(KeyUsage.keyCertSign, KeyUsage.cRLSign)))
            .thenReturn(x509ExtensionBuilder);

        when(this.certificateGeneratorFactory.getInstance(any(), any())).thenReturn(mock(CertificateGenerator.class));

        CertifiedKeyPair actual = this.actorHandler.generateCertifiedKeyPair();

        assertEquals(asymmetricKeyPair.getPrivate(), actual.getPrivateKey());
    }
}