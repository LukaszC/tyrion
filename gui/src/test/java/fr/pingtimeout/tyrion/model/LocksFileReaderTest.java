package fr.pingtimeout.tyrion.model;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class LocksFileReaderTest {


    @Test
    public void should_transform_events_into_accesses_accessible_by_thread() {
        // Given
        String events = ""
                + "{\"enter\":{\"timestamp\":100,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":101,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":102,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":103,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"enter\":{\"timestamp\":103,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":107,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n";
        InputStream inputStream = IOUtils.toInputStream(events);

        Accessor accessor1 = new Accessor(1L, "Thread 1");
        Accessor accessor2 = new Accessor(2L, "Thread 2");
        Target lock1 = new Target("java.lang.Class", 1690956574L);
        Target lock2 = new Target("java.lang.Object", 2112631749L);

        Access accessFromThread1ToLock1 = new Access(100L, 101L, accessor1, lock1);
        Access accessFromThread2ToLock2 = new Access(102L, 103L, accessor2, lock2);
        Access accessFromThread1ToLock2 = new Access(103L, 107L, accessor1, lock2);


        // When
        AccessReport accessReport = new LocksFileReader(inputStream).buildAccessReport();

        Set<Access> thread1CriticalSections = accessReport.retrieveCriticalSectionsFor(accessor1);
        Set<Access> thread2CriticalSections = accessReport.retrieveCriticalSectionsFor(accessor2);


        // Then
        assertThat(thread1CriticalSections).containsOnly(accessFromThread1ToLock1, accessFromThread1ToLock2);
        assertThat(thread2CriticalSections).containsOnly(accessFromThread2ToLock2);
    }


    @Test
    public void should_transform_events_into_accesses_accessible_by_monitor() {
        // Given
        String events = ""
                + "{\"enter\":{\"timestamp\":100,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":101,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":102,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":103,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"enter\":{\"timestamp\":103,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":107,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n";
        InputStream inputStream = IOUtils.toInputStream(events);

        Accessor accessor1 = new Accessor(1L, "Thread 1");
        Accessor accessor2 = new Accessor(2L, "Thread 2");
        Target lock1 = new Target("java.lang.Class", 1690956574L);
        Target lock2 = new Target("java.lang.Object", 2112631749L);

        Access accessFromThread1ToLock1 = new Access(100L, 101L, accessor1, lock1);
        Access accessFromThread2ToLock2 = new Access(102L, 103L, accessor2, lock2);
        Access accessFromThread1ToLock2 = new Access(103L, 107L, accessor1, lock2);


        // When
        AccessReport accessReport = new LocksFileReader(inputStream).buildAccessReport();

        Set<Access> lock1CriticalSections = accessReport.retrieveCriticalSectionsFor(lock1);
        Set<Access> lock2CriticalSections = accessReport.retrieveCriticalSectionsFor(lock2);


        // Then
        assertThat(lock1CriticalSections).containsOnly(accessFromThread1ToLock1);
        assertThat(lock2CriticalSections).containsOnly(accessFromThread1ToLock2, accessFromThread2ToLock2);
    }


    @Test
    public void should_count_accesses_by_monitor() {
        // Given
        String events = ""
                + "{\"enter\":{\"timestamp\":100,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":101,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":102,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":103,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"enter\":{\"timestamp\":103,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":107,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":2112631749,\"className\":\"java.lang.Object\"}}}\n";
        InputStream inputStream = IOUtils.toInputStream(events);

        Target lock1 = new Target("java.lang.Class", 1690956574L);
        Target lock2 = new Target("java.lang.Object", 2112631749L);


        // When
        AccessReport accessReport = new LocksFileReader(inputStream).buildAccessReport();

        int accessorsOfLock1 = accessReport.countDifferentAccessorsFor(lock1);
        int accessorsOfLock2 = accessReport.countDifferentAccessorsFor(lock2);


        // Then
        assertThat(accessorsOfLock1).isEqualTo(1);
        assertThat(accessorsOfLock2).isEqualTo(2);
    }


    @Test
    public void should_detect_frequent_accesses_with_margin() {
        // Given
        String events = ""
                + "{\"enter\":{\"timestamp\":95,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":96,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":100,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":101,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":102,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":103,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":108,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":110,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n";
        InputStream inputStream = IOUtils.toInputStream(events);

        Accessor accessor1 = new Accessor(1L, "Thread 1");
        Accessor accessor2 = new Accessor(2L, "Thread 2");
        Target lock1 = new Target("java.lang.Class", 1690956574L);

        Access accessFromThread1At95 = new Access(95L, 96L, accessor1, lock1);
        Access accessFromThread1At100 = new Access(100L, 101L, accessor1, lock1);
        Access accessFromThread2At102 = new Access(102L, 103L, accessor2, lock1);


        // When
        AccessReport accessReport = new LocksFileReader(inputStream).buildAccessReport();

        Set<Access> frequentAccesses = accessReport.retrieveFrequentAccesses(lock1, 4, TimeUnit.MILLISECONDS);


        // Then
        assertThat(frequentAccesses).containsOnly(accessFromThread1At95, accessFromThread1At100, accessFromThread2At102);
    }


    @Test
    public void should_detect_contended_accesses_with_margin() {
        // Given
        String events = ""
                + "{\"enter\":{\"timestamp\":95,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":96,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":100,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":101,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":102,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":103,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":108,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":110,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n";
        InputStream inputStream = IOUtils.toInputStream(events);

        Accessor accessor1 = new Accessor(1L, "Thread 1");
        Accessor accessor2 = new Accessor(2L, "Thread 2");
        Target lock1 = new Target("java.lang.Class", 1690956574L);

        Access accessFromThread1At100 = new Access(100L, 101L, accessor1, lock1);
        Access accessFromThread2At102 = new Access(102L, 103L, accessor2, lock1);


        // When
        AccessReport accessReport = new LocksFileReader(inputStream).buildAccessReport();

        Set<Access> contentedAccesses = accessReport.retrieveContendedAccesses(lock1, 4, TimeUnit.MILLISECONDS);


        // Then
        assertThat(contentedAccesses).containsOnly(accessFromThread1At100, accessFromThread2At102);
    }


    @Test
    public void should_detect_contended_accesses_on_all_locks() {
        // Given
        String events = ""
                + "{\"enter\":{\"timestamp\":100,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":101,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"enter\":{\"timestamp\":102,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"
                + "{\"exit\":{\"timestamp\":103,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n"

                + "{\"enter\":{\"timestamp\":110,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":9876543211,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":111,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":9876543211,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"enter\":{\"timestamp\":112,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":9876543211,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":113,\"accessor\":{\"id\":2,\"name\":\"Thread 2\"},\"target\":{\"hashcode\":9876543211,\"className\":\"java.lang.Object\"}}}\n"

                + "{\"enter\":{\"timestamp\":121,\"accessor\":{\"id\":3,\"name\":\"Thread 3\"},\"target\":{\"hashcode\":2222222222,\"className\":\"java.lang.Object\"}}}\n"
                + "{\"exit\":{\"timestamp\":122,\"accessor\":{\"id\":3,\"name\":\"Thread 3\"},\"target\":{\"hashcode\":2222222222,\"className\":\"java.lang.Object\"}}}\n"
                ;
        InputStream inputStream = IOUtils.toInputStream(events);

        Accessor accessor1 = new Accessor(1L, "Thread 1");
        Accessor accessor2 = new Accessor(2L, "Thread 2");
        Accessor accessor3 = new Accessor(3L, "Thread 3");
        Target lock1 = new Target("java.lang.Class", 1690956574L);
        Target lock2 = new Target("java.lang.Object", 9876543211L);
        Target lock3 = new Target("java.lang.Object", 2222222222L);

        Access accessOfLock1FromThread1At100 = new Access(100L, 101L, accessor1, lock1);
        Access accessOfLock1FromThread2At102 = new Access(102L, 103L, accessor2, lock1);
        Access accessOfLock2FromThread1At110 = new Access(110L, 111L, accessor1, lock2);
        Access accessOfLock2FromThread2At112 = new Access(112L, 113L, accessor2, lock2);


        // When
        AccessReport accessReport = new LocksFileReader(inputStream).buildAccessReport();

        Map<Target, Set<Access>> contentedAccesses = accessReport.retrieveAllContendedAccesses(1, TimeUnit.MILLISECONDS);


        // Then
        assertThat(contentedAccesses.get(lock1)).containsOnly(accessOfLock1FromThread1At100, accessOfLock1FromThread2At102);
        assertThat(contentedAccesses.get(lock2)).containsOnly(accessOfLock2FromThread1At110, accessOfLock2FromThread2At112);
        assertThat(contentedAccesses).hasSize(2);
    }

    @Test
    public void should_ignore_entering_event_for_now() {
        // TODO: this test is going to be removed once we will implement handling for entering event
        // Given
        String events = ""
                + "{\"entering\":{\"timestamp\":100,\"accessor\":{\"id\":1,\"name\":\"Thread 1\"},\"target\":{\"hashcode\":1690956574,\"className\":\"java.lang.Class\"}}}\n";
        InputStream inputStream = IOUtils.toInputStream(events);

         // When
        new LocksFileReader(inputStream).buildAccessReport();

        // Then
        // should not throw an exception
    }


}
