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
package org.xwiki.contrib.activitypub.internal.json.absolute;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.internal.json.AbstractActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.internal.json.ObjectMapperConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default implementation of {@link ActivityPubJsonSerializer}.
 * This implementation will keep all URI absolute when serializing the information.
 *
 * @version $Id$
 * @since 1.2
 */
@Component
@Singleton
public class DefaultActivityPubJsonSerializer extends AbstractActivityPubJsonSerializer
{
    @Inject
    private ObjectMapperConfiguration objectMapperConfiguration;

    @Override
    public ObjectMapper getObjectMapper()
    {
        return this.objectMapperConfiguration.getObjectMapper();
    }
}
