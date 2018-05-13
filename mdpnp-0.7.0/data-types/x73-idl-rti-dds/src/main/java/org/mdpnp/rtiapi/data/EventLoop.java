/*******************************************************************************
 * Copyright (c) 2014, MD PnP Program
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package org.mdpnp.rtiapi.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rti.dds.infrastructure.Condition;
import com.rti.dds.infrastructure.ConditionSeq;
import com.rti.dds.infrastructure.Duration_t;
import com.rti.dds.infrastructure.GuardCondition;
import com.rti.dds.infrastructure.RETCODE_TIMEOUT;
import com.rti.dds.infrastructure.WaitSet;
import com.rti.dds.infrastructure.WaitSetProperty_t;

/**
 * @author Jeff Plourde
 *
 */
public class EventLoop {

    private static final Logger log = LoggerFactory.getLogger(EventLoop.class);

    public interface ConditionHandler {
        void conditionChanged(Condition condition);
    }

    private final Map<Condition, ConditionHandler> conditionHandlers = new HashMap<Condition, ConditionHandler>();
    private final List<Mutation> queuedMutations = new ArrayList<Mutation>();
    private final List<Runnable> queuedRunnables = new ArrayList<Runnable>();
    private final WaitSet waitSet;
    private final GuardCondition mutate = new GuardCondition();
    private final GuardCondition runnable = new GuardCondition();

    protected void handleMutation(Mutation m) {
        if (m.isAdd()) {
            // log.debug("Handling an add mutation for " + m.getCondition());
            conditionHandlers.put(m.getCondition(), m.getConditionHandler());
            waitSet.attach_condition(m.getCondition());
        } else {
            // log.debug("Handling a remove mutation for " + m.getCondition());
            if (null == conditionHandlers.remove(m.getCondition())) {
                log.warn("Attempt to detach unknown condition:" + m.getCondition());
                for (int i = 0; i < m.getTrace().length; i++) {
                    log.warn("\tat " + m.getTrace()[i]);
                }
            } else {
                waitSet.detach_condition(m.getCondition());
            }
        }
        m.done();
    }

    private final ConditionHandler mutateHandler = new ConditionHandler() {
        @Override
        public void conditionChanged(Condition condition) {
            Mutation[] mutations = new Mutation[0];
            synchronized (queuedMutations) {
                mutations = queuedMutations.toArray(mutations);
                queuedMutations.clear();
                ((GuardCondition) condition).set_trigger_value(false);
            }
            for (Mutation m : mutations) {
                handleMutation(m);
            }
        }
    };

    private Thread currentServiceThread;

    private final ConditionHandler runnableHandler = new ConditionHandler() {
        public void conditionChanged(Condition condition) {
            Runnable[] runnables = new Runnable[0];
            synchronized (queuedRunnables) {
                runnables = queuedRunnables.toArray(runnables);
                queuedRunnables.clear();
                ((GuardCondition) condition).set_trigger_value(false);
            }
            for (Runnable r : runnables) {
                r.run();
            }
        }
    };

    private static class Mutation {
        private final boolean add;
        private final Condition condition;
        private final ConditionHandler conditionHandler;
        private final StackTraceElement[] trace;

        private boolean done = false;

        public Mutation(boolean add, Condition condition, ConditionHandler conditionHandler) {
            this.add = add;
            this.condition = condition;
            this.conditionHandler = conditionHandler;
            this.trace = Thread.currentThread().getStackTrace();
        }

        public boolean isAdd() {
            return add;
        }

        public Condition getCondition() {
            return condition;
        }

        public ConditionHandler getConditionHandler() {
            return conditionHandler;
        }

        public StackTraceElement[] getTrace() {
            return trace;
        }

        public synchronized void done() {
            this.done = true;
            this.notifyAll();
        }

