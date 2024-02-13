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
package de.uni_mannheim.swt.lasso.arena.event;

import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecords;
import randoop.sequence.ExecutableSequence;

/**
 * Listener for candidate executions.
 *
 * @author Marcus Kessel
 */
public interface ArenaExecutionListener {

    /**
     * Called BEFORE the execution of a suite/set of sequences.
     *
     * @param results
     */
    void onBeforeExecution(SequenceExecutionRecords results);
    /**
     * Called AFTER the execution of a suite/set of sequences.
     *
     * @param results
     */
    void onAfterExecution(SequenceExecutionRecords results);

    /**
     * Called BEFORE the execution of a single sequence.
     *
     * @param result
     * @param executableSequence
     */
    void onBeforeSequence(SequenceExecutionRecord result, ExecutableSequence executableSequence);
    /**
     * Called AFTER the execution of a single sequence.
     *
     * @param result
     * @param executableSequence
     */
    void onAfterSequence(SequenceExecutionRecord result, ExecutableSequence executableSequence);

    /**
     * Called BEFORE the execution of a single statement of a sequence.
     *
     * @param result
     * @param executableSequence
     * @param i
     */
    void onBeforeStatement(SequenceExecutionRecord result, ExecutableSequence executableSequence, int i);

    /**
     * Called AFTER the execution of a single statement of a sequence.
     *
     * @param result
     * @param executableSequence
     * @param i
     */
    void onAfterStatement(SequenceExecutionRecord result, ExecutableSequence executableSequence, int i);
}
