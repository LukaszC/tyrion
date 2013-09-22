package fr.pingtimeout.tyrion.util;

import fj.data.List;
import fr.pingtimeout.tyrion.model.CriticalSectionEntered;
import fr.pingtimeout.tyrion.model.CriticalSectionEntering;
import fr.pingtimeout.tyrion.model.CriticalSectionEvent;
import fr.pingtimeout.tyrion.model.CriticalSectionExit;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public enum EventsHolderSingleton implements EventsHolder {
    INSTANCE;


    private Map<Long, AtomicReference<List<CriticalSectionEvent>>> events = new ConcurrentHashMap<>();

    @Override
    public void recordNewEntering(Thread accessor, Object objectUnderLock) {
        add(accessor, new CriticalSectionEntering(accessor, objectUnderLock));
    }

    @Override
    public void recordNewEntry(Thread accessor, Object objectUnderLock) {
        add(accessor, new CriticalSectionEntered(accessor, objectUnderLock));
    }

    @Override
    public void recordNewExit(Thread accessor, Object objectUnderLock) {
        add(accessor, new CriticalSectionExit(accessor, objectUnderLock));
    }


    private void add(Thread accessor, CriticalSectionEvent newEvent) {
        AtomicReference<List<CriticalSectionEvent>> accessorEventsRef = safeGet(accessor);
        List<CriticalSectionEvent> currentEventsList;
        List<CriticalSectionEvent> updatedEventsList;

        do {
            currentEventsList = accessorEventsRef.get();
            updatedEventsList = currentEventsList.cons(newEvent);
        } while (!accessorEventsRef.compareAndSet(currentEventsList, updatedEventsList));
    }


    private AtomicReference<List<CriticalSectionEvent>> safeGet(Thread accessor) {
        long accessorId = accessor.getId();
        if (events.get(accessorId) == null) {
            List<CriticalSectionEvent> accessorEvents = List.nil();
            AtomicReference<List<CriticalSectionEvent>> reference = new AtomicReference<>(accessorEvents);
            events.put(accessorId, reference);
        }

        return events.get(accessorId);
    }


    public Set<Long> getThreadIds() {
        return events.keySet();
    }


    public List<CriticalSectionEvent> getAndClearEventsListOf(Long threadId) {
        AtomicReference<List<CriticalSectionEvent>> accessorEventsRef = events.get(threadId);
        List<CriticalSectionEvent> currentEventsList;
        List<CriticalSectionEvent> emptyEventsList = List.nil();

        do {
            currentEventsList = accessorEventsRef.get();
        } while (!accessorEventsRef.compareAndSet(currentEventsList, emptyEventsList));

        return currentEventsList;
    }
}
