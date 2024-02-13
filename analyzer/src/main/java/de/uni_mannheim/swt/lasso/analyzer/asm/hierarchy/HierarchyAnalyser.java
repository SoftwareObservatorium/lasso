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
package de.uni_mannheim.swt.lasso.analyzer.asm.hierarchy;

import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.analyzer.model.Method;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Super class and interface hierarchy analysis based on graph structure derived from {@link CompilationUnit}.
 *
 * @author Marcus Kessel
 */
public class HierarchyAnalyser {

    public static final String JAVA_LANG_OBJECT = "java/lang/Object";

    // for quick lookups
    private Map<String, CompilationUnit> lookup = new HashMap<>();

    /**
     * Analyse hierarchy and set to {@link CompilationUnit}.
     *
     * @param classes
     */
    public void analyse(List<CompilationUnit> classes) {
        Graph<String, DefaultEdge> superClassGraph = createSuperClassHierarchy(classes);

        for (CompilationUnit clazz : classes) {
            String ownerName = clazz.getByteCodeName();

            DepthFirstIterator<String, DefaultEdge> depthFirstIterator
                    = new DepthFirstIterator<>(superClassGraph, ownerName);

            Set<String> superClasses = null;
            Set<String> interfaceClasses = CollectionUtils.isNotEmpty(clazz.getInterfaceNames()) ? clazz.getInterfaceNames() : new HashSet<>();

            if (CollectionUtils.isNotEmpty(interfaceClasses)) {
                // resolve any super interfaces
                Set<String> superInterfaceClasses = new HashSet<>();
                for (String interfaceName : interfaceClasses) {
                    Set<String> superInterfaceNames = resolveSuperInterfaces(superClassGraph, interfaceName);
                    if (CollectionUtils.isNotEmpty(superInterfaceNames)) {
                        if (superInterfaceClasses == null) {
                            superInterfaceClasses = new HashSet<>();
                        }

                        superInterfaceClasses.addAll(superInterfaceNames);
                    }
                }

                interfaceClasses.addAll(superInterfaceClasses);
            }

            List<Method> inheritedMethods = new LinkedList<>();
            while (depthFirstIterator.hasNext()) {
                if (superClasses == null) {
                    superClasses = new HashSet<>();
                }
                String superName = depthFirstIterator.next();

                // lookup unit, if available add all interfaces from it
                CompilationUnit superUnit = lookup.get(superName);
                if (superUnit != null && CollectionUtils.isNotEmpty(superUnit.getInterfaceNames())) {
                    // only if not clazz == superUnit
                    if(!StringUtils.equals(superUnit.getByteCodeName(), clazz.getByteCodeName())) {
                        // add interfaces
                        interfaceClasses.addAll(superUnit.getInterfaceNames());

                        // resolve any super interfaces of the superclass' interfaces
                        Set<String> superInterfaceClasses = new HashSet<>();
                        for (String interfaceName : interfaceClasses) {
                            Set<String> superInterfaceNames = resolveSuperInterfaces(superClassGraph, interfaceName);
                            if (CollectionUtils.isNotEmpty(superInterfaceNames)) {
                                if (superInterfaceClasses == null) {
                                    superInterfaceClasses = new HashSet<>();
                                }

                                superInterfaceClasses.addAll(superInterfaceNames);
                            }
                        }

                        interfaceClasses.addAll(superInterfaceClasses);

                        // add methods that can be inherited (either package protected within same package or visible)
                        if (CollectionUtils.isNotEmpty(superUnit.getMethods())) {
                            List<Method> filteredList = superUnit.getMethods().stream().filter(
                                    m -> m.isInheritable(StringUtils.equals(clazz.getPackageName(), superUnit.getPackageName())))
                                    .collect(Collectors.toList());

                            if(CollectionUtils.isNotEmpty(filteredList)) {
                                inheritedMethods.addAll(filteredList);
                            }
                        }
                    }
                }

                superClasses.add(superName);
            }

            // set
            // remove owner class
            if (CollectionUtils.isNotEmpty(superClasses)) {
                superClasses.remove(clazz.getByteCodeName());
                // add if not already there
                superClasses.add(JAVA_LANG_OBJECT);
            } else {
                // add default
                superClasses = new HashSet<>();
                // add if not already there
                superClasses.add(JAVA_LANG_OBJECT);
            }

            clazz.setSuperClassNames(superClasses);

            clazz.setInheritedMethods(inheritedMethods);

            // remove artifical Object
            if (CollectionUtils.isNotEmpty(interfaceClasses)) {
                interfaceClasses.remove(JAVA_LANG_OBJECT);
            }
            clazz.setInterfaceClassNames(interfaceClasses);


        }

        // free
        lookup.clear();
    }

    /**
     * This method assumes interfaces and super interfaces (i.e one extend!).
     *
     * @param superClassGraph
     * @param interfaceName
     * @return
     */
    private Set<String> resolveSuperInterfaces(Graph<String, DefaultEdge> superClassGraph, String interfaceName) {
        CompilationUnit interfaceUnit = lookup.get(interfaceName);

        Set<String> interfaces = null;
        if (interfaceUnit != null) {
            DepthFirstIterator<String, DefaultEdge> depthFirstIterator
                    = new DepthFirstIterator<>(superClassGraph, interfaceName);

            while (depthFirstIterator.hasNext()) {
                // add superclasses of interface
                String superInterfaceName = depthFirstIterator.next();

                if (interfaces == null) {
                    interfaces = new HashSet<>();
                }

                //
                interfaces.add(superInterfaceName);
            }
        }

        return interfaces;
    }

    /**
     * Create super class hierarchy.
     *
     * @param classes
     * @return
     */
    public Graph<String, DefaultEdge> createSuperClassHierarchy(List<CompilationUnit> classes) {
        Graph<String, DefaultEdge> g
                = new DefaultDirectedGraph<>(DefaultEdge.class);

        // add java.lang.Object
        g.addVertex(JAVA_LANG_OBJECT);

        for (CompilationUnit clazz : classes) {
            String ownerName = clazz.getByteCodeName();

            // add to lookup
            lookup.put(ownerName, clazz);

            // add owner
            if (!g.containsVertex(ownerName)) {
                g.addVertex(ownerName);
            }

            String superName = clazz.getSuperClassName();
            if (superName != null) {
                if (!g.containsVertex(superName)) {
                    g.addVertex(superName);
                }

                // add edge (bottom-up hierarchy, from superclass to subclass)
                g.addEdge(ownerName, superName);
            } else {
                // add edge (bottom-up hierarchy, from superclass to subclass)
                g.addEdge(ownerName, JAVA_LANG_OBJECT);
            }
        }

        return g;
    }
}
