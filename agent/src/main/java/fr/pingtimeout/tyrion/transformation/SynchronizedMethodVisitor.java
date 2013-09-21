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

package fr.pingtimeout.tyrion.transformation;

import fr.pingtimeout.tyrion.util.SimpleLogger;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

class SynchronizedMethodVisitor extends ClassVisitor {


    private final String className;


    public SynchronizedMethodVisitor(int api, ClassVisitor cv, String className) {
        super(api, cv);
        this.className = className;
    }


    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final MethodVisitor nextVisitor = super.visitMethod(access, name, desc, signature, exceptions);

        if (isSynchronized(access)) {
            SimpleLogger.debug("Found synchronized method : %s %s::%s %s", accessToString(access), className, name, desc);
            return new SynchronizedMethodWrapper(api, nextVisitor, access, name, desc, className);
        }

        return nextVisitor;
    }


    public static boolean isSynchronized(int access) {
        return (access & Opcodes.ACC_SYNCHRONIZED) != 0;
    }


    public static boolean isStatic(int access) {
        return (access & Opcodes.ACC_STATIC) != 0;
    }


    public static String accessToString(int access) {
        Map<Integer, String> opCodes = opCodeToString();

        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, String> opCodeEntry : opCodes.entrySet()) {
            int opCode = opCodeEntry.getKey();
            String opCodeToString = opCodeEntry.getValue();

            if ((access & opCode) != 0) {
                result.append(opCodeToString);
            }
        }

        return result.toString();
    }

    private static Map<Integer, String> opCodeToString() {
        Map<Integer, String> opCodes = new HashMap<>();

        opCodes.put(Opcodes.ACC_ABSTRACT, " abstract");
        opCodes.put(Opcodes.ACC_BRIDGE, " bridge");
        opCodes.put(Opcodes.ACC_DEPRECATED, " deprecated");
        opCodes.put(Opcodes.ACC_FINAL, " final");
        opCodes.put(Opcodes.ACC_NATIVE, " native");
        opCodes.put(Opcodes.ACC_PRIVATE, " private");
        opCodes.put(Opcodes.ACC_PROTECTED, " protected");
        opCodes.put(Opcodes.ACC_PUBLIC, " public");
        opCodes.put(Opcodes.ACC_STATIC, " static");
        opCodes.put(Opcodes.ACC_STRICT, " strict");
        opCodes.put(Opcodes.ACC_SYNCHRONIZED, " synchronized");
        opCodes.put(Opcodes.ACC_SYNTHETIC, " synthetic");
        opCodes.put(Opcodes.ACC_VARARGS, " varargs");

        return opCodes;
    }
}
