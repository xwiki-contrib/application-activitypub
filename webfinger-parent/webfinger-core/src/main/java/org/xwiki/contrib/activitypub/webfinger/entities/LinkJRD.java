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

import java.util.HashMap;
import java.util.Map;

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

    private Map<String, String> titles = new HashMap<>();

    private Map<String, String> properties = new HashMap<>();

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

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.4.
     * @return the titles.
     */
    public Map<String, String> getTitles()
    {
        return this.titles;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.4.
     * @param titles the titles.
     * @return self to allow chained method calls (fluent API).
     */
    public LinkJRD setTitles(Map<String, String> titles)
    {
        this.titles = titles;
        return this;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.5.
     * @return the properties.
     */
    public Map<String, String> getProperties()
    {
        return this.properties;
    }

    /**
     * Cf https://tools.ietf.org/html/rfc7033#section-4.4.4.5.
     * @param properties the properties.
     * @return self to allow chained method calls (fluent API).
     */
    public LinkJRD setProperties(Map<String, String> properties)
    {
        this.properties = properties;
        return this;
    }
}
