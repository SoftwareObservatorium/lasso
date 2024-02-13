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
package de.uni_mannheim.swt.lasso.arena;

import de.uni_mannheim.swt.lasso.arena.event.ArenaExecutionListener;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceExecutionRecord;
import randoop.ExecutionVisitor;
import randoop.sequence.ExecutableSequence;

/**
 * Delegates events to {@link ArenaExecutionListener}.
 *
 * @author Marcus Kessel
 */
public class CutExecutionListener implements ExecutionVisitor {

    private final ArenaExecutionListener delegate;

    private SequenceExecutionRecord current;

    public CutExecutionListener(ArenaExecutionListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void visitBeforeStatement(ExecutableSequence executableSequence, int i) {
        try {
            delegate.onBeforeStatement(current, executableSequence, i);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visitAfterStatement(ExecutableSequence executableSequence, int i) {
        try {
            delegate.onAfterStatement(current, executableSequence, i);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(ExecutableSequence executableSequence) {
        try {
            delegate.onBeforeSequence(current, executableSequence);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void visitAfterSequence(ExecutableSequence executableSequence) {
        try {
            delegate.onAfterSequence(current, executableSequence);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public SequenceExecutionRecord getCurrent() {
        return current;
    }

    public void setCurrent(SequenceExecutionRecord current) {
        this.current = current;
    }
}
