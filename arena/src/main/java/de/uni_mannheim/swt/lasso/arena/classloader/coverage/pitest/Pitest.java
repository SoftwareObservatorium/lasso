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
package de.uni_mannheim.swt.lasso.arena.classloader.coverage.pitest;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.classloader.ContainerFactory;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;

import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.gregor.GregorMutater;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.config.Mutator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Predicate;

/**
 * Integration of Pit's instrumentation facility.
 *
 * @author Marcus Kessel
 */
public class Pitest {

    private final GregorMutater engine;
    private final ClassUnderTest classUnderTest;

    private Map<String, Mutant> mutants = new LinkedHashMap<>();

    public Pitest(ClassUnderTest classUnderTest) {
        this(classUnderTest, i -> true, Mutator.newDefaults());
    }

    public Pitest(ClassUnderTest classUnderTest, Predicate<MethodInfo> methodFilter, Collection<MethodMutatorFactory> operators) {
        this.classUnderTest = classUnderTest;
        this.engine = new GregorMutater(
                new ClassloaderByteArraySource(classUnderTest.getProject().getContainer()),
                methodFilter,
                operators);
    }

    public List<MutationDetails> findMutations() {
        ClassName className = ClassName.fromString(classUnderTest.getClassName());

        return this.engine.findMutations(className);
    }

    public Mutant createMutant(MutationDetails details) {
        return this.engine.getMutation(details.getId());
    }

    /**
     * Generates mutant based on underlying {@link ClassUnderTest}.
     * <p>
     * Circumvents class reloading issues in Java.
     * Creates a copy of {@link ClassUnderTest} which uses a different {@link ClassLoader}.
     *
     * @param id
     * @param details
     * @param resolver
     * @return
     */
    public ClassUnderTest generateMutant(String id, MutationDetails details, DependencyResolver resolver) {
        Mutant mutant = createMutant(details);

        ClassUnderTest mutee = new ClassUnderTest(classUnderTest.getImplementation());
        mutee.setVariantId(String.format("mutant%s", id));

        // set dependency result to speed up things (do not re-fetch)
        mutee.getProject().setDependencyResult(classUnderTest.getProject().getDependencyResult());

        //
        MavenRepository mavenRepository = new MavenRepository(resolver);

        mavenRepository.resolve(mutee, new ContainerFactory.PitestContainerFactory(mutant));

        mutants.put(mutee.getVariantId(), mutant);

        return mutee;
    }

    /**
     * Generate simple CSV report.
     *
     * @param suffix
     * @throws IOException
     */
    public void generateReport(String suffix) throws IOException {
        MavenProject mavenProject = classUnderTest.getLocalProject();
        File csv = new File(mavenProject.getTarget(), String.format("mutants_%s_%s.csv", suffix, System.currentTimeMillis()));

        try (
                BufferedWriter writer = Files.newBufferedWriter(csv.toPath());
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                        .withHeader("Variant ID", "ID", "Mutator", "Block", "Line number", "Line number BC", "Description", "Class", "Method"));
        ) {
            for (Map.Entry<String, Mutant> mutant : mutants.entrySet()) {
                MutationDetails details = mutant.getValue().getDetails();

                csvPrinter.printRecord(mutant.getKey(),
                        details.getId().toString(),
                        details.getMutator(),
                        details.getBlock(),
                        details.getClassLine().getLineNumber(),
                        details.getLineNumber(),
                        details.getDescription(),
                        details.getClassName().asJavaName(),
                        details.getId().getLocation().getMethodName() + details.getId().getLocation().getMethodDesc());
            }

            csvPrinter.flush();
        }
    }
}
