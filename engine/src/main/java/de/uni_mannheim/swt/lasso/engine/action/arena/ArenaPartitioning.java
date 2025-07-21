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
package de.uni_mannheim.swt.lasso.engine.action.arena;

import de.uni_mannheim.swt.lasso.benchmark.Benchmark;
import de.uni_mannheim.swt.lasso.benchmark.FunctionalAbstraction;
import de.uni_mannheim.swt.lasso.benchmark.Test2XSLX;
import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.cluster.client.ClusterArenaJobRepository;
import de.uni_mannheim.swt.lasso.cluster.client.JobStatus;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.model.System;
import de.uni_mannheim.swt.lasso.core.model.*;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.LassoInput;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Stable;
import de.uni_mannheim.swt.lasso.engine.action.annotations.Tester;
import de.uni_mannheim.swt.lasso.engine.action.utils.SequenceUtils;
import de.uni_mannheim.swt.lasso.engine.adaptation.SystemAdapterReport;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironmentManager;
import de.uni_mannheim.swt.lasso.engine.langsupport.LangSupport;
import de.uni_mannheim.swt.lasso.srm.ClusterSRMRepository;
import de.uni_mannheim.swt.lasso.srm.olap.Warehouse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Arena action which supports splitting of implementations.
 *
 * @author Marcus Kessel
 */
@LassoAction(desc = "ArenaSSN action")
@Stable
@Tester
public class ArenaPartitioning extends DefaultAction {

    private static final Logger LOG = LoggerFactory
            .getLogger(ArenaPartitioning.class);



    public static String POM_TEMPLATE = "pom.template";

    @LassoInput(desc = "Enable features", optional = true)
    public List<String> features = new LinkedList<>();

    @LassoInput(desc = "Task to execute in arena (default 'SSNExecute')", optional = false)
    public String task = "SSNExecute";

    @LassoInput(desc = "Process reference implementation only", optional = true)
    public boolean referenceImplementationOnly = false;

    @LassoInput(desc = "Provide Sequence Sheets", optional = true)
    public Map<String, Object> sequences;

    @LassoInput(desc = "Use Sequences provided by benchmark with given id", optional = true)
    public String benchmark;
    @LassoInput(desc = "Use Sequences provided by benchmark", optional = true)
    public boolean noTestsFromBenchmark;

    @LassoInput(desc = "Obtain stored Sequences from the following actions", optional = true)
    public List<String> sequenceActions = Collections.emptyList();

    @LassoInput(desc = "Fully-qualified class name of CUT", optional = false)
    public String cut = "";

    @LassoInput(desc = "max. no. adaptations allowed", optional = true)
    public int maxAdaptations = 1;

    @LassoInput(desc = "Adapter strategy", optional = false)
    public String adapterStrategy = "DefaultAdaptationStrategy";

    @LassoInput(desc = "generate JUnit tests?", optional = true)
    public boolean generateJUnitTests = false;

    @LassoInput(desc = "export SRM as CSV?", optional = true)
    public boolean exportCsv = false;

    @LassoInput(desc = "Ignore visibility (access to constructors/methods)", optional = true)
    public boolean ignoreVisibility = true;

    @LassoInput(desc = "Write sequence records to SRM", optional = true)
    public boolean writeSequenceRecords = true;

    @LassoInput(desc = "Arena Container timeout in millis", optional = true)
    public long containerTimeout = 30 * 60 * 1000L; // half an hour

    @LassoInput(desc = "Timeout for each implementation in millis", optional = true)
    public long implementationTimeout = 30 * 1000L; // 30 seconds

    @LassoInput(desc = "How many implementations to run in parallel in the arena (default 1)", optional = true)
    public int threads = 1;

//    @LassoInput(desc = "Maven Repository URL", optional = true)
//    public String mavenRepository;

    /**
     * Adapt by sequence specification or by entire set of sequence specifications.
     */
    @LassoInput(desc = "adapt by sequence specification (true) or by all (false)", optional = true)
    private boolean adaptBySequenceSpecification = false;

