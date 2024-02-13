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
package de.uni_mannheim.swt.lasso.arena.adaptation.filter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * An implementation unit filter (class, method).
 *
 * @author Marcus Kessel
 */
public class ImplementationUnitFilter implements SpecFilter {

    private static final org.slf4j.Logger LOG = LoggerFactory
            .getLogger(ImplementationUnitFilter.class);

    @Override
    public boolean accept(ClassUnderTest classUnderTest, Method method) {
        try {
            if(classUnderTest.getImplementation().getCode().getUnitType() == CodeUnit.CodeUnitType.METHOD) {
                // extract single method
                String bcMethodName = StringUtils.substringAfterLast(classUnderTest.getImplementation().getCode().getBytecodeName(), ".");
                String desc = org.objectweb.asm.Type.getMethodDescriptor(method);

                boolean same = StringUtils.equals(String.format("%s%s", method.getName(), desc), bcMethodName);

                if(same) {
                    if(LOG.isInfoEnabled()) {
                        LOG.info("accepting method '{}' vs '{}'", method.toString(), bcMethodName);
                    }
                }

                return same;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return false;
    }
}