        public synchronized void await() {
            while (!done) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public EventLoop() {
        this(null);
    }

    public EventLoop(WaitSetProperty_t properties) {
        waitSet = null == properties ? new WaitSet() : new WaitSet(properties);
        waitSet.attach_condition(mutate);
        waitSet.attach_condition(runnable);
        conditionHandlers.put(mutate, mutateHandler);
        conditionHandlers.put(runnable, runnableHandler);
    }
    
    private static final long WARNING_ELAPSED_TIME_NANOSECONDS = 100000000L;

    public boolean waitAndHandle(ConditionSeq condSeq, Duration_t dur) {
        // Only one thread at a time can currently service the event loop
        long giveup = dur.is_infinite() ? Long.MAX_VALUE : (System.currentTimeMillis() + dur.sec * 1000L + dur.nanosec / 1000000L);

        long now = System.currentTimeMillis();
        synchronized (this) {
            while (currentServiceThread != null && now < giveup) {
                if (dur.is_zero()) {
                    throw new RETCODE_TIMEOUT("Timed out waiting to become service thread");
                }
                try {
                    this.wait(giveup - now);
                } catch (InterruptedException e) {
                    log.error("Interrupted", e);
                }
                now = System.currentTimeMillis();
            }
            currentServiceThread = Thread.currentThread();
        }

        if (!dur.is_zero() && now >= giveup) {
            throw new RETCODE_TIMEOUT("Timed out waiting to become service thread");
        }

        condSeq.clear();
        try {
            waitSet.wait(condSeq, dur);
            for (int i = 0; i < condSeq.size(); i++) {
                Condition c = (Condition) condSeq.get(i);
                ConditionHandler ch = conditionHandlers.get(c);
                if (null != ch) {
                    long s = System.nanoTime();
                    ch.conditionChanged(c);
                    long elapsed = System.nanoTime() - s;
                    if(elapsed >= WARNING_ELAPSED_TIME_NANOSECONDS) {
                        log.warn(elapsed + "ns to service " + ch);
                    }
                } else {
                    log.warn("No ConditionHandler for Condition " + c);
                }
            }
            return true;
        } catch (RETCODE_TIMEOUT timeout) {
            return false;
        } finally {
            synchronized (this) {
                currentServiceThread = null;
                this.notifyAll();
            }
        }
    }

    public synchronized boolean isCurrentServiceThread() {
        return Thread.currentThread().equals(currentServiceThread);
    }

    public void addHandler(Condition condition, ConditionHandler conditionHandler) {
        Mutation m = new Mutation(true, condition, conditionHandler);
        if (isCurrentServiceThread()) {
            handleMutation(m);
        } else {
            synchronized (queuedMutations) {
                // log.debug("Queue add condition:"+condition);
                queuedMutations.add(m);
                mutate.set_trigger_value(true);
            }
            m.await();
        }
        // log.debug("addHandler complete for " + condition);
    }

    public void removeHandler(Condition condition) {

        Mutation m = new Mutation(false, condition, null);
        if (isCurrentServiceThread()) {
            handleMutation(m);
        } else {
            synchronized (queuedMutations) {
                // log.debug("Queue remove condition:"+condition);
                queuedMutations.add(m);
                mutate.set_trigger_value(true);
            }
            m.await();
        }
        // log.debug("removeHandler complete for " + condition);
    }

    public void doLater(Runnable r) {
        synchronized (queuedRunnables) {
            queuedRunnables.add(r);
            runnable.set_trigger_value(true);
        }
    }
    
    public void doNow(Runnable r) {
        if(isCurrentServiceThread()) {
            r.run();
        } else {
            NestedRunnable nr = new NestedRunnable(r);
            synchronized (queuedRunnables) {
                queuedRunnables.add(nr);
                runnable.set_trigger_value(true);
            }
            nr.waitTillDone();
        }
    }

    private static class NestedRunnable implements Runnable {
        private final Runnable runnable;
        private boolean done = false;
        
        public NestedRunnable(Runnable runnable) {
            this.runnable = runnable;
        }
        public void run() {
            try {
                runnable.run();
            } finally {
                synchronized(this) {
                    done = true;
                    this.notifyAll();
                }
            }
        }
        public synchronized void waitTillDone() {
            while(!done) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    log.error("Interrupted waiting for task completion", e);
                }
            }
        }
    }
}
