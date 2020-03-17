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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * The public key object of an ActivityPub actor.
 *
 * @version $Id$
 * @since 1.1
 */
public class PublicKey
{
    private String id;

    private String owner;

    private String publicKeyPem;

    /**
     * 
     * @return The public key id.
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Set the public key id.
     * @param id The id.
     * @return The public key object.
     */
    public PublicKey setId(String id)
    {
        this.id = id;
        return this;
    }

    /**
     * 
     * @return The public key own.
     */
    public String getOwner()
    {
        return this.owner;
    }

    /**
     * Set the public key owner.
     * @param owner The owner.
     * @return The public key object.
     */
    public PublicKey setOwner(String owner)
    {
        this.owner = owner;
        return this;
    }

    /**
     * 
     * @return The public key pem.
     */
    public String getPublicKeyPem()
    {
        return this.publicKeyPem;
    }

    /**
     * Set the public key pem.
     * @param publicKeyPem The public key pem.
     * @return the public key object.
     */
    public PublicKey setPublicKeyPem(String publicKeyPem)
    {
        this.publicKeyPem = publicKeyPem;
        return this;
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("id", this.getId())
                   .append("owner", this.getOwner())
                   .append("publicKeyPem", this.getPublicKeyPem())
                   .build();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        PublicKey publicKey = (PublicKey) o;

        return new EqualsBuilder()
                   .append(this.id, publicKey.id)
                   .append(this.owner, publicKey.owner)
                   .append(this.publicKeyPem, publicKey.publicKeyPem)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.id)
                   .append(this.owner)
                   .append(this.publicKeyPem)
                   .toHashCode();
    }
}
