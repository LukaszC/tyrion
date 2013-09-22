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

package fr.pingtimeout.tyrion.agent;

import fr.pingtimeout.tyrion.util.EventsHolder;
import fr.pingtimeout.tyrion.util.EventsHolderSingleton;
import fr.pingtimeout.tyrion.util.SimpleLogger;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class LockInterceptor implements LockInterceptorMXBean {

    private static LockInterceptor INSTANCE = newLockInterceptor();


    private final EventsHolder eventsHolder;

    private final AtomicBoolean enabled;

    private final Set<String> excludedThreads;


    LockInterceptor(EventsHolder eventsHolder, AtomicBoolean enabled) {
        this.eventsHolder = eventsHolder;
        this.enabled = enabled;
        excludedThreads = new HashSet<>();
    }


    @Override
    public boolean isEnabled() {
        return enabled.get();
    }

    @Override
    public void setEnabled(boolean newState) {
        if (newState) {
            SimpleLogger.info("Enabling locks interception excepted for %s.", excludedThreads);
        } else {
            SimpleLogger.info("Disabling locks interception.");
        }
        this.enabled.getAndSet(newState);
    }

    // Note : this method is called dynamically
    public void enteringCriticalSection(Object lock) {
        if (enabled.get() && shouldIncludeThread()) {
            eventsHolder.recordNewEntering(Thread.currentThread(), lock);
        }
    }

    // Note : this method is called dynamically
    public void enteredCriticalSection(Object lock) {
        if (enabled.get() && shouldIncludeThread()) {
            eventsHolder.recordNewEntry(Thread.currentThread(), lock);
        }
    }

    // Note : this method is called dynamically
    public void leavingCriticalSection(Object lock) {
        if (enabled.get() && shouldIncludeThread()) {
            eventsHolder.recordNewExit(Thread.currentThread(), lock);
        }
    }

    private boolean shouldIncludeThread() {
        String currentThreadName = Thread.currentThread().getName();

        boolean causedByExcludedThread = false;
        for (String excludedThreadName : excludedThreads) {
            if (currentThreadName.equals(excludedThreadName)) {
                causedByExcludedThread = true;
            }
        }

        return !causedByExcludedThread;
    }


    public void addExcludedThread(String threadName) {
        this.excludedThreads.add(threadName);
    }


    private static LockInterceptor newLockInterceptor() {
        LockInterceptor lockInterceptor = new LockInterceptor(EventsHolderSingleton.INSTANCE, new AtomicBoolean(false));

        try {
            Class<LockInterceptor> lockInterceptorClass = LockInterceptor.class;
            String mxBeanName = String.format("%s:type=%s", lockInterceptorClass.getPackage().getName(), lockInterceptorClass.getSimpleName());
            ObjectName objectName = null;
            objectName = new ObjectName(mxBeanName);
            MBeanServer platformMBeanServer = ManagementFactory.getPlatformMBeanServer();
            platformMBeanServer.registerMBean(lockInterceptor, objectName);
        } catch (Exception e) {
            SimpleLogger.warn("Could not register LockInterceptor as an MX Bean. Locks interception cannot be enabled. Cause : %s", e.getMessage());
            SimpleLogger.debug(e);
        }

        return lockInterceptor;
    }

    public static LockInterceptor getInstance() {
        return INSTANCE;
    }
}
