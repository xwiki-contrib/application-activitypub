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
package org.xwiki.contrib.activitypub.internal.async;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.job.AbstractRequest;
import org.xwiki.model.reference.DocumentReference;

/**
 * Create a request for handling a page creation event on ActivityPub asynchronously.
 *
 * @version $Id$
 * @since 1.2
 */
public class PageCreatedRequest extends AbstractRequest
{
    private final DocumentReference documentReference;

    private final DocumentReference authorReference;

    private final String viewURL;

    private final String documentTitle;

    private final Date creationDate;

    /**
     * Default constructor for page creation requests.
     *
     * @param documentReference DocumentReference of the created page.
     * @param authorReference   DocumentReference of the author of the page.
     * @param viewURL           The url to view the created page.
     * @param documentTitle     The title of the created page.
     * @param creationDate      The date of creation of the page.
     */
    public PageCreatedRequest(DocumentReference documentReference, DocumentReference authorReference,
        String viewURL, String documentTitle, Date creationDate)
    {
        this.documentReference = documentReference;
        this.authorReference = authorReference;
        this.viewURL = viewURL;
        this.documentTitle = documentTitle;
        this.creationDate = creationDate;
    }

    /**
     * @return The DocumentReference of the created page.
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     * @return The DocumentReference of the author of the page.
     */
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * @return The url to view the created page.
     */
    public String getViewURL()
    {
        return this.viewURL;
    }

    /**
     * @return The title of the created page.
     */
    public String getDocumentTitle()
    {
        return this.documentTitle;
    }

    /**
     * @return The date of creation of the page.
     */
    public Date getCreationDate()
    {
        return this.creationDate;
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

        PageCreatedRequest that = (PageCreatedRequest) o;

        return new EqualsBuilder()
                   .append(this.documentReference, that.documentReference)
                   .append(this.authorReference, that.authorReference)
                   .append(this.viewURL, that.viewURL)
                   .append(this.documentTitle, that.documentTitle)
                   .append(this.creationDate, that.creationDate)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.documentReference)
                   .append(this.authorReference)
                   .append(this.viewURL)
                   .append(this.documentTitle)
                   .append(this.creationDate)
                   .toHashCode();
    }
}
