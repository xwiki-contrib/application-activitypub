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

import java.net.URI;
import java.util.List;

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A Link is an indirect, qualified reference to a resource identified by a URL.
 *
 * @version $Id$
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-link">Activity Stream vocabulary Link
 *     definition</a>
 * @since 1.4
 */
@Unstable
@JsonDeserialize(as = Link.class)
public class Link extends ActivityPubObject
{
    private URI href;

    private List<String> rel;

    private String mediaType;

    private String hreflang;

    private Integer height;

    private Integer width;

    private ActivityPubObjectReference<? extends ActivityPubObject> preview;

    /**
     * The target resource pointed to by a {@link Link}.
     *
     * @return the target resource {@link URI}
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-href">Activity Stream vocabulary href
     *     definition</a>
     */
    public URI getHref()
    {
        return this.href;
    }

    /**
     * The target resource pointed to by a {@link Link}.
     *
     * @param href the target resource {@link URI}
     * @return the current object
     * @param <T> the dynamic type of the object
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-href">Activity Stream vocabulary href
     *     definition</a>
     */
    public <T extends Link> T setHref(URI href)
    {
        this.href = href;
        return (T) this;
    }

    /**
     * A link relation associated with a {@link Link}.
     *
     * @return the list of values
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-rel">Activity Stream vocabulary rel
     *     definition</a>
     */
    public List<String> getRel()
    {
        return this.rel;
    }

    /**
     * A link relation associated with a {@link Link}.
     *
     * @param rel the list of values
     * @return the current object
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-rel">Activity Stream vocabulary rel
     *     definition</a>
     */
    public Link setRel(List<String> rel)
    {
        this.rel = rel;
        return this;
    }

    /**
     * When used on a {@link Link}, identifies the MIME media type of the referenced resource.
     *
     * @return the media type of the link
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-mediatype">Activity Stream vacabulary
     *     mediaType definition</a>
     */
    public String getMediaType()
    {
        return this.mediaType;
    }

    /**
     * When used on a {@link Link}, identifies the MIME media type of the referenced resource.
     *
     * @param mediaType the media type of the link
     * @return the current object
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-mediatype">Activity Stream vacabulary
     *     mediaType definition</a>
     */
    public Link setMediaType(String mediaType)
    {
        this.mediaType = mediaType;
        return this;
    }

    /**
     * Hints as to the language used by the target resource.
     *
     * @return the language used by the target resource of the link
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-hreflang">Activity Stream vocabulary
     *     hrefLang definition</a>
     */
    public String getHreflang()
    {
        return this.hreflang;
    }

    /**
     * Hints as to the language used by the target resource.
     *
     * @param hreflang the language used by the target resource of the link
     * @return the current object
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-hreflang">Activity Stream vocabulary
     *     hrefLang definition</a>
     */
    public Link setHreflang(String hreflang)
    {
        this.hreflang = hreflang;
        return this;
    }

    /**
     * On a {@link Link}, specifies a hint as to the rendering height in device-independent pixels of the linked
     * resource.
     *
     * @return the height of the link in pixels
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-height">Activity Stream vocabulary height
     *     definition</a>
     */
    public Integer getHeight()
    {
        return this.height;
    }

    /**
     * On a {@link Link}, specifies a hint as to the rendering height in device-independent pixels of the linked
     * resource.
     *
     * @param height the height of the link in pixels
     * @return the current object
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-height">Activity Stream vocabulary height
     *     definition</a>
     */
    public Link setHeight(Integer height)
    {
        this.height = height;
        return this;
    }

    /**
     * On a {@link Link}, specifies a hint as to the rendering width in device-independent pixels of the linked
     * resource.
     *
     * @return the width of the link in pixels
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-width">Activity Stream vocabulary width
     *     definition</a>
     */
    public Integer getWidth()
    {
        return this.width;
    }

    /**
     * On a {@link Link}, specifies a hint as to the rendering width in device-independent pixels of the linked
     * resource.
     *
     * @param width the width of the link in pixels
     * @return the current object
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-width">Activity Stream vocabulary width
     *     definition</a>
     */
    public Link setWidth(Integer width)
    {
        this.width = width;
        return this;
    }

    /**
     * Identifies an entity that provides a preview of this {@link Link}.
     *
     * @return the link to an entity, or an object providing the preview of this {@link Link}
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-preview">Activity Pub vocabulary preview
     *     definition</a>
     */
    public ActivityPubObjectReference<? extends ActivityPubObject> getPreview()
    {
        return this.preview;
    }

    /**
     * Identifies an entity that provides a preview of this {@link Link}.
     *
     * @param preview the link to an entity, or an object providing the preview of this {@link Link}
     * @return the current object
     */
    public Link setPreview(ActivityPubObjectReference<? extends ActivityPubObject> preview)
    {
        this.preview = preview;
        return this;
    }
}
