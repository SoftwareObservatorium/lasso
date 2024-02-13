/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.arena.classloader.graph;

import org.apache.commons.collections4.Bag;
import org.apache.commons.collections4.bag.TreeBag;

import java.io.IOException;
import java.util.*;

/**
 * Helper class to record method calls which were instrumented by {@link DCGContainer}.
 *
 * @author Marcus Kessel
 */
public class Invocations {

    private static Map<String, Deque<String>> invocations = Collections.synchronizedMap(new LinkedHashMap<>());

    public static Map<String, Bag<String>> calls = Collections.synchronizedMap(new LinkedHashMap<>());

    private static long currentThreadId = -1;

    public static void enter(String id, String callee, Object ownerInstance, Object[] methodArgs) throws IOException {
        if(currentThreadId < 0) {
            currentThreadId = Thread.currentThread().getId();
        }

        if (currentThreadId != Thread.currentThread().getId()) {
            System.err.println(String.format(">>> DCG enter WRONG THREAD '%s' vs '%s'", currentThreadId, Thread.currentThread().getId()));

            //return;
        }

        System.out.println(String.format("DEBUG enter >> %s", String.join("|", id, callee)));

        String instanceId = getInstanceId(ownerInstance);

        // FIXME do something

        Deque<String> thisInvocations = invocations.get(id);
        Bag<String> thisCalls = calls.get(id);

        // who called us?
        String caller = thisInvocations.peek();
        if(caller != null) {
            thisCalls.add(String.format("%s->%s", caller, callee));
        } else {
            thisCalls.add(String.format("_START_->%s", callee));
        }

        thisInvocations.push(callee);
    }

    public static void leave(String id, String callee, Object ownerInstance, Object returnValue, boolean isExceptionThrown) throws IOException {
        if(currentThreadId < 0) {
            currentThreadId = Thread.currentThread().getId();
        }

        if (currentThreadId != Thread.currentThread().getId()) {
            System.err.println(String.format(">>> DCG enter WRONG THREAD '%s' vs '%s'", currentThreadId, Thread.currentThread().getId()));

            //return;
        }

        String instanceId = getInstanceId(ownerInstance);

        Deque<String> thisInvocations = invocations.get(id);

        System.out.println(String.format("DEBUG leave >> %s", String.join("|", id, callee)));


        // invocation entered
        String invocation = thisInvocations.pop();

        if (!invocation.equals(callee)) {
            System.out.println(String.format(">>> LEAVING INVOCATION. Unequal callees '%s' (enter) vs '%s' (leave)", invocation, callee));
        }

        // FIXME do something

        // constructor failed? then make sure to also fail callee inits
        if (isExceptionThrown && invocation.contains("<init>") && thisInvocations.size() > 0) {
            String outerInit = thisInvocations.peek();

            // LEAVE
            leave(id, outerInit, null, returnValue, isExceptionThrown);
        }
    }

    /**
     * We do the best we can to obtain a very "likely" unique identifier ..
     *
     * @param instance
     * @return
     */
    public static String getInstanceId(Object instance) {
        return Integer.toHexString(System.identityHashCode(instance));
    }

    public static void register(DCGContainer container) {
        invocations.put(container.getId(), new ArrayDeque<>());
        calls.put(container.getId(), new TreeBag<>());
    }

    public static void remove(DCGContainer container) {
        invocations.remove(container);
        calls.remove(container);
    }
}
