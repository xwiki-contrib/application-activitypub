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
package org.xwiki.contrib.activitypub.internal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.activitypub.ActivityPubEvent;
import org.xwiki.contrib.activitypub.ActivityPubJsonSerializer;
import org.xwiki.contrib.activitypub.ActivityPubNotifier;
import org.xwiki.contrib.activitypub.ActivityPubObjectReferenceResolver;
import org.xwiki.contrib.activitypub.entities.AbstractActor;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Define the conversion from an {@link ActivityPubEvent} to a {@link org.xwiki.eventstream.internal.DefaultEvent}.
 * The component will set the activity in a parameter of the {@link org.xwiki.eventstream.internal.DefaultEvent} and
 * will ensure that the event have a defined user.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(ActivityPubNotifier.EVENT_TYPE)
public class ActivityPubRecordableEventConverter implements RecordableEventConverter
{
    /**
     * Key of the parameter where the activity is put.
     */
    public static final String ACTIVITY_PARAMETER_KEY = "activity";

    @Inject
    private RecordableEventConverter defaultConverter;

    @Inject
    private ActivityPubJsonSerializer activityPubJsonSerializer;

    @Inject
    private ActivityPubObjectReferenceResolver objectReferenceResolver;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> stringDocumentReferenceResolver;

    @Override
    public Event convert(RecordableEvent recordableEvent, String source, Object data) throws Exception
    {
        Event convertedEvent = this.defaultConverter.convert(recordableEvent, source, data);

        ActivityPubEvent<?> activityPubEvent = (ActivityPubEvent<?>) recordableEvent;
        Map<String, String> parameters = new HashMap<>(convertedEvent.getParameters());
        parameters.put(ACTIVITY_PARAMETER_KEY,
            this.activityPubJsonSerializer.serialize(activityPubEvent.getActivity()));
        convertedEvent.setParameters(parameters);
        convertedEvent.setType(ActivityPubNotifier.EVENT_TYPE);

        // FIXME: this is a really ugly hack to be sure we have a user in the notif, else it's never taken into account
        if (convertedEvent.getUser() == null) {
            AbstractActor actor = this.objectReferenceResolver
                .resolveReference(activityPubEvent.getActivity().getActor());
            DocumentReference userReference = stringDocumentReferenceResolver.resolve(actor.getPreferredUsername());
            convertedEvent.setUser(userReference);
        }
        return convertedEvent;
    }

    @Override
    public List<RecordableEvent> getSupportedEvents()
    {
        return Arrays.asList(new ActivityPubEvent<>(null, null));
    }
}
