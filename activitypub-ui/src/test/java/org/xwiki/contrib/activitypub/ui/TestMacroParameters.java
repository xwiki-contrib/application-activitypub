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
package org.xwiki.contrib.activitypub.ui;

/**
 * Parameters of the test replacements of the macros of the discussions application.
 *
 * @version $Id$
 */
public class TestMacroParameters
{
    private String reference;

    private String namespace;

    private String type;

    private String listId;

    private String linkTemplate;

    /**
     * @return the reference of the discussion, or of the actor
     */
    public String getReference()
    {
        return this.reference;
    }

    /**
     * @param reference the reference of the discussion, or of the actor
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return the namespace of the discussion
     */
    public String getNamespace()
    {
        return this.namespace;
    }

    /**
     * @param namespace the namespace of the discussion
     */
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    /**
     * @return the type of the discussions
     */
    public String getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the discussions
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return the identifier of the list
     */
    public String getListId()
    {
        return this.listId;
    }

    /**
     * @param listId the identifier of the list
     */
    public void setListId(String listId)
    {
        this.listId = listId;
    }

    /**
     * @return the template of the links to the discussions
     */
    public String getLinkTemplate()
    {
        return this.linkTemplate;
    }

    /**
     * @param linkTemplate the template of the links to the discussions
     */
    public void setLinkTemplate(String linkTemplate)
    {
        this.linkTemplate = linkTemplate;
    }
}
