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
package randoop.operation;

import randoop.types.ClassOrInterfaceType;
import randoop.types.Type;
import randoop.types.TypeTuple;

/**
 * Workaround visibility issues in randoop.
 *
 * @author Marcus Kessel
 */
public class RandoopHelper {

    /**
     *
     * @param op
     * @param declaringType
     * @param inputTypes
     * @param outputType
     * @return
     */
    public static TypedClassOperationWithCast createTypedClassOperationWithCast(CallableOperation op, ClassOrInterfaceType declaringType, TypeTuple inputTypes, Type outputType) {
        return new TypedClassOperationWithCast(op, declaringType, inputTypes, outputType);
    }

    public static TypedOperation createCodeTerm(CodeTerm term) {
        return new TypedTermOperation(term, new TypeTuple(), term.getType());
    }
}
