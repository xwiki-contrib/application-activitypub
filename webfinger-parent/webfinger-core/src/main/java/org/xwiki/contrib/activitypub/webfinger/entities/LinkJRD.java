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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.
 *
 * @version $Id$
 * @since 1.1
 */
public class LinkJRD
{
    private String rel;

    private String type;

    private String href;

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.1.
     * @return the rel value.
     */
    public String getRel()
    {
        return this.rel;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.1.
     * @param rel the rel value.
     * @return self to allow chained method calls (fluent API).
     */
    public LinkJRD setRel(String rel)
    {
        this.rel = rel;
        return this;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.2.
     * @return the type.
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.2.
     * @param type the type.
     * @return self to allow chained method calls (fluent API).
     */
    public LinkJRD setType(String type)
    {
        this.type = type;
        return this;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.3.
     * @return the href.
     */
    public String getHref()
    {
        return this.href;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.3.
     * @param href the href.
     * @return self to allow chained method calls (fluent API).
     */
    public LinkJRD setHref(String href)
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

        LinkJRD linkJRD = (LinkJRD) o;

        return new EqualsBuilder()
                   .append(this.rel, linkJRD.rel)
                   .append(this.type, linkJRD.type)
                   .append(this.href, linkJRD.href)
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

    @Override public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("rel", this.getRel())
                   .append("type", this.getType())
                   .append("href", this.getHref())
                   .build();
    }
}