    //@LassoInput(desc = "execution strategy has impact on unexpected execution behavior of  ('all' for all at once which is more efficient (but more error prone), 'one' for one by one which is less efficient but much more controllable)", optional = true)
    //private String executionStrategy = "all";

    private List<Sheet> stimulusSheets = new LinkedList<>();

    /**
     * scope-aware measurements
     */
    private Scope scope;

    @Override
    public void execute(LSLExecutionContext context, ActionConfiguration actionConfiguration) throws IOException {
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing " + this.getClass());
        }

        if(CollectionUtils.isEmpty(actionConfiguration.getAbstraction().getImplementations())) {
            // nothing to do
            setExecutables(Systems.fromAbstraction(actionConfiguration.getAbstraction(), getName()));

            LOG.info("No implementations. Returning .. ");

            return;
        }

        // set scope
        scope = actionConfiguration.getScope();

        Systems executables = null;

        // lang
        ArenaProjectManager arenaProjectManager = null;

        if(LangSupport.isJava(actionConfiguration)) {
            arenaProjectManager = new JavaArenaProjectManager(context);
            executables = arenaProjectManager.initNew(this,
                    getInstanceId(),
                    actionConfiguration.getAbstraction(),
                    POM_TEMPLATE,
                    (system, candidate, valueMap) -> {}, executable -> true);
        }

        if(LangSupport.isPython(actionConfiguration)) {
            arenaProjectManager = new PythonArenaProjectManager(context);
            executables = arenaProjectManager.initNew(this,
                    getInstanceId(),
                    actionConfiguration.getAbstraction(),
                    "FIXME", // FIXME project template for python projects
                    (system, candidate, valueMap) -> {}, executable -> true);
        }

        Validate.notNull(arenaProjectManager, "No project manager identified");

        Validate.notNull(executables, "Executables are null");

        // set
        setExecutables(executables);

        if(executables.getExecutables().isEmpty()) {
            LOG.warn("No executables to process. Returning ...");

            return;
        }

        // set additional sheets?
        processSheets(context, actionConfiguration, executables);

        // create job
        ArenaJob job = createJob(context, actionConfiguration, executables);
        // put
        ClusterEngine clusterEngine = context.getConfiguration().getService(ClusterEngine.class);
        ClusterArenaJobRepository jobRepository = clusterEngine.getArenaJobRepository();
        jobRepository.put(job.getId(), job);

//        ObjectMapper objectMapper = new ObjectMapper();
//        java.lang.System.out.println(objectMapper.writeValueAsString(job));

        // also make sure that the SRM is initialized (otherwise the client has no way to put cells)
        ClusterSRMRepository srmRepository = clusterEngine.getClusterSRMRepository();

        // also make sure to init report caches

        try {
            // for storing adaptations
            clusterEngine.getOrCreateReportCache(context.getExecutionId(), SystemAdapterReport.class);
        } catch (Throwable e) {
            LOG.warn("Setting up caches failed", e);
        }

        // executable corpus
        ExecutableCorpus corpus = context.getConfiguration().getExecutableCorpus();

        ExecutionEnvironment executionEnvironment = arenaProjectManager.createExecutionEnvironment(this, actionConfiguration, job, corpus, task, features, containerTimeout, implementationTimeout, threads);

        ExecutionEnvironmentManager executionEnvironmentManager = context.getExecutionEnvironmentManager();
        executionEnvironmentManager.run(executionEnvironment);

        // collect data

//        // show logs
//        if(LOG.isDebugEnabled()) {
//            LOG.debug("process stdout for job id => '{}' '{}'", job.getId(), arenaExecutionEnvironment.getLogs());
//        }

        // check job status
        ArenaJob updatedJob = jobRepository.get(job.getId());

        if (LOG.isInfoEnabled()) {
            LOG.info("Arena job status is '{}'", updatedJob.getStatus());
        }

        if (updatedJob.getStatus() == JobStatus.FINISHED) {
            LOG.info("Arena job finished successfully");
        } else {
            LOG.warn("Arena job failed");
        }

        // collect sequences
        collectSequences(context, executables);

