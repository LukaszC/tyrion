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

import fr.pingtimeout.tyrion.agent.LockInterceptorStaticAccessor;
import fr.pingtimeout.tyrion.util.SimpleLogger;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.AdviceAdapter;

import static fr.pingtimeout.tyrion.transformation.SynchronizedMethodVisitor.isStatic;

class SynchronizedMethodWrapper extends AdviceAdapter {


    private final String methodName;
    private final String className;


    protected SynchronizedMethodWrapper(int api, MethodVisitor mv, int access, String name, String desc, String className) {
        super(Opcodes.ASM4, mv, access, name, desc);

        this.methodName = name;
        this.className = className.replaceAll("/", ".");
    }


    @Override
    protected void onMethodEnter() {
        SimpleLogger.debug("Entering synchronized method %s", methodName);

        if (isStatic(methodAccess)) {
            // Retrieve Class object that is subject to lock
            mv.visitLdcInsn(className);
            mv.visitMethodInsn(INVOKESTATIC,
                    "java/lang/Class",
                    "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        } else {
            // Retrieve Object that is subject to lock
            mv.visitVarInsn(ALOAD, 0);
        }

        mv.visitMethodInsn(INVOKESTATIC,
                LockInterceptorStaticAccessor.CLASS_FQN,
                LockInterceptorStaticAccessor.ENTER_METHOD_NAME, LockInterceptorStaticAccessor.ENTER_EXIT_METHOD_SIGNATURE);

        super.onMethodEnter();
    }


    @Override
    protected void onMethodExit(int opcode) {
        SimpleLogger.debug("Leaving synchronized method %s", methodName);

        if (isStatic(methodAccess)) {
            // Retrieve Class object that is subject to lock
            mv.visitLdcInsn(className);
            mv.visitMethodInsn(INVOKESTATIC,
                    "java/lang/Class",
                    "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        } else {
            // Retrieve Object that is subject to lock
            mv.visitVarInsn(ALOAD, 0);
        }

        mv.visitMethodInsn(INVOKESTATIC,
                LockInterceptorStaticAccessor.CLASS_FQN,
                LockInterceptorStaticAccessor.EXIT_METHOD_NAME, LockInterceptorStaticAccessor.ENTER_EXIT_METHOD_SIGNATURE);

        super.onMethodExit(opcode);
    }
}