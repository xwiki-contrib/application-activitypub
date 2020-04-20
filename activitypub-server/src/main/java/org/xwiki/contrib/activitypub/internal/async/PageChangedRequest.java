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
import org.xwiki.rendering.block.XDOM;

/**
 * Create a request for handling a page creation event on ActivityPub asynchronously.
 *
 * @version $Id$
 * @since 1.2
 */
public class PageChangedRequest extends AbstractRequest
{
    private DocumentReference documentReference;

    private DocumentReference authorReference;

    private String documentTitle;

    private XDOM content;

    private Date creationDate;

    private String viewURL;

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
     * @return The title of the created page.
     */
    public String getDocumentTitle()
    {
        return this.documentTitle;
    }

    /**
     * @return the content of the created page.
     */
    public XDOM getContent()
    {
        return this.content;
    }

    /**
     * @return The date of creation of the page.
     */
    public Date getCreationDate()
    {
        return this.creationDate;
    }

    /**
     * @return The url to view the created page.
     */
    public String getViewURL()
    {
        return this.viewURL;
    }

    /**
     * 
     * @param documentReference Set the document reference.
     * @return The current object.
     */
    public PageChangedRequest setDocumentReference(DocumentReference documentReference)
    {
        this.documentReference = documentReference;
        return this;
    }

    /**
     * 
     * @param authorReference The author reference.
     * @return The current object.
     */
    public PageChangedRequest setAuthorReference(DocumentReference authorReference)
    {
        this.authorReference = authorReference;
        return this;
    }

    /**
     * 
     * @param documentTitle the document title.
     * @return The current object.
     */
    public PageChangedRequest setDocumentTitle(String documentTitle)
    {
        this.documentTitle = documentTitle;
        return this;
    }

    /**
     * 
     * @param content The content of the page.
     * @return The current object.
     */
    public PageChangedRequest setContent(XDOM content)
    {
        this.content = content;
        return this;
    }

    /**
     * 
     * @param creationDate the creation date of the page.
     * @return The current object.
     */ 
    public PageChangedRequest setCreationDate(Date creationDate)
    {
        this.creationDate = creationDate;
        return this;
    }

    /**
     * 
     * @param viewURL The url of the page.
     * @return The current object.
     */
    public PageChangedRequest setViewURL(String viewURL)
    {
        this.viewURL = viewURL;
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

        PageChangedRequest that = (PageChangedRequest) o;

        return new EqualsBuilder()
                   .append(this.documentReference, that.documentReference)
                   .append(this.authorReference, that.authorReference)
                   .append(this.documentTitle, that.documentTitle)
                   .append(this.content, that.content)
                   .append(this.creationDate, that.creationDate)
                   .append(this.viewURL, that.viewURL)
                   .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
                   .append(this.documentReference)
                   .append(this.authorReference)
                   .append(this.documentTitle)
                   .append(this.content)
                   .append(this.creationDate)
                   .append(this.viewURL)
                   .toHashCode();
    }
}
