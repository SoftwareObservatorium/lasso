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
package de.uni_mannheim.swt.lasso.engine.action.clonedectection;

import de.uni_mannheim.swt.lasso.cluster.data.repository.ExecKey;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.LassoUtils;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Local;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.data.ReportKey;
import de.uni_mannheim.swt.lasso.engine.data.ReportOperations;
import de.uni_mannheim.swt.lasso.engine.environment.BasicExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.DockerExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.Cache;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * Nicad 6 action for code clone detection.
 *
 * @author Marcus Kessel
 *
 * <a href="https://www.txl.ca/txl-nicaddownload.html">Nicad 5 Website</a>
 */
@LassoAction(desc = "Nicad 6 Code Clone Detection")
@Stable
@Local
public class Nicad6 extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(Nicad6.class);

    protected static final String CLASS_SKELETON = "public class %s {\n%s\n}";

    @Deprecated
    protected static final String METHOD_SKELETON = "void callgraph() {\n%s\n}";

    protected static final String NICAD_REPORT_ROOT = "modules_files-*";
    protected static final String NICAD_REPORT = "modules_files-*clones-*-classes.xml";

    public static final String NICAD_6_2 = "nicad:6.2";

    @LassoInput(desc = "Clone Type", optional = true)
    public String cloneType = "type2";

    @LassoInput(desc = "Normalize Visibility Modifiers", optional = true)
    public boolean normalizeVisibility = true;

    @LassoInput(desc = "Collapse detected Clones", optional = true)
    public boolean collapseClones = true;

    @LassoInput(desc = "Include reference implementation from the following action", optional = true)
    public String refActionRef;

    @LassoInput(desc = "Do clone detection on Call Graph comparisons", optional = true)
    public boolean callGraph = false;

    protected File getReportXml(File root) throws IOException {
        // determine directory
        FileFilter dirFilter = new WildcardFileFilter(NICAD_REPORT_ROOT);
        File[] dirs = root.listFiles(dirFilter);

        if(ArrayUtils.isEmpty(dirs)) {
            throw new IOException("Did not find nicad report directory");
        }

        File cloneRoot = Arrays.stream(dirs).filter(File::isDirectory).findFirst().get();
        if(!cloneRoot.isDirectory()) {
            throw new IOException("Directory identified is not a directory " + cloneRoot.getAbsolutePath());
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Found directory " + cloneRoot.getAbsolutePath());
        }

        FileFilter fileFilter = new WildcardFileFilter(NICAD_REPORT);
        File[] files = cloneRoot.listFiles(fileFilter);

        if(ArrayUtils.isEmpty(files)) {
            throw new IOException("Did not find report file");
        }

        if(LOG.isDebugEnabled()) {
            LOG.debug("Found XML file in '{}'", files[0]);
        }

        return files[0];
    }

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if(LOG.isInfoEnabled()) {
            LOG.info("Executing "+  this.getClass());
        }

        //
        File projectRoot = context.getWorkspace().getRoot(getInstanceId(), actionConfiguration.getAbstraction());

        // required by nicad
        File submoduleRoot = new File(projectRoot, "modules");

        // create project folders
        List<System> impls = actionConfiguration.getAbstraction().getImplementations();
        impls.forEach(implementation -> {
//            if(callGraph) {
//                // attempt to retrieve last report
//                ReportKey key = ReportKey.of((String) null, actionConfiguration.getAbstraction().getName(), implementation);
//
//                StaticCallGraphReport staticCallGraphReport = context.getReportOperations().getLast(context.getExecutionId(), key, StaticCallGraphReport.class);
//                createProject(submoduleRoot, implementation, staticCallGraphReport);
//            } else {
//                createProject(submoduleRoot, implementation);
//            }
            createProject(submoduleRoot, implementation.getCode());
        });

        // include reference implementation ?
        if(StringUtils.isNotBlank(refActionRef)) {
            // find impl == abstraction
            String refImplId = LassoUtils.getReferenceImplementationFromAlternatives(actionConfiguration.getAbstraction());

            if(LOG.isInfoEnabled()) {
                LOG.info("Adding reference implementation '{}' to the list of code clone modules", refImplId);
            }

            Cache.Entry<ExecKey, System> executableEntry = context.getLassoOperations().getExecutableFromAction(context.getExecutionId(), refActionRef, refImplId);

            // add to submodules
            CodeUnit referenceImplementation = executableEntry.getValue().getCode();
            createProject(submoduleRoot, referenceImplementation);
        }

        // args passed to nicad
        List<String> args = new ArrayList<>(Arrays.asList("nicad", "files", "java", "/src/" + submoduleRoot.getName() + "/", cloneType));

        // set default commands
        Environment environment;
        if(actionConfiguration.getProfile() != null) {
            environment = actionConfiguration.getProfile().getEnvironment();
        } else {
            environment = new Environment();
            environment.setImage(NICAD_6_2);
        }

        if(CollectionUtils.isEmpty(environment.getCommandArgsList())) {
            environment.setCommandArgsList(new LinkedList<>());
        }

        environment.getCommandArgsList().add(args);

        //
        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();

        BasicExecutionEnvironment executionEnvironment =
                (BasicExecutionEnvironment) executionEnvironmentManager.createExecutionEnvironment(ExecutionEnvironmentManager.BASIC);

        // configure environment
        executionEnvironment.setImage(environment.getImage());
        executionEnvironment.setWorkingDirectory(new File("/src/")); // docker working directory

        // docker in docker?
        File dockerProjectRoot = projectRoot;
        if (DockerExecutionEnvironment.isRunningDockerInDocker()) {
            // where project lives
            dockerProjectRoot = DockerExecutionEnvironment.rewritePathDind(context.getWorkspace(), dockerProjectRoot);
        }

        executionEnvironment.setProjectRoot(context.getWorkspace(), dockerProjectRoot);
        executionEnvironment.setCommands(environment.getCommandArgsList().get(0));

        // run
        if(LOG.isInfoEnabled()) {
            LOG.info("Running container");
        }

        executionEnvironmentManager.run(executionEnvironment);

        if(LOG.isInfoEnabled()) {
            LOG.info("Finished container");
        }

        // collect data (one report for all !)
        Map<Integer, NicadParser.CloneClass> cloneMap = collectData(projectRoot);

        // group/collapse by detected clones
        group(cloneMap, context, actionConfiguration);
    }

