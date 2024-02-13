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

import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.classloader.Containers;

import de.uni_mannheim.swt.lasso.arena.event.ArenaExecutionListener;

import de.uni_mannheim.swt.lasso.arena.event.DefaultExecutionListener;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import javassist.*;
import org.apache.commons.collections4.Bag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.sequence.ExecutableSequence;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;

/**
 * (Simple) DCG container for handling generating dynamic call graphs.
 *
 * @author Marcus Kessel
 */
public class DCGContainer extends Container {

    private static final Logger LOG = LoggerFactory
            .getLogger(DCGContainer.class);

    private final ClassPool classPool;

    /**
     * Creates a new class realm.
     *
     * @param containers      The class world this realm belongs to, must not be <code>null</code>.
     * @param id              The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     */
    public DCGContainer(Containers containers, String id, ClassLoader baseClassLoader) {
        super(containers, id, baseClassLoader);

        classPool = new ClassPool(null);
        classPool.appendClassPath(new LoaderClassPath(this));

//        try {
//            generateClass();
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
    }

//    /**
//     * generates unique {@link Invocations} class on the fly.
//     *
//     * @throws IOException
//     * @throws CannotCompileException
//     * @throws ClassNotFoundException
//     */
//    private void generateClass() throws IOException, CannotCompileException, ClassNotFoundException {
//        byte[] bytes = loadClassBytes(Invocations.class.getName());
//        CtClass ctClass = ClassPool.getDefault().makeClass(new ByteArrayInputStream(bytes));
//
//        ctClass.setName(ctClass.getName() + "_" + UUID.randomUUID().toString().replaceAll("-", ""));
//
//        this.invocationsClazz = defineAndLoadCustomClass(ctClass.getName(), ctClass.toBytecode(), false);
//    }

    @Override
    protected boolean instrumentClass(String name) {
        // FIXME
        return true;
    }

