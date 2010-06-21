package de.akquinet.gomobile.deploymentadmin;

/**
 * A semaphore, that maintains one single permit. An <code>acquire()</code> blocks until a permit is
 * available, whilst <code>release()</code> will unblock it.
 */
public class Semaphore {
    private boolean m_available;

    /**
     * Creates a new semaphore that is available.
     */
    public Semaphore() {
        m_available = true;
    }

    /**
     * Creates a new semaphore and allows you to specify if it's available or not.
     *
     * @param isAvailable should the semaphore be available or not
     */
    public Semaphore(boolean isAvailable) {
        m_available = isAvailable;
    }

    /**
     * Acquires the semaphore, or blocks until it's available or the thread is interrupted.
     *
     * @throws InterruptedException when the thread is interrupted
     */
    public void acquire() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        synchronized (this) {
            try {
                if (!m_available) {
                    wait();
                }
                m_available = false;
            }
            catch (InterruptedException ie) {
                notify();
                throw ie;
            }
        }
    }

    /**
     * Tries to acquire the semaphore and waits for the duration of the specified timeout
     * until it becomes available.
     *
     * @param timeout the number of milliseconds to wait
     * @return <code>true</code> if the semaphore was acquired, <code>false</code> if it was
     *     not after waiting for the specified amount of time
     * @throws InterruptedException when the thread is interrupted
     */
    public boolean tryAcquire(long timeout) throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        synchronized (this) {
            if (m_available) {
                m_available = false;
                return true;
            }
            else if (timeout <= 0) {
                return false;
            }
            else {
                long startTime = System.currentTimeMillis();
                try {
                    while (true) {
                        wait(timeout);
                        if (m_available) {
                            m_available = false;
                            return true;
                        }
                        else {
                            timeout -= (System.currentTimeMillis() - startTime);
                            if (timeout <= 0) {
                                return false;
                            }
                        }
                    }
                }
                catch (InterruptedException ie) {
                    notify();
                    throw ie;
                }
            }
        }
    }

    /**
     * Releases the semaphore. If threads were waiting, one of them is
     * notified.
     */
    public synchronized void release() {
        m_available = true;
        notify();
    }
}
