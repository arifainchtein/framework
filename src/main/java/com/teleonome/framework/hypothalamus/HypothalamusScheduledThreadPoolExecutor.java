package com.teleonome.framework.hypothalamus;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.teleonome.framework.utils.Utils;

public class HypothalamusScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
	Logger logger;
    public HypothalamusScheduledThreadPoolExecutor(int corePoolSize) {
            super(corePoolSize);
            logger = Logger.getLogger(getClass());
    }

    @Override
    public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
    }

    private Runnable wrapRunnable(Runnable command) {
            return new LogOnExceptionRunnable(command);
    }

    private class LogOnExceptionRunnable implements Runnable {
            private Runnable theRunnable;

            public LogOnExceptionRunnable(Runnable theRunnable) {
                    super();
                    this.theRunnable = theRunnable;
            }

            @Override
            public void run() {
                    try {
                            theRunnable.run();
                    } catch (Exception e) {                            
                            System.err.println("error in executing: " + theRunnable + ". It will no longer be run!");
                            logger.warn(Utils.getStringException(e));
                            throw new RuntimeException(e);
                    }
            }
    }

    
}