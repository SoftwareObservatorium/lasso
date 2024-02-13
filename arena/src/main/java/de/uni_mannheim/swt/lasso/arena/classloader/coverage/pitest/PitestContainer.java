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
package de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest;

import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.classloader.Containers;
import de.uni_mannheim.swt.lasso.arena.event.ArenaExecutionListener;
import de.uni_mannheim.swt.lasso.arena.event.DefaultExecutionListener;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import org.apache.commons.lang3.StringUtils;

import org.pitest.mutationtest.engine.Mutant;

import org.pitest.mutationtest.engine.MutationDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import randoop.sequence.ExecutableSequence;

/**
 * Pitest container for handling {@link Mutant}s.
 *
 * @author Marcus Kessel
 */
public class PitestContainer extends Container {

    private static final Logger LOG = LoggerFactory
            .getLogger(PitestContainer.class);

    private final Mutant mutant;

    /**
     * Creates a new class realm.
     *
     * @param containers           The class world this realm belongs to, must not be <code>null</code>.
     * @param id              The identifier for this realm, must not be <code>null</code>.
     * @param baseClassLoader The base class loader for this realm, may be <code>null</code> to use the bootstrap class
     */
    public PitestContainer(Containers containers, String id, ClassLoader baseClassLoader, Mutant mutant) {
        super(containers, id, baseClassLoader);

        this.mutant = mutant;
    }

    @Override
    protected boolean instrumentClass(String name) {
        return StringUtils.equals(name, mutant.getDetails().getClassName().asJavaName());
    }

    @Override
    protected byte[] instrumentClassBytes(String name, byte[] bytes) {
        if(LOG.isInfoEnabled()) {
            LOG.info("Loading mutated code '{}'", mutant.getDetails());
        }

        return mutant.getBytes();
    }

    @Override
    public ArenaExecutionListener getArenaExecutionListener() {
        return new DefaultExecutionListener(this) {
            @Override
            public void onBeforeExecution(SequenceExecutionRecords results) {
                super.onBeforeExecution(results);
            }

            @Override
            public void onAfterExecution(SequenceExecutionRecords results) {
                try {
                    MutationDetails details = mutant.getDetails();
                    MutantObservation observation = new MutantObservation(details);
                    results.addObservation("mutation", observation);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onBeforeStatement(SequenceExecutionRecord result, ExecutableSequence executableSequence, int i) {
                super.onBeforeStatement(result, executableSequence, i);
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

    public Mutant getMutant() {
        return mutant;
    }
}
