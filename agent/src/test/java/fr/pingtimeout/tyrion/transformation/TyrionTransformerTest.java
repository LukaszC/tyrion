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

import fr.pingtimeout.tyrion.agent.LockInterceptor;
import fr.pingtimeout.tyrion.agent.LockInterceptorStaticAccessor;
import fr.pingtimeout.tyrion.transformation.source.MeasurePoints;
import fr.pingtimeout.tyrion.transformation.source.MeasurePointsStaticAccessor;
import fr.pingtimeout.tyrion.transformation.source.ProtectedBlock;
import fr.pingtimeout.tyrion.transformation.source.SynchronizationBlock;
import javassist.ClassPool;
import javassist.CtClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;

import static fr.pingtimeout.tyrion.transformation.CheckLockStateAnswer.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TyrionTransformerTest {

    private TyrionTransformer instance = new TyrionTransformer();

    @Mock
    MeasurePoints measurePoints;

    @Mock
    LockInterceptor lockInterceptor;

    @Before
    public void before() {
        MeasurePointsStaticAccessor.INSTANCE = measurePoints;
        LockInterceptorStaticAccessor.lockInterceptor = lockInterceptor;
    }

    @Test
    public void shouldInstrumentSynchronizationBlock() throws Exception {
        ProtectedBlock result = transformClass(SynchronizationBlock.class);
        Object lock = result.getLock();
        assertLockIsNotTaken(lock, measurePoints).measurePoint1();
        assertLockIsNotTaken(lock, lockInterceptor).enteringCriticalSection(any());
        assertLockIsTaken(lock, lockInterceptor).enteredCriticalSection(any());
        assertLockIsTaken(lock, measurePoints).measurePoint2();
        assertLockIsTaken(lock, lockInterceptor).leavingCriticalSection(any());
        assertLockIsNotTaken(lock, measurePoints).measurePoint3();

        result.invoke();

        InOrder inOrder = inOrder(measurePoints, lockInterceptor);
        inOrder.verify(measurePoints).measurePoint1();
        inOrder.verify(lockInterceptor).enteringCriticalSection(lock);
        inOrder.verify(lockInterceptor).enteredCriticalSection(lock);
        inOrder.verify(measurePoints).measurePoint2();
        inOrder.verify(lockInterceptor).leavingCriticalSection(lock);
        inOrder.verify(measurePoints).measurePoint3();
        verifyNoMoreInteractions(measurePoints, lockInterceptor);
    }

    @SuppressWarnings("unchecked")
    public <T> T transformClass(Class<? extends T> clazz) throws Exception {
        String className = clazz.getName();
        ClassPool classPool = ClassPool.getDefault();

        CtClass ctClass = classPool.get(className);
        ctClass.setName(className + "TyrionTransformed");
        byte[] classBytes = ctClass.toBytecode();
        ctClass.defrost();

        byte[] resultBytes = instance.transform(className, classBytes);

        CtClass resultCtClass = classPool.makeClass(new ByteArrayInputStream(resultBytes));
        return (T) resultCtClass.toClass().newInstance();
    }

    public <T> T assertLockIsNotTaken(Object lock, T mock) {
        return doAnswer(assertLockIsNotTakenAnswer(lock)).when(mock);
    }

    public <T> T assertLockIsTaken(Object lock, T mock) {
        return doAnswer(assertLockIsTakenAnswer(lock)).when(mock);
    }

}
