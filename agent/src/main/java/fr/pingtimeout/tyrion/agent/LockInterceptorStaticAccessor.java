package fr.pingtimeout.tyrion.agent;

public class LockInterceptorStaticAccessor {
    public static final String CLASS_FQN = LockInterceptorStaticAccessor.class.getName().replace('.', '/');
    public static final String ENTER_METHOD_NAME = "enteredCriticalSection";
    public static final String EXIT_METHOD_NAME = "leavingCriticalSection";
    public static final String ENTER_EXIT_METHOD_SIGNATURE = "(Ljava/lang/Object;)V";

    public static LockInterceptor lockInterceptor = LockInterceptor.getInstance();

    public static void enteredCriticalSection(Object lock) {
        lockInterceptor.enteredCriticalSection(lock);
    }

    public static void leavingCriticalSection(Object lock) {
        lockInterceptor.leavingCriticalSection(lock);
    }
}
