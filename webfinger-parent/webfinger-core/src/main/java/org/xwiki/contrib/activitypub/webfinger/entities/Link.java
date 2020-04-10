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

import java.net.URI;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 *
 * A JSON Resource Descriptor link object.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4">The JSON Resource Descriptor (JRD)</a>
 * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">JRD's Links specifications</a>
 *
 * @version $Id$
 * @since 1.1
 */
public class Link
{
    private String rel;

    private String type;

    private URI href;

    /**
     * Get the link rel value.
     *
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4.1.">Specifications of the Link's ref value</a>
     * @return the rel value.
     */
    public String getRel()
    {
        return this.rel;
    }

    /**
     * Set the link rel value.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4.1.">Specifications of the Link's rel value</a>
     * @param rel the rel value.
     * @return self to allow chained method calls (fluent API).
     */
    public Link setRel(String rel)
    {
        this.rel = rel;
        return this;
    }

    /**
     * Get the link type value.
     * @see  <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4.2.">Specifications of the Link's type value</a>
     * @return the type.
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * Set the link type value.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4.2.">Specifications of the Link's type value</a>
     * @param type the type.
     * @return self to allow chained method calls (fluent API).
     */
    public Link setType(String type)
    {
        this.type = type;
        return this;
    }

    /**
     * Get the link href value.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4.3.">Specification of the Link's href value</a>
     * @return the href.
     */
    public URI getHref()
    {
        return this.href;
    }

    /**
     * Set the link href value.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4.3.">Specification of the Link's href value</a>
     * @param href the href.
     * @return self to allow chained method calls (fluent API).
     */
    public Link setHref(URI href)
    {
        this.href = href;
        return this;
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

        Link link = (Link) o;

        return new EqualsBuilder()
                   .append(this.rel, link.rel)
                   .append(this.type, link.type)
                   .append(this.href, link.href)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.rel)
                   .append(this.type)
                   .append(this.href)
                   .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("rel", this.getRel())
                   .append("type", this.getType())
                   .append("href", this.getHref())
                   .build();
    }
}