        //
        if (exportCsv) {
            LOG.info("Exporting SRMs to CSV");

            try {
                Table table = srmRepository.sqlToTable("SELECT * FROM CELLVALUE WHERE executionId = ?", context.getExecutionId());

                File projectRoot = context.getWorkspace().getRoot(getInstanceId(), actionConfiguration.getAbstraction());

                table.write().csv(new File(projectRoot, "srm.csv"));
            } catch (Throwable e) {
                LOG.warn("Export CSV failed", e);
            }
        }
    }

    private void processSheets(LSLExecutionContext context, ActionConfiguration actionConfiguration, Systems executables) {
        // write manual sheets
        if (MapUtils.isNotEmpty(sequences)) {
            try {
                stimulusSheets.addAll(SequenceUtils.toSheetsJSONL(sequences, actionConfiguration.getAbstraction().getSpecification().getInterfaceSpecification().getLqlQuery()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // get from abstraction
        if(CollectionUtils.isNotEmpty(actionConfiguration.getAbstraction().getSpecification().getTests())) {
            List<Sheet> tests = actionConfiguration.getAbstraction().getSpecification().getTests();
            stimulusSheets.addAll(tests);
        }

//        if(LOG.isDebugEnabled()) {
//            stimulusSheets.forEach(s -> {
//                LOG.debug(ToStringBuilder.reflectionToString(s));
//
//                s.getInvocations().forEach(i -> LOG.debug("Type {} {}", i, i.getClass().getName()));
//            });
//        }

        // update executables
        executables.getSpecification().setTests(stimulusSheets);

        // XXX start: deprecated code blocks ...

        // benchmark set?
        if(benchmark != null) {
            LOG.info("Trying to load sequences from benchmark '{}' using abstraction '{}'", benchmark, executables.getAbstractionName());
            Benchmark b = context.getBenchmarkManager().load(benchmark);

            FunctionalAbstraction ab = b.getAbstractions().get(executables.getAbstractionName());

            if(!noTestsFromBenchmark) {
                Test2XSLX test2XSLX = new Test2XSLX();

                int s = 0;
                for(de.uni_mannheim.swt.lasso.benchmark.Sequence sequence : ab.getSequences()) {
                    String name = sequence.getId() + "_" + s;
                    LOG.info("Writing sheet '{}'", name);

                    Sequence seq = new Sequence();
                    seq.setName(name);
                    seq.setId(getName() + "_" + name);
                    seq.setActionId(getName());
                    executables.addSequence(seq);

                    try {
                        XSSFSheet xssfSheet = test2XSLX.createSheet(sequence, name);

                        for (System executable : executables.getExecutables()) {
                            try {
                                LOG.info("Writing sheet '{}' for '{}'", name, executable.getId());

                                test2XSLX.write(executable, xssfSheet, name);
                            } catch (Throwable e) {
                                LOG.warn("Failed to write sheet for '{}'", executable.getId());
                                LOG.warn("stack trace", e);
                            }
                        }
                    } catch (Throwable e) {
                        LOG.warn("Failed to read sheet '{}'", name);
                        LOG.warn("stack trace", e);
                    }

                    s++;
                }
            }

            // FIXME also set specification from benchmark
            String specification = ab.getLql();

            LOG.info("Specification set from benchmark '{}' to '{}'", benchmark, specification);
        }

        // generated sequences available? (TestGen actions)
        if(CollectionUtils.isNotEmpty(sequenceActions)) {
            List<de.uni_mannheim.swt.lasso.benchmark.Sequence> collectedSequences =
                    SequenceUtils.collectSequences(context, executables, sequenceActions);
            // write sequences locally for each candidate
            SequenceUtils.writeSequences2Xlsx(this, getExecutables(), collectedSequences, executables.getSpecification());
        }
    }

    private void collectSequences(LSLExecutionContext context, Systems executables) {
//        // remove (deprecated)
//        for (System executable : executables.getExecutables()) {
//            // make tests available in filesystem
//            List<File> testClasses = executable.getProject().getFiles(executable.getProject().getSrcTest(), "java");
//            testClasses.forEach(file -> {
//                if (LOG.isDebugEnabled()) {
//                    LOG.debug("Writing test to remote filesystem '{}'", file.getAbsolutePath());
//                }
//
//                try {
//                    context.getLassoFileSystem().write(file.getAbsolutePath(), file);
//                } catch (Throwable e) {
//                    LOG.warn("Failed to write test '{}'", file.getAbsolutePath());
//                    LOG.warn("Stack trace:", e);
//                }
//            });
//        }

        // read sheets from arena: Sheets
        try {
            LOG.info("Storing all sheets in specification");

            List<Sheet> stimulusSheets = Warehouse.queryStimulusSheets(context.getExecutionId(), getName(), executables.getAbstractionName());

            LOG.info("Found '{}' sheets", stimulusSheets.size());

            // TODO filter / duplicates? (if arena is re-run)
            //stimulusSheets = stimulusSheets.stream().filter(s -> StringUtils.startsWithIgnoreCase(s.getSignature(), "evo_")).toList();

            // adding or replacing?
//            if(CollectionUtils.isEmpty(getExecutables().getSpecification().getTests())) {
//                getExecutables().getSpecification().setTests(new LinkedList<>());
//            } else {
//                getExecutables().getSpecification().getTests().addAll(stimulusSheets);
//            }

            getExecutables().getSpecification().setTests(stimulusSheets);
        } catch (Throwable e) {
            //throw new RuntimeException(e);
            LOG.warn("Storing all sheets failed", e);
        }
    }

    private ArenaJob createJob(LSLExecutionContext context, ActionConfiguration actionConfiguration, Systems executables) {
        ArenaJob job = new ArenaJob();
        job.setId(String.format("%s_%s_%s", getInstanceId(), UUID.randomUUID(), actionConfiguration.getAbstraction().getName()));
        job.setExecutionId(context.getExecutionId());
        job.setWorkerNode(context.getWorkerNodeId());
        job.setAbstractionId(actionConfiguration.getAbstraction().getName());
        job.setActionId(getName());
        job.setBySequenceSpecification(adaptBySequenceSpecification);
        job.setImplementations(executables.getExecutables());

        if(actionConfiguration.getAbstraction().getSpecification() != null) {
            if(actionConfiguration.getAbstraction().getSpecification().getInterfaceSpecification() != null) {
                LOG.debug("Found specification in abstraction '{}'", actionConfiguration.getAbstraction().getSpecification().getInterfaceSpecification());

                job.setSpecification(actionConfiguration.getAbstraction().getSpecification().getInterfaceSpecification().getLqlQuery());
            }
        } else {
            throw new RuntimeException("No LQL specification found"); // FIXME validate specification
        }

        //job.setSheets(sheets);
        if (StringUtils.isNotBlank(cut)) {
            job.setCut(cut);
        }

        job.setAdapterStrategy(adapterStrategy);

        job.getConfiguration().put("perms", maxAdaptations);
        job.getConfiguration().put("generateTests", generateJUnitTests);
        job.getConfiguration().put("ignoreVisibility", ignoreVisibility);
        job.getConfiguration().put("writeSequenceRecords", writeSequenceRecords);

        // set scope
        job.setScope(scope);

        // amplify
        job.setReferenceImplementationOnly(referenceImplementationOnly);

        // set threads
        int threads = Runtime.getRuntime().availableProcessors() / 2;

        LOG.debug("Setting threads to '{}'", threads);

        job.setThreads(threads);

        // new parameters
        job.setAdapterStrategy("DefaultAdaptationStrategy");
//        List<Sheet> stimulusSheets = createSheets();
        LOG.debug("Setting stimulus sheets (JSONL) {}", stimulusSheets.size());
        job.setStimulusSheets(stimulusSheets);
        // new parameters

        return job;
    }

    /**
     * Stop execution environment NOW.
     */
    public void stopNow() {
//        if (arenaExecutionEnvironment != null) {
//            try {
//                arenaExecutionEnvironment.kill();
//            } catch (Throwable e) {
//                if (LOG.isWarnEnabled()) {
//                    LOG.warn("Killing Arena execution environment failed", e);
//                }
//            }
//        }
    }
}
