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

package fr.pingtimeout.tyrion.transformation.source;

import static fr.pingtimeout.tyrion.transformation.source.MeasurePointsStaticAccessor.*;

public class SynchronizationBlock implements ProtectedBlock {

    private final Object monitor = new Object();

    @Override
    public void invoke() {
        meausrePoint1();
        synchronized(monitor) {
            meausrePoint2();
        }
        meausrePoint3();
    }

    @Override
    public Object getLock() {
        return monitor;
    }

}
