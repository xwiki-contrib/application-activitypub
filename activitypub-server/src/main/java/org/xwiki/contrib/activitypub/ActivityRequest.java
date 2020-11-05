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
package org.xwiki.contrib.activitypub;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.contrib.activitypub.entities.AbstractActivity;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.stability.Unstable;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A request to be handled by an {@link ActivityHandler}.
 *
 * @param <T> the type of activity
 * @version $Id$
 * @since 1.0
 */
@Unstable
public class ActivityRequest<T extends AbstractActivity>
{
    private final AbstractActor actor;

    private final T activity;

    private final HttpServletRequest request;

    private final HttpServletResponse response;

    /**
     * Constructor without any response or request parameters: this should only be used in case of activity handling
     * inside the same instance (see {@link org.xwiki.contrib.activitypub.internal.listeners.DocumentCreatedEventListener}).
     *
     * @param actor the actor who received the activity
     * @param activity the activity to be handled
     */
    public ActivityRequest(AbstractActor actor, T activity)
    {
        this(actor, activity, null, null);
    }

    /**
     * Default constructor.
     *
     * @param actor the actor who received the activity
     * @param activity the activity to be handled
     * @param request the servlet request used to post the activity
     * @param response the corresponding servlet response
     */
    public ActivityRequest(AbstractActor actor, T activity, HttpServletRequest request, HttpServletResponse response)
    {
        this.actor = actor;
        this.activity = activity;
        this.request = request;
        this.response = response;
    }

    /**
     * @return the actor who received the activity.
     */
    public AbstractActor getActor()
    {
        return this.actor;
    }

    /**
     * @return the activity to be handled.
     */
    public T getActivity()
    {
        return this.activity;
    }

    /**
     * @return the request used to post the activity (might be null).
     */
    public HttpServletRequest getRequest()
    {
        return this.request;
    }

    /**
     * @return the response servlet to use for the answer of the handling (might be null).
     */
    public HttpServletResponse getResponse()
    {
        return this.response;
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

        ActivityRequest<?> that = (ActivityRequest<?>) o;

        return new EqualsBuilder()
            .append(this.actor, that.actor)
            .append(this.activity, that.activity)
            .append(this.request, that.request)
            .append(this.response, that.response)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(this.actor)
            .append(this.activity)
            .append(this.request)
            .append(this.response)
            .toHashCode();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this)
            .append("actor", this.getActor())
            .append("activity", this.getActivity())
            .build();
    }
}
