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
package de.uni_mannheim.swt.lasso.arena.task.load;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.SequenceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SheetSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SpreadSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.JUnitSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.engine.matcher.TestMatcher;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lookup local projects on the file system.
 *
 * @author Marcus Kessel
 */
public class FileSystemSheetProvider implements SheetProvider {

    private static final Logger LOG = LoggerFactory
            .getLogger(FileSystemSheetProvider.class);

    public static final Pattern UUID_REGEX = Pattern.compile("\\p{XDigit}{8}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{4}-\\p{XDigit}{12}");

    public static final String TEST_CLASS_PATTERN = "/**/src/test/java/**/*";

    private int threads = Runtime.getRuntime().availableProcessors() - 2;

    protected final File path;

    protected final CodeSearch codeSearch;
    protected final CandidatePool pool;

    private List<ResolvedSheets> resolvedSheets;

    private boolean method = false;

    private InterfaceSpecification interfaceSpecification;

    public FileSystemSheetProvider(File path, CodeSearch codeSearch, CandidatePool pool) {
        this.path = path;
        this.codeSearch = codeSearch;
        this.pool = pool;
    }

    @Override
    public synchronized List<ResolvedSheets> resolve() throws IOException {
        if(CollectionUtils.isNotEmpty(resolvedSheets)) {
            return resolvedSheets;
        }

        List<SheetMatch> sheets = findSheets();

        // resolve CUTs
        resolveCuts(sheets);

        List<ResolvedSheets> resolvedSheetList = Collections.synchronizedList(new LinkedList<>());

        // run parallel
        ForkJoinPool nExecutor = new ForkJoinPool(getThreads(), pool -> {
            final ForkJoinWorkerThread worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            worker.setName("sheet-provider-" + worker.getPoolIndex());
            return worker;
        }, null, false);

        try {
            nExecutor.submit(
                    () -> sheets.parallelStream().forEach(s -> {
                        try {
                            ResolvedSheets resolvedSheets = resolve(s);
                            resolvedSheetList.add(resolvedSheets);
                        } catch (Throwable e) {
                            LOG.warn("Sheet failed " + s.getFile(), e);
                        }
                    })).get();
        } catch (Throwable e) {
            LOG.warn("executor service failed", e);
        } finally {
            nExecutor.shutdown();
        }

        this.resolvedSheets = resolvedSheetList;

        return resolvedSheetList;
    }

    @Override
    public void resolveCuts() throws IOException {
        List<SheetMatch> sheets = findSheets();

        // resolve CUTs
        resolveCuts(sheets);
    }

    protected ClassUnderTest retrieve(String implementation) throws IOException {
        return codeSearch.queryForClass(implementation);
    }

    protected void resolveCuts(List<SheetMatch> sheets) {
        Set<String> impls = sheets.stream().map(SheetMatch::getImplementation).collect(Collectors.toSet());

        resolveCuts(impls);
    }

    public void resolveCuts(Set<String> impls) {
        for(String impl : impls) {
            try {
                Optional<ClassUnderTest> classUnderTestOptional = pool.getClassUnderTest(impl);
                if(!classUnderTestOptional.isPresent()) {
                    ClassUnderTest classUnderTest = retrieve(impl);

                    pool.addClass(classUnderTest);
                }
            } catch (Throwable e) {
                LOG.warn("Failed to resolve CUT", e);
            }
        }

        // re-init projects
        pool.initProjects();
    }

