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
package de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco;

import org.apache.commons.lang3.StringUtils;
import org.jacoco.core.internal.analysis.filter.IFilter;
import org.jacoco.core.internal.analysis.filter.IFilterContext;
import org.jacoco.core.internal.analysis.filter.IFilterOutput;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Filter methods by name.
 *
 * @author Marcus Kessel
 */
public class MethodFilter implements IFilter {

    private static final Logger LOG = LoggerFactory
            .getLogger(MethodFilter.class);

    private final List<String> methods;
    private final boolean allowed;

    public MethodFilter(List<String> methods, boolean allowed) {
        this.methods = methods;
        this.allowed = allowed;
    }

    @Override
    public void filter(MethodNode methodNode, IFilterContext iFilterContext, IFilterOutput iFilterOutput) {
        if(allowed) { // white list
            if(!StringUtils.equalsAnyIgnoreCase(methodNode.name, methods.toArray(new String[0]))) {
                iFilterOutput.ignore(methodNode.instructions.getFirst(), methodNode.instructions.getLast());

                return;
            }
        } else {
            // not allowed (black list)
            if(StringUtils.equalsAnyIgnoreCase(methodNode.name, methods.toArray(new String[0]))) {
                //LOG.info("Filtering JaCoCo method '{}'", methodNode.name);

                iFilterOutput.ignore(methodNode.instructions.getFirst(), methodNode.instructions.getLast());
            }
        }
    }
}
