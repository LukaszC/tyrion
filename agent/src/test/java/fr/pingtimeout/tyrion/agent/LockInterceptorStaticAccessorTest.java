package fr.pingtimeout.tyrion.agent;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class LockInterceptorStaticAccessorTest {
    @Test
    public void should_delegate_entering_critical_section_to_LockInterceptor() {
        // Given
        LockInterceptor mockedLockInterceptor = mock(LockInterceptor.class);
        LockInterceptorStaticAccessor.lockInterceptor = mockedLockInterceptor;
        Object mockedTarget = mock(Object.class);

        // When
        LockInterceptorStaticAccessor.enteringCriticalSection(mockedTarget);

        // Then
        verify(mockedLockInterceptor).enteringCriticalSection(mockedTarget);
    }

    @Test
    public void should_delegate_entered_critical_section_to_LockInterceptor() {
        // Given
        LockInterceptor mockedLockInterceptor = mock(LockInterceptor.class);
        LockInterceptorStaticAccessor.lockInterceptor = mockedLockInterceptor;
        Object mockedTarget = mock(Object.class);

        // When
        LockInterceptorStaticAccessor.enteredCriticalSection(mockedTarget);

        // Then
        verify(mockedLockInterceptor).enteredCriticalSection(mockedTarget);
    }

    @Test
    public void should_delegate_leaving_critical_section_to_LockInterceptor() {
        // Given
        LockInterceptor mockedLockInterceptor = mock(LockInterceptor.class);
        LockInterceptorStaticAccessor.lockInterceptor = mockedLockInterceptor;
        Object mockedTarget = mock(Object.class);

        // When
        LockInterceptorStaticAccessor.leavingCriticalSection(mockedTarget);

        // Then
        verify(mockedLockInterceptor).leavingCriticalSection(mockedTarget);
    }
}
