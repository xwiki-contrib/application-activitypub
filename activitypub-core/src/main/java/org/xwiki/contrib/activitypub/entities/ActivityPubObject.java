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
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.contrib.activitypub.internal.json.ActivityPubObjectDeserializer;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an object as defined in ActivityStream vocabulary. Most of the other ActivityStream entities inherits
 * from it.
 * Note that this class is a POJO to be used by the JSON serializer and parser so most of the methods are dumb
 * getters/setters .
 * Only {@link #getType()} and {@link #setType(String)} are a bit special since they don't rely on stored information.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-object">ActivityStream Object definition</a>.
 */
@JsonDeserialize(using = ActivityPubObjectDeserializer.class)
public class ActivityPubObject extends JSONLDObjects
{
    private URI id;
    private String name;
    private Date published;
    private String summary;
    private List<ActivityPubObjectReference<Actor>> to;
    private String content;
    private List<ActivityPubObjectReference<Actor>> attributedTo;

    /**
     * The type is not stored as a property but instead we rely on the class name to return it.
     * @return the current type based on class name.
     */
    public String getType()
    {
        return getClass().getSimpleName();
    }

    /**
     * This setter only checks that the given type matches with the current class name.
     * It does not aim at being use by an API, but only to be used by the JSON parser to ensure everything's ok.
     *
     * @param type the type to check
     * @param <T> the object type
     * @return the current object for fluent API.
     */
    public <T extends ActivityPubObject> T setType(String type)
    {
        if (!StringUtils.isEmpty(type) && !getType().toLowerCase().equals(type.toLowerCase())) {
            throw new IllegalArgumentException(String.format("Error while parsing [%s]: illegal type [%s].",
                getClass().toString(), type));
        }
        return (T) this;
    }

    /**
     * @return the ID of the object.
     */
    public URI getId()
    {
        return id;
    }

    /**
     * @param id the URI specifying the ID of the object.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     */
    public <T extends ActivityPubObject> T setId(URI id)
    {
        this.id = id;
        return (T) this;
    }

    /**
     * @return the name of the object.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name of the object
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     */
    public <T extends ActivityPubObject> T setName(String name)
    {
        this.name = name;
        return (T) this;
    }

    /**
     * @return the publication date.
     */
    public Date getPublished()
    {
        return published;
    }

    /**
     * @param published the publication date.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     */
    public <T extends ActivityPubObject> T setPublished(Date published)
    {
        this.published = published;
        return (T) this;
    }

    /**
     * @return a summary of the object.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-summary">ActivityStream definition</a>
     */
    public String getSummary()
    {
        return summary;
    }

    /**
     * @param summary the summary to set.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-summary">ActivityStream definition</a>
     */
    public <T extends ActivityPubObject> T setSummary(String summary)
    {
        this.summary = summary;
        return (T) this;
    }

    /**
     * @return the content of the object.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-content">ActivityStream definition</a>
     */
    public String getContent()
    {
        return content;
    }

    /**
     * @param content the content to set.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-content">ActivityStream definition</a>
     */
    public <T extends ActivityPubObject> T setContent(String content)
    {
        this.content = content;
        return (T) this;
    }

    /**
     * @return the references of the actors the object is targeted to.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-to">ActivityStream definition</a>
     */
    public List<ActivityPubObjectReference<Actor>> getTo()
    {
        return to;
    }

    /**
     * @param to the list of references of the actors the object is targeted to.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-to">ActivityStream definition</a>
     */
    public <T extends ActivityPubObject> T setTo(List<ActivityPubObjectReference<Actor>> to)
    {
        this.to = to;
        return (T) this;
    }

    /**
     * @return the list of references of the actors the object is attributed to.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-attributedto">ActivityStream definition</a>
     */
    public List<ActivityPubObjectReference<Actor>> getAttributedTo()
    {
        return attributedTo;
    }

    /**
     * @param attributedTo the list of references of the actors the object is attributed to.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-attributedto">ActivityStream definition</a>
     */
    public <T extends ActivityPubObject> T setAttributedTo(List<ActivityPubObjectReference<Actor>> attributedTo)
    {
        this.attributedTo = attributedTo;
        return (T) this;
    }

    @Override
    public boolean equals(java.lang.Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActivityPubObject object = (ActivityPubObject) o;
        return Objects.equals(id, object.id) &&
            Objects.equals(name, object.name) &&
            Objects.equals(published, object.published) &&
            Objects.equals(summary, object.summary) &&
            Objects.equals(to, object.to) &&
            Objects.equals(content, object.content) &&
            Objects.equals(attributedTo, object.attributedTo);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id, name, published, summary, to, content, attributedTo);
    }
}
