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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The representation of a webfinger object. Cf https://tools.ietf.org/html/rfc7033
 *
 * @version $Id$
 * @since 1.1
 */
@Unstable
public class WebfingerJRD
{
    private String subject;

    private List<LinkJRD> links = new ArrayList<>();

    /**
     *
     * @return the webfinger subject.
     */
    public String getSubject()
    {
        return this.subject;
    }

    /**
     *
     * @param subject the webfinger subject.
     * @return self to allow chained method calls (fluent API).
     */
    public WebfingerJRD setSubject(String subject)
    {
        this.subject = subject;
        return this;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#page-12.
     * @return get the webfinger links.
     */
    public List<LinkJRD> getLinks()
    {
        return this.links;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#page-12.
     * @param links the webfinger links.
     * @return self to allow chained method calls (fluent API).
     */
    public WebfingerJRD setLinks(List<LinkJRD> links)
    {
        this.links = links;
        return this;
    }

    /**
     *
     * @return the list of href of the links.
     */
    @JsonProperty
    public List<String> getAliases()
    {
        return this.links.stream().map(LinkJRD::getHref).collect(Collectors.toList());
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

        WebfingerJRD that = (WebfingerJRD) o;

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
