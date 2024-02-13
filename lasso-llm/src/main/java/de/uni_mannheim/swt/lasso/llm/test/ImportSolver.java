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
package de.uni_mannheim.swt.lasso.llm.test;

import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionFactory;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ImportSolver extends ClassLoaderTypeSolver {

    private static final Logger LOG = LoggerFactory
            .getLogger(ImportSolver.class);

    private List<String> imports;

    private final ClassLoader classLoader;

    public ImportSolver(Container container, List<String> imports) {
        super(container);
        this.classLoader = container;
        this.imports = imports;
    }

    @Override
    public SymbolReference<ResolvedReferenceTypeDeclaration> tryToSolveType(String name) {
        LOG.debug("Trying to solve '{}'", name);

        // do not resolve any qualified ones
        if(!StringUtils.contains(name, ".")) {
            for(String pkg : imports) {
                LOG.debug("Trying to solve with pkg '{}' . '{}'", pkg, name);

                try {
                    Class<?> clazz = classLoader.loadClass(pkg + "." + name);
                    return SymbolReference.solved(ReflectionFactory.typeDeclarationFor(clazz, getRoot()));
                } catch (NoClassDefFoundError | ClassNotFoundException e) {
                    //
                }
            }
        }

        return SymbolReference.unsolved(ResolvedReferenceTypeDeclaration.class);
    }
}
