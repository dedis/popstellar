package com.github.dedis.popstellar.repository;

import com.github.dedis.popstellar.model.objects.event.Event;
import com.github.dedis.popstellar.utility.error.UnknownEventException;

import java.util.Set;

import io.reactivex.Observable;

public interface EventRepository<T extends Event> {

  Observable<T> getEventObservable(String laoId, String eventId) throws UnknownEventException;

  Observable<Set<String>> getEventIdsObservable(String laoId);

  Class<T> getType();
}
