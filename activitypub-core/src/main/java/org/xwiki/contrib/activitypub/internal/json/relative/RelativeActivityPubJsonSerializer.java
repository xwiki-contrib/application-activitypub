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
package org.xwiki.contrib.activitypub.internal.json.relative;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.internal.json.AbstractActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.internal.json.ObjectMapperConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * An implementation of {@link ActivityPubJsonSerializer} that relies on the {@link RelativeObjectMapperConfiguration}.
 *
 * @since 1.2
 * @version $Id$
 */
@Component
@Named("relative")
@Singleton
public class RelativeActivityPubJsonSerializer extends AbstractActivityPubJsonSerializer
{
    @Inject
    @Named("relative")
    private ObjectMapperConfiguration objectMapperConfiguration;

    @Override
    public ObjectMapper getObjectMapper()
    {
        return this.objectMapperConfiguration.getObjectMapper();
    }
}
