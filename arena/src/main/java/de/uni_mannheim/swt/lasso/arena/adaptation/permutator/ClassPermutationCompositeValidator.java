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
package de.uni_mannheim.swt.lasso.arena.adaptation.permutator;

import de.uni_mannheim.swt.lasso.runner.permutator.validate.PermutationValidator;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.List;

/**
 * Validator that supports the composition of {@link PermutationValidator}s.
 *
 * @author Marcus Kessel
 *
 */
public class ClassPermutationCompositeValidator implements ClassPermutationValidator {

    private final List<ClassPermutationValidator> validators;

    /**
     * @param validators
     *            One or more {@link PermutationValidator}s
     */
    public ClassPermutationCompositeValidator(ClassPermutationValidator... validators) {
        Validate.notEmpty(validators, "Validator array cannot be empty");
        this.validators = Arrays.asList(validators);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(ClassPermutation permutation) {
        return validators.stream().map(v -> v.isValid(permutation)).allMatch(v -> v);
    }
}