//    protected void createProject(File submoduleRoot, CodeUnit implementation, StaticCallGraphReport staticCallGraphReport) {
//        if(staticCallGraphReport == null || CollectionUtils.isEmpty(staticCallGraphReport.getCalls())) {
//            if(LOG.isWarnEnabled()) {
//                LOG.warn("No calls found for '{}'", implementation.getId());
//            }
//
//            return;
//        }
//
//        try {
//            String source = "";
//            if(implementation.getImplementationUnit() == CodeUnit.CodeUnitType.METHOD) {
//                List<String> calls = staticCallGraphReport.getCalls();
//
//                String callChain = calls.stream()
//                        .map(s -> StringUtils.substringBeforeLast(s, ":"))
//                        .map(s -> StringUtils.replace(s, "::", ".") + ";")
//                        .map(s -> StringUtils.replaceEach(s, new String[]{".<init>", ".<clinit>"}, new String[]{"", ""}))
//                        .collect(Collectors.joining("\n"));
//
//                String methodSource = String.format(METHOD_SKELETON, callChain);
//
//                source = String.format(CLASS_SKELETON, implementation.getName(), methodSource);
//            } else {
//                throw new UnsupportedOperationException("not implemented");
//            }
//
//            // create project
//            MavenProject mavenProject = new MavenProject(new File(submoduleRoot, implementation.getId()), false);
//            mavenProject.getSrcMain().mkdirs();
//
//            CompilationUnit cu = new CompilationUnit();
//            cu.setName(implementation.getName());
//            cu.setPkg(implementation.getPackagename());
//            cu.setSourceCode(source);
//
//            mavenProject.writeCompilationUnit(cu, false);
//        } catch(Throwable e) {
//            if(LOG.isWarnEnabled()) {
//                LOG.warn("Implementation '{}' failed", implementation.getId());
//            }
//        }
//    }

    protected void createProject(File submoduleRoot, CodeUnit implementation) {
        try {
            String source = "";
            if(implementation.getUnitType() == CodeUnit.CodeUnitType.METHOD) {

                source = String.format(CLASS_SKELETON, implementation.getName(), implementation.getContent());
            } else {
                source = implementation.getContent();
            }

            if(StringUtils.isBlank(source)) {
                // ignore
                if(LOG.isWarnEnabled()) {
                    LOG.warn("Did not find source code for implementation '{}'", implementation.getId());
                }

                return;
            }

            //
            if(normalizeVisibility) {
                source = StringUtils.replaceEach(source, new String[] {"private ", "protected ", "public "}, new String[]{"", "", ""});

                if(LOG.isDebugEnabled()) {
                    LOG.debug("Normalized code snippet to: \n{}", source);
                }
            }

            // create project
            MavenProject mavenProject = new MavenProject(new File(submoduleRoot, implementation.getId()), false);
            mavenProject.getSrcMain().mkdirs();

            CompilationUnit cu = new CompilationUnit();
            cu.setName(implementation.getName());
            cu.setPkg(implementation.getPackagename());
            cu.setSourceCode(source);

            mavenProject.writeCompilationUnit(cu, false);
        } catch(Throwable e) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Implementation '{}' failed", implementation.getId());
            }
        }
    }

    protected Map<Integer, NicadParser.CloneClass> collectData(File projectRoot) throws IOException {
        // read report
        File xmlFile = getReportXml(projectRoot);

        if(!xmlFile.exists()) {
            if(LOG.isWarnEnabled()) {
                LOG.warn("Did not find nicad report => '{}'", xmlFile.getAbsolutePath());
            }

            return null;
        }

        try {
            Map<Integer, NicadParser.CloneClass> cloneMap = new NicadParser(
                    FileUtils.openInputStream(xmlFile))
                    .parse();

            for(NicadParser.CloneClass cloneClass : cloneMap.values()) {

                LOG.debug(cloneClass.getClassid() + " => " + cloneClass.getImplementations());
            }

            return cloneMap;
        } catch (Throwable e) {
            //
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not parse nicad report '%s'", xmlFile.getAbsolutePath()), e);
            }
        }
        return null;
    }

    public void group(Map<Integer, NicadParser.CloneClass> cloneMap, LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        List<Integer> remove = new LinkedList<>();

        // simply drop all implementations of the same cluster id (when ref)
        if(StringUtils.isNotBlank(refActionRef)) {
            // find impl == abstraction
            String refImplId = LassoUtils.getReferenceImplementationFromAlternatives(actionConfiguration.getAbstraction());
            Optional<NicadParser.CloneClass> refOp = cloneMap.values().stream().filter(clazz -> clazz.getImplementations().contains(refImplId)).findFirst();
            refOp.ifPresent(s -> remove.add(s.getClassid()));
        }

        //
        Map<Integer, CodeUnit> cloneCollapseMap = new HashMap<>();
        actionConfiguration.getAbstraction().getImplementations().removeIf(impl -> {
            // collapse by clone
            if(collapseClones) {
                Optional<NicadParser.CloneClass> cloneClass = cloneMap.values().stream().filter(cc -> cc.getImplementations().contains(impl.getId())).findFirst();

                if(cloneClass.isPresent()) {
                    int cloneId = cloneClass.get().getClassid();

                    if(remove.contains(cloneId)) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Removing clone of reference implementation {}", impl.getId());
                        }

                        // remove
                        return true;
                    } else if(cloneCollapseMap.containsKey(cloneId)) {
                        CodeUnit implementation = cloneCollapseMap.get(cloneId);
                        // add as child
                        if(implementation.getClonesDetected() == null) {
                            implementation.setClonesDetected(new LinkedList<>());
                        }
                        implementation.getClonesDetected().add(impl.getCode());

                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Removing clone of alternative implementation {}", impl.getId());
                        }

                        // remove
                        return true;
                    } else {
                        cloneCollapseMap.put(cloneId, impl.getCode());
                    }
                } else {
                    // nothing
                }
            }

            if(LOG.isDebugEnabled()) {
                LOG.debug("Retaining {}", impl.getId());
            }

            // do not remove
            return false;
        });

        //
        ReportOperations recordOperations = context.getReportOperations();

        // write report
        Set<Integer> classes = new HashSet<>();
        classes.addAll(cloneMap.keySet());

        actionConfiguration.getAbstraction().getImplementations().stream().forEach(impl -> {
            Nicad5Report report = new Nicad5Report();

            Optional<NicadParser.CloneClass> cloneClass = cloneMap.values().stream().filter(cc -> cc.getImplementations().contains(impl.getId())).findFirst();

            if(cloneClass.isPresent()) {
                NicadParser.CloneClass cc = cloneClass.get();
                report.setClassid(cc.getClassid());
                report.setNclones(cc.getNclones());
                report.setNlines(cc.getNlines());
                report.setSimilarity(cc.getSimilarity());
            } else {
                int classId = generateClassId(classes);
                classes.add(classId);

                report.setClassid(classId);
                report.setNclones(1);
                report.setNlines(-1);
                report.setSimilarity(100);
            }

            recordOperations.put(context.getExecutionId(), ReportKey.of(this, actionConfiguration.getAbstraction(), impl), report);
        });

        // set setExecutables() to be compliant with other actions
        Systems executables = new Systems();
        executables.setExecutables(actionConfiguration.getAbstraction().getImplementations());
        executables.setAbstractionName(actionConfiguration.getAbstraction().getName());
        executables.setActionInstanceId(getInstanceId());
        setExecutables(executables);
    }

    protected int generateClassId(Set<Integer> numbers) {
        int max = CollectionUtils.isEmpty(numbers) ? 0: Collections.max(numbers);

        return max + 1;
    }
}
