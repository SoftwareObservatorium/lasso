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
package de.uni_mannheim.swt.lasso.arena.adaptation;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

/**
 *
 * @author Marcus Kessel
 */
public class OriginalImplementation extends AdaptedImplementation {

    public OriginalImplementation(ClassUnderTest adaptee) {
        super(adaptee);
    }

    @Override
    public AdaptedMethod getMethod(InterfaceSpecification specification, int m) {
        return null;
    }

    @Override
    public int getNoOfMethods() {
        return 0;
    }

    @Override
    public AdaptedInitializer getInitializer(InterfaceSpecification specification, int c) {
        return null;
    }

    @Override
    public AdaptedInitializer getDefaultInitializer() {
        return null;
    }

    @Override
    public String getAdapterId() {
        return "0";
    }
}