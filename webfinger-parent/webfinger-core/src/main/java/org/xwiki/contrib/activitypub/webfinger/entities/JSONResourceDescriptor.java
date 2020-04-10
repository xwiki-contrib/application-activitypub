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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * A JSON Resource Descriptor object.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4">JSON Resource Descriptor (JRD) specifications</a>
 *
 * @version $Id$
 * @since 1.1
 */
@Unstable
public class JSONResourceDescriptor
{
    private String subject;

    private List<Link> links = new ArrayList<>();

    /**
     * Get the JRD subject value.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.1">Specifications of the JRD's subject</a>
     * @return the subject.
     */
    public String getSubject()
    {
        return this.subject;
    }

    /**
     * Set the JRD's subject value.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.1">Specifications of the JRD's subject</a>
     * @param subject the webfinger subject.
     * @return self to allow chained method calls (fluent API).
     */
    public JSONResourceDescriptor setSubject(String subject)
    {
        this.subject = subject;
        return this;
    }

    /**
     * Get the JRD's link values.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">Specifications of the JRD's links</a>
     * @return get the webfinger links.
     */
    public List<Link> getLinks()
    {
        return this.links;
    }

    /**
     * Set the JRD's link values.
     * @see <a href="https://tools.ietf.org/html/rfc7033#section-4.4.4">Specifications of the JRD's links</a>
     * @param links the webfinger links.
     * @return self to allow chained method calls (fluent API).
     */
    public JSONResourceDescriptor setLinks(List<Link> links)
    {
        this.links = links;
        return this;
    }

    /**
     *
     * @return the list of href of the links.
     */
    @JsonProperty
    public List<URI> getAliases()
    {
        return this.links.stream().map(Link::getHref).collect(Collectors.toList());
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

        JSONResourceDescriptor that = (JSONResourceDescriptor) o;

        return new EqualsBuilder()
                   .append(this.subject, that.subject)
                   .append(this.links, that.links)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.subject)
                   .append(this.links)
                   .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
                   .append("subject", this.getSubject())
                   .append("links", this.getLinks())
                   .build();
    }
}
