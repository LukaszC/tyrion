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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

public class TyrionTransformer {

    private static final boolean PRINT_BYTECODE = false;

    public byte[] transform(String className, byte[] classfileBuffer) {
        ClassReader reader = new ClassReader(classfileBuffer);
        ClassNode classNode = new ClassNode();
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        final ClassVisitor syncMethodsVisitor;
        if (PRINT_BYTECODE) {
            ClassVisitor traceClassVisitor = new TraceClassVisitor(writer, new PrintWriter(System.out));
            syncMethodsVisitor = new SynchronizedMethodVisitor(Opcodes.ASM4, traceClassVisitor, className);
        } else {
            syncMethodsVisitor = new SynchronizedMethodVisitor(Opcodes.ASM4, writer, className);
        }

        // Reader -> ClassNode -> SynchronizedMethodVisitor -> (TraceClassVisitor ->) Writer
        reader.accept(classNode, 0);
        interceptAllSynchronizedBlocks(classNode);
        classNode.accept(syncMethodsVisitor);


        return writer.toByteArray();
    }


    private void interceptAllSynchronizedBlocks(ClassNode classNode) {
        @SuppressWarnings("unchecked")
        List<MethodNode> methods = classNode.methods;

        int blocksIntercepted = 0;
        for (MethodNode methodNode : methods) {
            blocksIntercepted += interceptAllSynchronizedBlocks(classNode, methodNode);
        }

        if (blocksIntercepted != 0) {
            SimpleLogger.debug("Intercepted %s synchronized blocks in %s", blocksIntercepted, classNode.name);
        }
    }


    private int interceptAllSynchronizedBlocks(ClassNode classNode, MethodNode methodNode) {
        int numberOfBlocksIntercepted = 0;
        if (SynchronizedMethodVisitor.isSynchronized(methodNode.access)) {
            SimpleLogger.debug("%s::%s is synchronized, nothing to do here", classNode.name, methodNode.name);
        } else {
            SimpleLogger.debug("Intercepting all synchronized blocks of %s::%s", classNode.name, methodNode.name);
            numberOfBlocksIntercepted += interceptSynchronizedBlocks(methodNode);
        }
        return numberOfBlocksIntercepted;
    }


    private int interceptSynchronizedBlocks(MethodNode methodNode) {
        int numberOfBlocksIntercepted = interceptMonitorEnter(methodNode);
        interceptMonitorExit(methodNode);
        return numberOfBlocksIntercepted;
    }


    private int interceptMonitorEnter(MethodNode methodNode) {
        Collection<AbstractInsnNode> monitorEnterInsn = extractMonitorEnterInsn(methodNode);

        for (AbstractInsnNode monitorEnterInsnNode : monitorEnterInsn) {
            // Duplicate lock
            SimpleLogger.debug("Inserting DUP before %s", monitorEnterInsnNode);
            methodNode.instructions.insertBefore(monitorEnterInsnNode, new InsnNode(Opcodes.DUP));
            methodNode.instructions.insertBefore(monitorEnterInsnNode, new InsnNode(Opcodes.DUP));

            // Add invokestatic just before critical section
            SimpleLogger.debug("Inserting call to enteringSynchronizedBlock before %s", monitorEnterInsnNode);
            methodNode.instructions.insertBefore(monitorEnterInsnNode, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    LockInterceptorStaticAccessor.CLASS_FQN,
                    LockInterceptorStaticAccessor.ENTERING_METHOD_NAME, LockInterceptorStaticAccessor.ENTER_EXIT_METHOD_SIGNATURE));

            // Add invokestatic as first instruction of critical section
            AbstractInsnNode nodeAfterInterception = monitorEnterInsnNode.getNext();
            SimpleLogger.debug("Inserting call to enteredSynchronizedBlock before %s", nodeAfterInterception);
            methodNode.instructions.insertBefore(nodeAfterInterception, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    LockInterceptorStaticAccessor.CLASS_FQN,
                    LockInterceptorStaticAccessor.ENTER_METHOD_NAME, LockInterceptorStaticAccessor.ENTER_EXIT_METHOD_SIGNATURE));
        }

        return monitorEnterInsn.size();
    }


    private void interceptMonitorExit(MethodNode methodNode) {
        Collection<AbstractInsnNode> monitorExitInsn = extractMonitorExitInsn(methodNode);

        for (AbstractInsnNode monitorExitInsnNode : monitorExitInsn) {
            // Duplicate lock
            SimpleLogger.debug("Inserting DUP before %s", monitorExitInsnNode);
            methodNode.instructions.insertBefore(monitorExitInsnNode, new InsnNode(Opcodes.DUP));

            // Add invokestatic as last instruction of critical section
            methodNode.instructions.insertBefore(monitorExitInsnNode, new MethodInsnNode(Opcodes.INVOKESTATIC,
                    LockInterceptorStaticAccessor.CLASS_FQN,
                    LockInterceptorStaticAccessor.EXIT_METHOD_NAME, LockInterceptorStaticAccessor.ENTER_EXIT_METHOD_SIGNATURE));
        }
    }


    private Collection<AbstractInsnNode> extractMonitorEnterInsn(MethodNode methodNode) {
        return extractInstructions(methodNode, Opcodes.MONITORENTER);
    }


    private Collection<AbstractInsnNode> extractMonitorExitInsn(MethodNode methodNode) {
        return extractInstructions(methodNode, Opcodes.MONITOREXIT);
    }


    @SuppressWarnings("unchecked")
    private Collection<AbstractInsnNode> extractInstructions(MethodNode methodNode, int instructionToExtract) {
        Collection<AbstractInsnNode> monitorEnterInsn = new ArrayList<>();
        ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode insnNode = iterator.next();
            if (insnNode.getOpcode() == instructionToExtract) {
                monitorEnterInsn.add(insnNode);
            }
        }
        return monitorEnterInsn;
    }


}