    protected ResolvedSheets resolve(SheetMatch sheetMatch) throws IOException {
        LOG.debug("RESOLVED SHEET: " + sheetMatch.getImplementation() + " => " + sheetMatch.getFile());

        Optional<ClassUnderTest> classUnderTestOptional = pool.getClassUnderTest(sheetMatch.getImplementation());
        ClassUnderTest classUnderTest = classUnderTestOptional
                .orElseThrow(() -> new IOException("Could not find CUT " + sheetMatch.getImplementation()));

        // extract CUT and its specification
        if(sheetMatch.isJava()) {
            JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();

            InterfaceSpecification specification = getInterfaceSpecification();
            if(getInterfaceSpecification() == null) {
                LOG.debug("Inferring interface specification for '{}'", sheetMatch.getImplementation());

                Map<String, InterfaceSpecification> specificationMap = importJUnitClass.toSpecification(
                        FileUtils.readFileToString(sheetMatch.getFile(), StandardCharsets.UTF_8),
                        classUnderTest);

                if(!specificationMap.containsKey(classUnderTest.getClassName())) {
                    throw new IllegalArgumentException();
                }

                specification = specificationMap.get(classUnderTest.getClassName());
            }

            LOG.debug("CUT " + classUnderTest.getClassName());
            LOG.debug("SPEC LQL " + specification.toLQL());

            //
            ResolvedSheets resolvedSheets = new ResolvedSheets();
            resolvedSheets.setClassUnderTest(classUnderTest);
            resolvedSheets.setSpecification(specification);

            // now parse sheets
            Map<String, SequenceSpecification> executableSheets = importJUnitClass.toSequenceSpecifications(
                    FileUtils.readFileToString(sheetMatch.getFile(), StandardCharsets.UTF_8),
                    specification,
                    classUnderTest,
                    classUnderTest.getClassName(),
                    // FIXME use FilenameUtils.getBaseName(sheetMatch.getFile().getName()) instead
                    sheetMatch.getFile().getName() /*prefix avoids name clashes*/,
                    classUnderTest.getId() /*postfix*/);

            resolvedSheets.setSheets(executableSheets);

            return resolvedSheets;
        } else if(sheetMatch.isSheet()) {
            SheetSequenceSpecificationParser sheetSequenceSpecificationParser = new SheetSequenceSpecificationParser();

            InterfaceSpecification specification = getInterfaceSpecification();
            if(specification == null) {
                throw new UnsupportedOperationException("specification must be set for sheets");
            }

            LOG.debug("CUT " + classUnderTest.getClassName());
            LOG.debug("SPEC LQL " + specification.toLQL());

            //
            ResolvedSheets resolvedSheets = new ResolvedSheets();
            resolvedSheets.setClassUnderTest(classUnderTest);
            resolvedSheets.setSpecification(specification);

            // now parse sheets
            Map<String, SequenceSpecification> executableSheets = sheetSequenceSpecificationParser.toSequenceSpecifications(
                    new SpreadSheet(sheetMatch.getFile()),
                    specification,
                    classUnderTest,
                    classUnderTest.getId() /*postfix*/);

            resolvedSheets.setSheets(executableSheets);

            return resolvedSheets;
        } else {
            throw new UnsupportedOperationException("sheet match type unsupported");
        }
    }

    @Override
    public List<String> getImplementations() {
        return new ArrayList<>(findSheets().stream().map(SheetMatch::getImplementation).collect(Collectors.toSet()));
    }

    @Override
    public CandidatePool getPool() {
        return pool;
    }

    public List<SheetMatch> findSheets() {
        List<SheetMatch> sheetMatches = findSheetSpecifications();
        List<SheetMatch> unitMatches = findUnitTests();

        List<SheetMatch> matches = new LinkedList<>();
        if(CollectionUtils.isNotEmpty(sheetMatches)) {
            matches.addAll(sheetMatches);
        }

        if(CollectionUtils.isNotEmpty(unitMatches)) {
            matches.addAll(unitMatches);
        }

        return matches;
    }

    public List<SheetMatch> findSheetSpecifications() {
        TestMatcher matcher = new TestMatcher();
        matcher.setExtension("xlsx"); // serialized sheets

        List<File> files = matcher.findMatches(TEST_CLASS_PATTERN, path);

        if(CollectionUtils.isEmpty(files)) {
            return Collections.emptyList();
        }

        return files.stream()
                .map(f -> {
                    String abs = f.getAbsolutePath();

                    // identify implementation id
                    Matcher reg = UUID_REGEX.matcher(abs);
                    String last = null;
                    while (reg.find()) {
                        last = reg.group(0);
                    }

                    SheetMatch sheetMatch = new SheetMatch(last, f);

                    return sheetMatch;
                }).collect(Collectors.toList());
    }

    /**
     * Find java files
     *
     * @return
     */
    public List<SheetMatch> findUnitTests() {
        TestMatcher matcher = new TestMatcher();

        List<File> files = matcher.findMatches(TEST_CLASS_PATTERN, path);

        if(CollectionUtils.isEmpty(files)) {
            return Collections.emptyList();
        }

        return files.stream()
                .map(f -> {
                    String abs = f.getAbsolutePath();

                    // identify implementation id
                    Matcher reg = UUID_REGEX.matcher(abs);
                    String last = null;
                    while (reg.find()) {
                        last = reg.group(0);
                    }

                    SheetMatch sheetMatch = new SheetMatch(last, f);

                    return sheetMatch;
                }).collect(Collectors.toList());
    }

    /**
     * Find all CUTs
     *
     * @return
     */
    public Set<String> findCuts() {
        FileFilter filter = new RegexFileFilter(UUID_REGEX.pattern());

        List<File> files = Arrays.stream(path.listFiles(filter)).collect(Collectors.toList());

        return files.stream()
                .map(f -> {
                    String abs = f.getAbsolutePath();

                    // identify implementation id
                    Matcher reg = UUID_REGEX.matcher(abs);
                    String last = null;
                    while (reg.find()) {
                        last = reg.group(0);
                    }

                    return last;
                }).collect(Collectors.toSet());
    }

    public boolean isMethod() {
        return method;
    }

    public void setMethod(boolean method) {
        this.method = method;
    }

    public InterfaceSpecification getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(InterfaceSpecification interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public int getThreads() {
        return threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }
}
