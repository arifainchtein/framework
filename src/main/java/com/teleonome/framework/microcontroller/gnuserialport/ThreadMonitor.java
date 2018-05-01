package com.teleonome.framework.microcontroller.gnuserialport;

class ThreadMonitor implements Runnable {

    private final Thread thread;
    private final long timeout;

    /**
     * Start monitoring the current thread.
     *
     * @param timeout The timeout amount in milliseconds
     * or no timeout if the value is zero or less
     * @return The monitor thread or {@code null}
     * if the timeout amount is not greater than zero
     */
    public static Thread start(final long timeout) {
        return start(Thread.currentThread(), timeout);
    }

    /**
     * Start monitoring the specified thread.
     *
     * @param thread The thread The thread to monitor
     * @param timeout The timeout amount in milliseconds
     * or no timeout if the value is zero or less
     * @return The monitor thread or {@code null}
     * if the timeout amount is not greater than zero
     */
    public static Thread start(final Thread thread, final long timeout) {
        Thread monitor = null;
        if (timeout > 0) {
            final ThreadMonitor timout = new ThreadMonitor(thread, timeout);
            monitor = new Thread(timout, ThreadMonitor.class.getSimpleName());
            monitor.setDaemon(true);
            monitor.start();
        }
        return monitor;
    }

    /**
     * Stop monitoring the specified thread.
     *
     * @param thread The monitor thread, may be {@code null}
     */
    public static void stop(final Thread thread) {
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * Construct and new monitor.
     *
     * @param thread The thread to monitor
     * @param timeout The timeout amount in milliseconds
     */
    private ThreadMonitor(final Thread thread, final long timeout) {
        this.thread = thread;
        this.timeout = timeout;
    }

    /**
     * Sleep until the specified timeout amount and then
     * interrupt the thread being monitored.
     *
     * @see Runnable#run()
     */
    @Override
    public void run() {
        try {
            sleep(timeout);
            thread.interrupt();
        } catch (final InterruptedException e) {
            // timeout not reached
        }
    }

    /**
     * Sleep for a guaranteed minimum number of milliseconds unless interrupted.
     *
     * This method exists because Thread.sleep(100) can sleep for 0, 70, 100 or 200ms or anything else
     * it deems appropriate. Read the docs on Thread.sleep for further interesting details.
     *
     * @param ms the number of milliseconds to sleep for
     * @throws InterruptedException if interrupted
     */
    private static void sleep(final long ms) throws InterruptedException {
        final long finishAt = System.currentTimeMillis() + ms;
        long remaining = ms;
        do {
            Thread.sleep(remaining);
            remaining = finishAt - System.currentTimeMillis();
        } while (remaining > 0);
    }


}