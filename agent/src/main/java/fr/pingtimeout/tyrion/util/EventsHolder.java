package fr.pingtimeout.tyrion.util;

public interface EventsHolder {
    void recordNewEntry(Thread accessor, Object objectUnderLock);

    void recordNewExit(Thread accessor, Object objectUnderLock);

    void recordNewEntering(Thread accessor, Object objectUnderLock);
}
