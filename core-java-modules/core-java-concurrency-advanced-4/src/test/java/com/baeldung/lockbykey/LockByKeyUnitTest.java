package com.baeldung.lockbykey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class LockByKeyUnitTest {
    
    @Test
    void givenNoLockedKey_WhenLock_ThenSuccess() throws InterruptedException {
        AtomicBoolean threadWasExecuted = new AtomicBoolean(false);
        Thread thread = new Thread(() -> {
            String key = "key";
            LockByKey lockByKey = new LockByKey();
            lockByKey.lock(key);
            try {
               threadWasExecuted.set(true);
            } finally {
                lockByKey.unlock(key);
            }
        });
        try {
            thread.start();
            Thread.sleep(100);
        } finally {
            assertTrue(threadWasExecuted.get());
        }
    }
    
    @Test
    void givenLockedKey_WhenLock_ThenFailure() throws InterruptedException {
        String key = "key";
        LockByKey lockByKey = new LockByKey();
        lockByKey.lock(key);
        AtomicBoolean anotherThreadWasExecuted = new AtomicBoolean(false);
        Thread threadLockingOnAnotherKey = new Thread(() -> {
            LockByKey otherLockByKey = new LockByKey();
            otherLockByKey.lock(key);
            try {
                anotherThreadWasExecuted.set(true);
            } finally {
                otherLockByKey.unlock(key);
            }
        });
        try {
            threadLockingOnAnotherKey.start();
            Thread.sleep(100);
        } finally {
            assertFalse(anotherThreadWasExecuted.get());
            lockByKey.unlock(key);
        }
    }
    
    @Test
    void givenAnotherKeyLocked_WhenLock_ThenSuccess() throws InterruptedException {
        String key = "key";
        LockByKey lockByKey = new LockByKey();
        lockByKey.lock(key);
        AtomicBoolean anotherThreadWasExecuted = new AtomicBoolean(false);
        Thread threadLockingOnAnotherKey = new Thread(() -> {
            String anotherKey = "anotherKey";
            LockByKey otherLockByKey = new LockByKey();
            otherLockByKey.lock(anotherKey);
            try {
                anotherThreadWasExecuted.set(true);
            } finally {
                otherLockByKey.unlock(anotherKey);
            }
        });
        try {
            threadLockingOnAnotherKey.start();
            Thread.sleep(100);
        } finally {
            assertTrue(anotherThreadWasExecuted.get());
            lockByKey.unlock(key);
        }
    }
    
    @Test
    void givenUnlockedKey_WhenLock_ThenSuccess() throws InterruptedException {
        String key = "key";
        LockByKey lockByKey = new LockByKey();
        lockByKey.lock(key);
        AtomicBoolean anotherThreadWasExecuted = new AtomicBoolean(false);
        Thread threadLockingOnAnotherKey = new Thread(() -> {
            LockByKey otherLockByKey = new LockByKey();
            otherLockByKey.lock(key);
            try {
                anotherThreadWasExecuted.set(true);
            } finally {
                otherLockByKey.unlock(key);
            }
        });
        try {
            lockByKey.unlock(key);
            threadLockingOnAnotherKey.start();
            Thread.sleep(100);
        } finally {
            assertTrue(anotherThreadWasExecuted.get());
        }
    }

    private static class LockedIncrementor implements Runnable {
        private final List<Integer> counters;
        private final int numIncrements;

        private LockedIncrementor(List<Integer> counters, int numIncrements) {
            this.counters = counters;
            this.numIncrements = numIncrements;
        }

        @Override
        public void run() {
            LockByKey lockByKey = new LockByKey();
            for (int i = 0; i < numIncrements; i++) {
                for (int j = 0; j < counters.size(); j++) {
                    String key = "counter_" + j;
                    try {
                        lockByKey.lock(key);
                        counters.set(j, counters.get(j) + 1);
                    } finally {
                        lockByKey.unlock(key);
                    }
                }
            }
        }
    }

    @Test
    @Timeout(10)
    void givenCounterGuardedByLockByKey_WhenMultipleThreadsIncrementTheCounterConcurrently_ThenCounterIsIncrementedConsistently() {
        final int numIncrements = 1000;
        final int numCounters = 5;
        final int threadCount = 10;

        List<Integer> counters = new ArrayList<>();
        for (int i = 0; i < numCounters; i++) {
            counters.add(0);
        }

        List<Thread> incrementors = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            incrementors.add(new Thread(new LockedIncrementor(counters, numIncrements)));
        }
        incrementors.forEach(Thread::start);
        incrementors.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        //if locking worked correctly, each counter should have been incremented <numIncrements> times
        counters.forEach(counterValue -> assertEquals(threadCount * numIncrements, counterValue));
        assertEquals(0, LockByKey.getNumberOfActiveLocks());
    }

}
