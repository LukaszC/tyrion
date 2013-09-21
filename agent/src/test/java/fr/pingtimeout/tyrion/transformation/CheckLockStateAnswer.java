/*
 * Copyright (c) 2013, Lukasz Celeban
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

package fr.pingtimeout.tyrion.transformation;


import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

public class CheckLockStateAnswer implements Answer<Object> {

    public static CheckLockStateAnswer assertLockIsTakenAnswer(Object lock) {
        return new CheckLockStateAnswer(lock, true);
    }

    public static CheckLockStateAnswer assertLockIsNotTakenAnswer(Object lock) {
        return new CheckLockStateAnswer(lock, false);
    }

    private static boolean isTaken(final Object lock) {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        Future<?> task = executorService.submit(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                }
            }
        });
        try {
            task.get(500, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            return true;
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            executorService.shutdownNow();
        }
        return false;
    }

    private final Object lock;
    private final boolean expectedTaken;

    private CheckLockStateAnswer(Object lock, boolean expectedTaken) {
        this.lock = lock;
        this.expectedTaken = expectedTaken;
    }

    @Override
    public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
        assertEquals("Lock state expected to be: " + (expectedTaken ? "TAKEN" : "NOT TAKEN") + ",", expectedTaken, isTaken(lock));
        return null;
    }

}
