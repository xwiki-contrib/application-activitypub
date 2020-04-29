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
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.contrib.activitypub.internal.json.ActivityPubObjectDeserializer;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * Represents an object as defined in ActivityStream vocabulary. Most of the other ActivityStream entities inherits
 * from it.
 * Note that this class is a POJO to be used by the JSON serializer and parser so most of the methods are dumb
 * getters/setters .
 * Only {@link #getType()} and {@link #setType(String)} are a bit special since they don't rely on stored information.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-object">ActivityStream Object definition</a>
 * @version $Id$
 * @since 1.0
 */
@Unstable
@JsonDeserialize(using = ActivityPubObjectDeserializer.class)
public class ActivityPubObject extends JSONLDObjects
{
    private URI id;

    private String name;

    private Date published;

    private String summary;

    private List<ProxyActor> to;

    private String content;

    private List<ActivityPubObjectReference<AbstractActor>> attributedTo;

    private List<URI> url;

    private ActivityPubObjectReference<OrderedCollection<Announce>> shares;

    private String xwikiReference;

    private Set<AbstractActor> computedTargets;

    private Date lastUpdated;

    /**
     * The type is not stored as a property but instead we rely on the class name to return it.
     *
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
        if (!StringUtils.isEmpty(type) && !getType().equalsIgnoreCase(type)) {
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
    public List<ProxyActor> getTo()
    {
        return to;
    }

    /**
     * @param to the list of references of the actors the object is targeted to.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-to">ActivityStream definition</a>
     */
    public <T extends ActivityPubObject> T setTo(List<ProxyActor> to)
    {
        this.to = to;
        return (T) this;
    }

    /**
     * @return the list of references of the actors the object is attributed to.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-attributedto">ActivityStream definition</a>
     */
    public List<ActivityPubObjectReference<AbstractActor>> getAttributedTo()
    {
        return attributedTo;
    }

    /**
     * @param attributedTo the list of references of the actors the object is attributed to.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-attributedto">ActivityStream definition</a>
     */
    public <T extends ActivityPubObject> T setAttributedTo(List<ActivityPubObjectReference<AbstractActor>> attributedTo)
    {
        this.attributedTo = attributedTo;
        return (T) this;
    }

    /**
     * @return the list of URLs that represents the object.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-url">ActivityStream definition</a>
     */
    public List<URI> getUrl()
    {
        return url;
    }

    /**
     * @param url the list of URLs that represents the object.
     * @param <T> the type of the object.
     * @return the current object for fluent API.
     * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/#dfn-url">ActivityStream definition</a>
     */
    public <T extends ActivityPubObject> T setUrl(List<URI> url)
    {
        this.url = url;
        return (T) this;
    }

    /**
     * @param <T> the concrete type of the current instance.
     * @return a reference for the current instance.
     */
    @JsonIgnore
    public <T extends ActivityPubObject> ActivityPubObjectReference<T> getReference()
    {
        return new ActivityPubObjectReference<T>().setObject((T) this);
    }

    /**
     * @return the list of shares of the object.
     */
    public ActivityPubObjectReference<OrderedCollection<Announce>> getShares()
    {
        return this.shares;
    }

    /**
     * @param shares The list of shares of the object.
     * @return The current object.
     * @param <T> The runtime type of the chained object.
     */
    public <T extends ActivityPubObject> T setShares(ActivityPubObjectReference<OrderedCollection<Announce>> shares)
    {
        this.shares = shares;
        return (T) this;
    }
    

    /**
     * An XWiki specific field that allows to retrieve an entity on the wiki instance.
     * This can be use for example to retrieve a XWikiDocument easily.
     *
     * @return a specific XWiki reference to the current entity.
     */
    public String getXwikiReference()
    {
        return xwikiReference;
    }

    /**
     * Specify the XWiki reference of an entity (a document, a user, ...).
     * The reference should serialized as an absolute reference to be stored.
     *
     * @param xwikiReference the absolute reference of the entity.
     * @param <T> the concrete type of the current instance.
     * @return a reference for the current instance.
     */
    public <T extends ActivityPubObject> T setXwikiReference(String xwikiReference)
    {
        this.xwikiReference = xwikiReference;
        return (T) this;
    }

    /**
     * Computed targets is the real list of targeted AbstractActor references, computed when an activity is delivered.
     *
     * @return the set of deduplicated concrete actors targets.
     */
    @JsonIgnore
    public Set<AbstractActor> getComputedTargets()
    {
        return computedTargets;
    }

    /**
     * Computed targets is the real list of targeted AbstractActor references, computed when an activity is delivered.
     *
     * @param computedTargets the list of concrete actors targets.
     * @param <T> the concrete type of this object.
     * @return the current instance.
     */
    @JsonIgnore
    public <T extends ActivityPubObject> T setComputedTargets(Set<AbstractActor> computedTargets)
    {
        this.computedTargets = computedTargets;
        return (T) this;
    }

    /**
     * @return {@code true} iff the {@link #getTo()} attribute contains a reference to public
     *          (see {@link ProxyActor#getPublicActor()}).
     */
    @JsonIgnore
    public boolean isPublic()
    {
        return this.to != null && this.to.contains(ProxyActor.getPublicActor());
    }

    /**
     * @return the date of the last time the object was stored in DB.
     */
    @JsonIgnore
    public Date getLastUpdated()
    {
        return lastUpdated;
    }

    /**
     * Set the date of the last time the object was stored in DB.
     *
     * @param lastUpdated the date of the last time the object was stored in DB.
     * @param <T> the concrete type of this object.
     * @return the current object.
     */
    @JsonIgnore
    public <T extends ActivityPubObject> T setLastUpdated(Date lastUpdated)
    {
        this.lastUpdated = lastUpdated;
        return (T) this;
    }

    /**
     * @return {@code true} only if the current object is a document of any kind.
     */
    @JsonIgnore
    public boolean isDocument()
    {
        return false;
    }

    /**
     * @return {@code true} only if the current object is an actor of any kind.
     */
    @JsonIgnore
    public boolean isActor()
    {
        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ActivityPubObject object = (ActivityPubObject) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(id, object.id)
                .append(name, object.name)
                .append(published, object.published)
                .append(summary, object.summary)
                .append(to, object.to)
                .append(content, object.content)
                .append(attributedTo, object.attributedTo)
                .append(url, object.url)
                .append(shares, object.shares)
                .append(xwikiReference, object.xwikiReference)
                .append(lastUpdated, object.lastUpdated)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(id)
                .append(name)
                .append(published)
                .append(summary)
                .append(to)
                .append(content)
                .append(attributedTo)
                .append(url)
                .append(shares)
                .append(xwikiReference)
                .append(lastUpdated)
                .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("type", getType())
            .append("id", getId())
            .append("name", getName())
            .append("published", getPublished())
            .append("summary", getSummary())
            .append("to", getTo())
            .append("attributedTo", getAttributedTo()).build();
    }
}
