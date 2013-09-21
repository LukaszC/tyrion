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

import fr.pingtimeout.tyrion.transformation.TyrionTransformer;
import fr.pingtimeout.tyrion.util.SimpleLogger;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class LocksTransformer implements ClassFileTransformer {

    private final TyrionTransformer transformer;

    public LocksTransformer(TyrionTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        try {
            SimpleLogger.debug("Trying to transform %s...", className);
            return transformer.transform(className, classfileBuffer);
        } catch (RuntimeException ignored) {
            SimpleLogger.warn("Unable to transform class %s, returning the class buffer unchanged. Cause : %s",
                    className, ignored.getMessage());
            SimpleLogger.debug(ignored);
            return classfileBuffer;
        }
    }

}