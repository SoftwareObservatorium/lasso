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
package de.uni_mannheim.swt.lasso.runner.permutator.validate;

import de.uni_mannheim.swt.lasso.runner.permutator.Permutation;

/**
 * Validator interface to check for valid {@link Permutation}s.
 * 
 * @author Marcus Kessel
 *
 */
public interface PermutationValidator {

    /**
     * @param permutation
     *            {@link Permutation} to check
     * @return true if perm is valid, otherwise false
     */
    public boolean isValid(Permutation permutation);
}
