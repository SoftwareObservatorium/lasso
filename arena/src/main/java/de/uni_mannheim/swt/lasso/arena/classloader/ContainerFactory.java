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
package de.uni_mannheim.swt.lasso.arena.classloader;

import de.uni_mannheim.swt.lasso.arena.classloader.coverage.jacoco.JaCoCoContainer;
import de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest.PitestContainer;

import de.uni_mannheim.swt.lasso.arena.classloader.graph.DCGContainer;
import de.uni_mannheim.swt.lasso.core.model.Scope;
import org.pitest.mutationtest.engine.Mutant;

/**
 * A factory for (ClassLoader) containers.
 *
 * @author Marcus Kessel
 */
public abstract class ContainerFactory {

    public static ContainerFactory DEFAULT_FACTORY = new ContainerFactory() {

        @Override
        public Container create(Containers containers, String id, ClassLoader baseClassLoader) {
            return new DefaultContainer(containers, id, baseClassLoader);
        }
    };

    public static ContainerFactory JACOCO_FACTORY = new ContainerFactory() {

        @Override
        public Container create(Containers containers, String id, ClassLoader baseClassLoader) {
            return new JaCoCoContainer(containers, id, baseClassLoader);
        }
    };

    public static ContainerFactory jacoco(Scope scope) {
        return new ContainerFactory() {

            @Override
            public Container create(Containers containers, String id, ClassLoader baseClassLoader) {
                return new JaCoCoContainer(containers, id, baseClassLoader, scope);
            }
        };
    }

    public static ContainerFactory DCG_FACTORY = new ContainerFactory() {

        @Override
        public Container create(Containers containers, String id, ClassLoader baseClassLoader) {
            return new DCGContainer(containers, id, baseClassLoader);
        }
    };

    public static class PitestContainerFactory extends ContainerFactory {

        private final Mutant mutant;

        public PitestContainerFactory(Mutant mutant) {
            this.mutant = mutant;
        }

        @Override
        public Container create(Containers containers, String id, ClassLoader baseClassLoader) {
            return new PitestContainer(containers, id, baseClassLoader, mutant);
        }
    }

    /**
     *
     *
     * @param containers
     * @param id
     * @param baseClassLoader
     * @return
     */
    public abstract Container create(Containers containers, String id, ClassLoader baseClassLoader);
}
