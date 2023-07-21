package com.baeldung.lockbykey;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockByKey {

    private static class LockWrapper {
        private final Lock lock = new ReentrantLock();
        private Integer numberOfThreadsInQueue = 1;

        private LockWrapper addThreadInQueue() {
            numberOfThreadsInQueue++;
            return this;
        }

        private int removeThreadFromQueue() {
            return --numberOfThreadsInQueue;
        }

    }

    private static final ConcurrentHashMap<String, LockWrapper> locks = new ConcurrentHashMap<>();

    public void lock(String key) {
        LockWrapper lockWrapper = locks.compute(key, (k, v) -> v == null ? new LockWrapper() : v.addThreadInQueue());
        lockWrapper.lock.lock();
    }

    public void unlock(String key) {
        locks.get(key).lock.unlock();
        //remove the lock in a thread safe manner, if this was the last thread using the lock
        locks.computeIfPresent(key, (k, v) -> v.removeThreadFromQueue() == 0 ? null : v);
    }

    public static int getNumberOfActiveLocks(){
        return locks.size();
    }

}
