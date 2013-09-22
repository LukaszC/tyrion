/*
 * Copyright (c) 2013, Pierre Laporte
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this work; if not, see <http://www.gnu.org/licenses/>.
 */

package fr.pingtimeout.tyrion.util;

import fr.pingtimeout.tyrion.model.*;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.extractProperty;

public class EventsHolderSingletonTest {
    @Test
    public void test_record_new_entering() {
        // Given
        Object lock = new Object();
        EventsHolderSingleton eventsHolder = EventsHolderSingleton.INSTANCE;
        Thread accessor = Thread.currentThread();
        CriticalSectionEvent criticalSectionEvent = new CriticalSectionEntering(accessor, lock);

        // When
        eventsHolder.recordNewEntering(accessor, lock);

        // Then
        assertThat(eventsHolder.getAndClearEventsListOf(accessor.getId()))
                .usingElementComparator(new CriticalSectionEventsWithoutTime())
                .containsOnly(criticalSectionEvent);
    }

    @Test
    public void test_record_new_entry() {
        // Given
        Object lock = new Object();
        EventsHolderSingleton eventsHolder = EventsHolderSingleton.INSTANCE;
        Thread accessor = Thread.currentThread();
        CriticalSectionEvent criticalSectionEvent = new CriticalSectionEntered(accessor, lock);

        // When
        eventsHolder.recordNewEntry(accessor, lock);

        // Then
        assertThat(eventsHolder.getAndClearEventsListOf(accessor.getId()))
                .usingElementComparator(new CriticalSectionEventsWithoutTime())
                .containsOnly(criticalSectionEvent);
    }

    @Test
    public void test_record_new_exit() {
        // Given
        Object lock = new Object();
        EventsHolderSingleton eventsHolder = EventsHolderSingleton.INSTANCE;
        Thread accessor = Thread.currentThread();
        CriticalSectionEvent criticalSectionEvent = new CriticalSectionExit(accessor, lock);

        // When
        eventsHolder.recordNewExit(accessor, lock);

        // Then
        assertThat(eventsHolder.getAndClearEventsListOf(accessor.getId()))
                .usingElementComparator(new CriticalSectionEventsWithoutTime())
                .containsOnly(criticalSectionEvent);
    }

    @Test
    public void test_record_entries_and_exit_multithreaded() throws InterruptedException {
        // Given
        final Object lock = new Object();
        final CountDownLatch startSignal = new CountDownLatch(1);
        final int numberOfThreads = 1000;
        final CountDownLatch doneSignal = new CountDownLatch(numberOfThreads);
        final EventsHolderSingleton eventsHolder = EventsHolderSingleton.INSTANCE;
        final Collection<Thread> threads = new ArrayList<>(numberOfThreads);
        for (int i = 0; i < numberOfThreads; i++) {
            Thread thread = new Thread(new Record2NewEntriesAndExit(startSignal, eventsHolder, lock, doneSignal));
            threads.add(thread);
            thread.start();
        }

        // When
        startSignal.countDown();
        doneSignal.await();

        // Then
        final List<Long> expectedThreadIds = extractProperty("id", Long.class).from(threads);
        final Set<Long> threadIds = eventsHolder.getThreadIds();
        final CriticalSectionEventsWithoutTime eventsWithoutTime = new CriticalSectionEventsWithoutTime();

        assertThat(threadIds).containsAll(expectedThreadIds);

        for (Thread thread : threads) {
            CriticalSectionEvent criticalSectionEnter = new CriticalSectionEntered(thread, lock);
            CriticalSectionEvent criticalSectionExit = new CriticalSectionExit(thread, lock);

            assertThat(eventsHolder.getAndClearEventsListOf(thread.getId()))
                    .usingElementComparator(eventsWithoutTime)
                    .containsOnly(criticalSectionEnter, criticalSectionExit);
        }
    }
}


class Record2NewEntriesAndExit implements Runnable {
    protected final CountDownLatch startSignal;
    protected final EventsHolderSingleton eventsHolder;
    protected final Object lock;
    protected final CountDownLatch doneSignal;

    Record2NewEntriesAndExit(CountDownLatch startSignal, EventsHolderSingleton eventsHolder, Object lock, CountDownLatch doneSignal) {
        this.startSignal = startSignal;
        this.eventsHolder = eventsHolder;
        this.lock = lock;
        this.doneSignal = doneSignal;
    }

    @Override
    public void run() {
        try {
            startSignal.await();
            record2EnterAndExit();
            doneSignal.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Assertions.fail(e.getMessage());
        }
    }

    protected void record2EnterAndExit() {
        eventsHolder.recordNewEntry(Thread.currentThread(), lock);
        eventsHolder.recordNewEntry(Thread.currentThread(), lock);
        eventsHolder.recordNewExit(Thread.currentThread(), lock);
        eventsHolder.recordNewExit(Thread.currentThread(), lock);
    }
}
