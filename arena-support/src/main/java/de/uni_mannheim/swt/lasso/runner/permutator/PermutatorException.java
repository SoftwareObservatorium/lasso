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
package de.uni_mannheim.swt.lasso.runner.permutator;

/**
 * Exception thrown in {@link Permutator}.
 * 
 * @author Marcus Kessel
 *
 */
public class PermutatorException extends RuntimeException {

    public PermutatorException() {
        super();
    }

    public PermutatorException(String arg0, Throwable arg1, boolean arg2,
            boolean arg3) {
        super(arg0, arg1, arg2, arg3);
    }

    public PermutatorException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }

    public PermutatorException(String arg0) {
        super(arg0);
    }

    public PermutatorException(Throwable arg0) {
        super(arg0);
    }

}