    @Override
    protected byte[] instrumentClassBytes(String name, byte[] bytes) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Loading instrumented code '{}'", name);
        }

        CtClass clazz = null;
        try {
            clazz = classPool.makeClass(new ByteArrayInputStream(bytes));
            // we are only interested in concrete classes
            if (!clazz.isInterface()) {
                CtBehavior[] behaviors = clazz.getDeclaredBehaviors();
                for (int i = 0; i < behaviors.length; i++) {
                    CtBehavior behavior = behaviors[i];

                    int modifiers = behavior.getModifiers();

                    // check for native methods and abstract methods (not interested in those)
                    if (!Modifier.isNative(modifiers)
                            && !Modifier.isAbstract(modifiers)
                            // no synthetic methods
                            && !StringUtils.contains(behavior.getName(), "$")) {
                        doInstrumentBehavior(clazz.getName(), behavior);
                    }
                }
                return clazz.toBytecode();
            } else {
                return bytes; // is interface
            }
        } catch (Throwable e) {
            e.printStackTrace();
            throw new RuntimeException(String.format("Cannot instrument class '%s'", name), e);
        } finally {
            // optimize
            if (clazz != null) {
                clazz.detach();
            }
        }
    }

    /**
     * Default behavior is to start measurement before sequence execution and to end it after sequence execution.
     *
     * @return
     */
    @Override
    public ArenaExecutionListener getArenaExecutionListener() {
        return new DefaultExecutionListener(this) {

            @Override
            public void onBeforeExecution(SequenceExecutionRecords results) {
                super.onBeforeExecution(results);

                // register
                Invocations.register(DCGContainer.this);
            }

            @Override
            public void onAfterExecution(SequenceExecutionRecords results) {
                super.onAfterExecution(results);

                Bag<String> calls = Invocations.calls.get(getId());
                Invocations.remove(DCGContainer.this);

                //
                DCGObservation observation = new DCGObservation(calls);
                results.addObservation("dcg", observation);
            }

            @Override
            public void onAfterStatement(SequenceExecutionRecord result, ExecutableSequence executableSequence, int i) {
                super.onAfterStatement(result, executableSequence, i);
            }

            @Override
            public void onBeforeSequence(SequenceExecutionRecord result, ExecutableSequence executableSequence) {
                super.onBeforeSequence(result, executableSequence);
            }

            @Override
            public void onAfterSequence(SequenceExecutionRecord result, ExecutableSequence executableSequence) {
                super.onAfterSequence(result, executableSequence);
            }
        };
    }

    private void doInstrumentBehavior(String className, CtBehavior behavior)
            throws NotFoundException, CannotCompileException {
        String behaviorName = behavior.getName();

        boolean constructor = behavior instanceof CtConstructor;
        if (constructor) {
            behaviorName = "<init>";
        }

        String invocationClass = Invocations.class.getName();

        String callee = String.format("\"%s::%s%s\"", className, behaviorName, behavior.getSignature());

        String id = StringUtils.wrap(getId(), '"');

        // no "this" instance -- static method
        if (Modifier.isStatic(behavior.getModifiers())) {
            behavior.insertBefore(String.format("%s.enter(%s, %s, null, $args);", invocationClass, id, callee));

            String after = String.format("%s.leave(%s, %s, null, (Object) ($w) $_, false);", invocationClass, id, callee);

            try {
                behavior.insertAfter(after);
            } catch (CannotCompileException e) {
                System.err.println("STATIC AFTER FAILED => " + after);

                // attempt alternative version
                behavior.insertAfter(String.format("%s.leave(%s, %s, null, ($r) $_, false);", invocationClass, id, callee));
            }

            CtClass exceptionType = ClassPool.getDefault().get("java.lang.Throwable");
            behavior.addCatch(String.format("{ %s.leave(%s, %s, null, $e, true); throw $e; }", invocationClass, id, callee), exceptionType);
        } else if (constructor) {
            // constructor call

            // FIXME disabled, as it causes issues
//                // issue here: called AFTER super() .. so super() might fail ..
//                CtConstructor constructor1 = (CtConstructor) behavior;
//                constructor1.insertBeforeBody("de.uni_mannheim.swt.lasso.observer.Journal.enter(" + callName + ", $0, $args);");
//

            // TODO maybe double-check for double push in Stack if we first and second push called

            behavior.insertBefore(String.format("%s.enter(%s, %s, null, $args);", invocationClass, id, callee));

            String after = String.format("%s.leave(%s, %s, null, (Object) ($w) $_, false);", invocationClass, id, callee);

            try {
                behavior.insertAfter(after);
            } catch (CannotCompileException e) {
                System.err.println("CONSTRUCTOR AFTER FAILED => " + after);

                // attempt alternative version
                behavior.insertAfter(String.format("%s.leave(%s, %s, null, ($r) $_, false);", invocationClass, id, callee));
            }

            CtClass exceptionType = ClassPool.getDefault().get("java.lang.Throwable");
            behavior.addCatch(String.format("{ %s.leave(%s, %s, null, $e, true); throw $e; }", invocationClass, id, callee), exceptionType);

        } else {
            // instance method
            behavior.insertBefore(String.format("%s.enter(%s, %s, $0, $args);", invocationClass, id, callee));

            String after = String.format("%s.leave(%s, %s, $0, (Object) ($w) $_, false);", invocationClass, id, callee);

            try {
                behavior.insertAfter(after);
            } catch (CannotCompileException e) {
                System.err.println("INSTANCE AFTER FAILED => " + after);

                // attempt alternative version
                behavior.insertAfter(String.format("%s.leave(%s, %s, $0, ($r) $_, false);", invocationClass, id, callee));
            }

            CtClass exceptionType = ClassPool.getDefault().get("java.lang.Throwable");
            behavior.addCatch(String.format("{ %s.leave(%s, %s, $0, $e, true); throw $e; }", invocationClass, id, callee), exceptionType);
        }
    }
}
