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
package de.uni_mannheim.swt.lasso.service;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.cluster.rest.Auth;
import de.uni_mannheim.swt.lasso.cluster.rest.LassoWorkerClient;
import de.uni_mannheim.swt.lasso.core.dto.*;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewItem;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewRequest;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewResponse;
import de.uni_mannheim.swt.lasso.engine.*;
import de.uni_mannheim.swt.lasso.engine.action.ActionManager;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.action.annotations.*;
import de.uni_mannheim.swt.lasso.engine.dag.DAG;
import de.uni_mannheim.swt.lasso.engine.dag.ExecutionPlan;
import de.uni_mannheim.swt.lasso.engine.dag.model.LGraph;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceManager;
import de.uni_mannheim.swt.lasso.engine.workspace.WorkspaceNotFoundException;
import de.uni_mannheim.swt.lasso.lsl.LSLDelegatingScript;
import de.uni_mannheim.swt.lasso.lsl.LSLRunner;
import de.uni_mannheim.swt.lasso.lsl.SimpleLogger;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;
import de.uni_mannheim.swt.lasso.service.notification.Notification;
import de.uni_mannheim.swt.lasso.service.notification.NotificationFactory;
import de.uni_mannheim.swt.lasso.service.notification.NotificationService;
import de.uni_mannheim.swt.lasso.service.persistence.*;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.cluster.ClusterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author Marcus Kessel
 */
public class LassoManager {

    private static final Logger LOG = LoggerFactory.getLogger(LassoManager.class);

    /**
     * Max. no. of parallel script executions
     */
    private int pipelineJobs = 1;

    private final LassoEngine lassoEngine;
    private final LassoConfiguration lassoConfiguration;

    private NotificationService notificationService;
    private WorkspaceManager workspaceManager;

    private UserRepository userRepository;
    private ScriptJobRepository scriptJobRepository;

    /**
     * FIXME maybe outsource
     */
    private final ExecutorService jobExecutor;

    /**
     * Current script executions
     */
    private Map<String, CompletableFuture<?>> scriptExecutions = new ConcurrentHashMap<>();

    public LassoManager(LassoEngine lassoEngine, LassoConfiguration lassoConfiguration) {
        this.lassoEngine = lassoEngine;
        this.lassoConfiguration = lassoConfiguration;

        this.jobExecutor = createJobExecutor();
    }

    /**
     * Shutdown parallel threads etc.
     */
    public void shutdown() {
        if (jobExecutor != null) {
            jobExecutor.shutdown();
        }

        if (lassoEngine != null) {
            lassoEngine.shutdown();
        }
    }

    public boolean isRunning() {
        if (jobExecutor == null) {
            return false;
        }

        return getActiveCount() > 0;
    }

    public int getActiveCount() {
        if (jobExecutor instanceof ThreadPoolExecutor) {
            return ((ThreadPoolExecutor) jobExecutor).getActiveCount();
        } else {
            return -1;
        }
    }

    protected LSLScript createScript(LSLRequest lassoRequest, UserInfo userInfo, String executionId) {
        LSLScript script = new LSLScript();
        script.setExecutionId(executionId);
        script.setContent(lassoRequest.getScript());
        script.setEmail(lassoRequest.getEmail());
        script.setIpAddress(userInfo.getRemoteIpAddress());

        return script;
    }

    protected String generateExecutionId() {
        //
        final String executionId = UUID.randomUUID().toString();

        return executionId;
    }

    public LSLResponse execute(LSLRequest lassoRequest, UserInfo userInfo) throws IOException {
        //
        final String executionId = generateExecutionId();
        LSLScript script = createScript(lassoRequest, userInfo, executionId);

        ScriptJobStatus status;
        if (StringUtils.equals(lassoRequest.getType(), ScriptJobStatus.DRAFT.name())) {
            status = ScriptJobStatus.DRAFT;
        } else {
            // create new async job for script execution
            CompletableFuture<?> jobFuture = CompletableFuture
                    .supplyAsync(() -> executeScript(script), jobExecutor)
                    .handle((result, error) -> postProcess(result, error, script));

            // put
            scriptExecutions.put(executionId, jobFuture);

            status = ScriptJobStatus.PENDING;
        }

        ScriptJob scriptJob = new ScriptJob();
        scriptJob.setExecutionId(executionId);
        scriptJob.setShared(lassoRequest.isShare());
        scriptJob.setContent(lassoRequest.getScript());

        // get name
        String name = StringUtils.substringBetween(lassoRequest.getScript().replace(" ", ""), "study(name:'", "')");
        scriptJob.setName(name);

        // get owner
        User owner = getOwner(userInfo);
        scriptJob.setOwner(owner);
        scriptJob.setStatus(status);
        scriptJob.setStart(new Date());
        // persist job
        scriptJobRepository.save(scriptJob);

        LSLResponse response = new LSLResponse();
        response.setExecutionId(executionId);
        response.setStatus(scriptJob.getStatus().toString());

        return response;
    }

    public LGraph graph(LSLRequest lassoRequest, UserInfo userInfo) throws IOException {
        LSLRunner runner = new LSLRunner();
        LSLDelegatingScript script = runner.runScript(lassoRequest.getScript(), new SimpleLogger());

        ExecutionPlan executionPlan = LassoEngine.createActionExecutionPlan(script.getLasso());

        LGraph graph = DAG.writeGraphToModel(executionPlan);

        return graph;
    }

    private User getOwner(UserInfo userInfo) throws IOException {
        Optional<User> ownerOptional = userRepository.findByUsername(userInfo.getUserDetails().getUsername());

        return ownerOptional.orElseThrow(
                () -> new IOException(String.format("Cannot find user '%s'", userInfo.getUserDetails().getUsername())));
    }

    public ExecutionStatus getExecutionStatus(String executionId, UserInfo userInfo) throws IOException, WorkspaceNotFoundException {
        //
        ScriptJob scriptJob = getScriptJob(executionId, userInfo);

        ExecutionStatus executionStatus = new ExecutionStatus();
        executionStatus.setExecutionId(executionId);
        executionStatus.setStatus(scriptJob.getStatus().toString());
        executionStatus.setStart(scriptJob.getStart());

        return executionStatus;
    }

    public ExecutionResult getExecutionResult(String executionId, UserInfo userInfo) throws IOException, WorkspaceNotFoundException {
        ScriptJob scriptJob = getScriptJob(executionId, userInfo);

        // check status
        if (scriptJob.getStatus() == ScriptJobStatus.PENDING) {
            throw new IllegalStateException("LSL script still running");
        }

        // FIXME handle workspace
        Workspace workspace = workspaceManager.load(executionId);
        LSLScript script = workspace.getScript();

        // TODO
        ExecutionResult executionResult = new ExecutionResult();
        executionResult.setExecutionId(executionId);

        return executionResult;
    }

    private LSLExecutionResult executeScript(LSLScript script) throws LSLExecutionException {
        try {
            return lassoEngine.execute(script);
        } catch (Throwable e) {
            //
            throw new LSLExecutionException(script, "LSL Script Execution failed", e);
        }
    }

    private LSLExecutionResult postProcess(LSLExecutionResult lslExecutionResult, Throwable error, LSLScript script)
            throws LSLExecutionException {
        //
        Workspace workspace = null;
        try {
            workspace = workspaceManager.load(lslExecutionResult.getExecutionId());
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Failed to load workspace for %s",
                        script.getExecutionId()), e);
            }
        }

        if (error != null) {
            // do error handling
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("LSL Script execution failed for '%s'", script), error);
            }

            // notify failed
            try {
                notifyFailed(script, workspace);
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(String.format("notifyFailed failed for '%s'", script), error);
                }
            }
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info("LSL Script execution finished for '{}'", script);
            }

            // notify
            try {
                notifyFinished(lslExecutionResult, workspace);
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(String.format("notifyFinished failed for '%s'", script), error);
                }
            }
        }

        return lslExecutionResult;
    }

    private void setStatus(String executionId, ScriptJobStatus status) throws IOException {
        // load
        Optional<ScriptJob> scriptJobOptional = scriptJobRepository.findByExecutionId(executionId);

        ScriptJob scriptJob = scriptJobOptional.orElseThrow(
                () -> new IOException(String.format("Could not find execution id in DB '%s'", executionId)));

        // status
        scriptJob.setStatus(status);
        scriptJob.setEnd(new Date());
        scriptJobRepository.save(scriptJob);
    }

    private ScriptJob getScriptJob(String executionId, UserInfo userInfo) throws IOException, WorkspaceNotFoundException {
        //
        User owner = getOwner(userInfo);
        // get script job
        Optional<ScriptJob> scriptJobOptional = scriptJobRepository.findByExecutionId(executionId);
        ScriptJob scriptJob = scriptJobOptional.orElseThrow(
                () -> new WorkspaceNotFoundException(String.format("Not found for %s", executionId)));
        if (!scriptJob.getOwner().getId().equals(owner.getId())) {
            throw new IOException(
                    String.format("User '%s' not allowed to access script job '%s'", userInfo.getUserDetails().getUsername(), executionId));
        }

        return scriptJob;
    }

    private void notifyFinished(LSLExecutionResult lslExecutionResult, Workspace workspace) throws IOException {
        //
        String executionId = lslExecutionResult.getExecutionId();
        // set new status
        setStatus(executionId, ScriptJobStatus.SUCCESSFUL);

        if (StringUtils.isNotEmpty(lslExecutionResult.getScript().getEmail()) && notificationService != null) {
            Notification notification = NotificationFactory.onScriptExecutionFinished(lslExecutionResult, workspace);
            try {
                notificationService.send(notification);
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Sending email notification failed", e);
                }
            }
        }
    }

    private void notifyFailed(LSLScript script, Workspace workspace) throws IOException {
        //
        String executionId = script.getExecutionId();
        // set new status
        setStatus(executionId, ScriptJobStatus.FAILED);

        if (StringUtils.isNotEmpty(script.getEmail()) && notificationService != null) {
            Notification notification = NotificationFactory.onScriptExecutionFailed(script, workspace);
            try {
                notificationService.send(notification);
            } catch (IOException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Sending email notification failed", e);
                }
            }
        }
    }

    protected ExecutorService createJobExecutor() {
        // create threadpool
        return Executors.newFixedThreadPool(getPipelineJobs(),
                new ThreadFactory() {

                    private AtomicInteger inc = new AtomicInteger();

                    /**
                     * Assign a specific name to the TDS job threads
                     */
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r,
                                "lasso-script-" + inc.getAndIncrement());
                    }

                });
    }

    public void setNotificationService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public void setWorkspaceManager(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void setScriptJobRepository(ScriptJobRepository scriptJobRepository) {
        this.scriptJobRepository = scriptJobRepository;
    }

    // TODO add distributed nature: get remote ZIPs and concat
    public StreamingResponseBody streamRecords(RecordsRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException {
        MultiValuedMap<String, String> distribution = new ArrayListValuedHashMap<>();

        request.getFilePatterns().forEach(p -> {
            String[] parts = StringUtils.split(p, ":");

            String id = parts[0];
            String path = parts[1];

            distribution.put(id, path);
        });

        ClusterEngine clusterEngine = this.lassoConfiguration.getService(ClusterEngine.class);

        Map<String, File> zips = new HashMap<>(distribution.keySet().size());

        List<ClusterNode> remoteNodes = clusterEngine.getWorkerNodes().nodes().stream().filter(node -> distribution.keySet().contains(node.id().toString())).collect(Collectors.toList());

        // collect ZIPs from remote
        for (ClusterNode remoteNode : remoteNodes) {
            // get from remote
            Optional<String> ipOp = clusterEngine.getIpAddress(remoteNode);
            LassoWorkerClient lassoWorkerClient = getWorkerClient(ipOp.get());

            if (LOG.isInfoEnabled()) {
                LOG.info("Retrieving ZIP from '{}'", ipOp.get());
            }

            try {
                RecordsRequest recordsRequest = new RecordsRequest();
                recordsRequest.setFilePatterns(new ArrayList<>(distribution.get(remoteNode.id().toString())));
                Path tmpFile = Files.createTempFile("lassofs", ".zip");

                lassoWorkerClient.downloadAsZIP(executionId, recordsRequest, tmpFile.toFile());

                zips.put(remoteNode.id().toString(), tmpFile.toFile());
            } catch (Throwable e) {
                LOG.warn("LassoWorkerClient failed", e);
            }
        }

        ClusterNode localNode = clusterEngine.getIgnite().cluster().localNode();
        String localId = localNode.id().toString();

        // collect ZIP from local node
        if(distribution.keySet().contains(localId)) {
            final Workspace workspace;
            try {
                workspace = workspaceManager.load(executionId);
            } catch (Throwable e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(String.format("Failed to load workspace for %s",
                            executionId), e);
                }

                throw new WorkspaceNotFoundException();
            }

            // clean paths
            try {
                Path tmpFile = Files.createTempFile("lassofs", ".zip");
                workspace.createZipFile(new FileOutputStream(tmpFile.toFile()), new ArrayList<>(distribution.get(localId)));

                zips.put(localId, tmpFile.toFile());
            } catch (IOException e) {
                LOG.warn("Local zip failed", e);
            }
        }

        return output -> {
            ZipOutputStream zipOut = null;
            try {
                zipOut = new ZipOutputStream(output);

                for (String rid : zips.keySet()) {
                    File fileToZip = zips.get(rid);
                    FileInputStream fis = new FileInputStream(fileToZip);

                    ZipEntry zipEntry = new ZipEntry(String.format("%s.zip", rid));
                    zipOut.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                    fis.close();
                }
            } finally {
                IOUtils.closeQuietly(zipOut);
                IOUtils.closeQuietly(output);

                // remove zips
                zips.values().forEach(File::delete);
            }
        };
    }

    // XXX rename into streamFile
    public StreamingResponseBody streamCSV(RecordsRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException, IOException {
        //
        String csvPath = request.getFilePatterns().get(0);

        ClusterEngine clusterEngine = this.lassoConfiguration.getService(ClusterEngine.class);
        ClusterNode localNode = clusterEngine.getIgnite().cluster().localNode();
        String localId = localNode.id().toString();

        String[] parts = StringUtils.split(csvPath, ":");

        String id = parts[0];
        String path = parts[1];

        // local
        if(StringUtils.equals(id, localId) || this.lassoConfiguration.getProperty("cluster.embedded", boolean.class)) {
            // local file
            final Workspace workspace;
            try {
                workspace = workspaceManager.load(executionId);
            } catch (Throwable e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn(String.format("Failed to load workspace for %s",
                            executionId), e);
                }

                throw new WorkspaceNotFoundException();
            }

            String[] files = workspace.scanForFiles(Arrays.asList(path));
            if (files == null) {
                throw new IOException(String.format("File not found: '%s'", Arrays.asList(request.getFilePatterns())));
            }

            if (files.length > 1) {
                throw new IOException(String.format("More than one File matched: '%s'", Arrays.asList(request.getFilePatterns())));
            }

            File file = new File(workspace.getRoot(), path);
            return output -> {
                // local
                FileUtils.copyFile(file, output);
            };
        } else {
            // remote
            Optional<ClusterNode> nodeOp = clusterEngine.getWorkerNodes().nodes().stream().filter(node -> StringUtils.equals(node.id().toString(), id)).findFirst();
            // get from remote
            Optional<String> ipOp = clusterEngine.getIpAddress(nodeOp.get());
            LassoWorkerClient lassoWorkerClient = getWorkerClient(ipOp.get());

            if (LOG.isInfoEnabled()) {
                LOG.info("Retrieving '{}' from '{}'", path, ipOp.get());
            }

            return output -> {
                try {
                    RecordsRequest recordsRequest = new RecordsRequest();
                    recordsRequest.setFilePatterns(Arrays.asList(path));
                    Path tmpFile = Files.createTempFile("lassofs", "xxx");

                    lassoWorkerClient.downloadFile(executionId, recordsRequest, tmpFile.toFile());

                    FileUtils.copyFile(tmpFile.toFile(), output);

                    // remove temp file
                    tmpFile.toFile().delete();
                } catch (Throwable e) {
                    LOG.warn("LassoWorkerClient failed", e);
                }
            };
        }
    }

    @Deprecated
    public RecordsResponse getRecords(RecordsRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException {
        //
        final Workspace workspace;
        try {
            workspace = workspaceManager.load(executionId);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Failed to load workspace for %s",
                        executionId), e);
            }

            throw new WorkspaceNotFoundException();
        }

        RecordsResponse recordsResponse = new RecordsResponse();

        recordsResponse.setFiles(Arrays.asList(workspace.scanForFiles(request.getFilePatterns())));

        return recordsResponse;
    }

    public FileViewResponse getFiles(FileViewRequest request, String executionId, UserInfo userInfo) throws WorkspaceNotFoundException {
        //
        final Workspace workspace;
        try {
            workspace = workspaceManager.load(executionId);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Failed to load workspace for %s",
                        executionId), e);
            }

            throw new WorkspaceNotFoundException();
        }

        ClusterEngine clusterEngine = this.lassoConfiguration.getService(ClusterEngine.class);
        ClusterNode localNode = clusterEngine.getIgnite().cluster().localNode();
        String id = localNode.id().toString();

        Map<String, FileViewItem> items = workspace.scanForFileItems(request.getFilePatterns());
        // rewrite path with id
        items.values().forEach(i -> {
            i.setValue(String.format("%s:%s", id, i.getValue()));
        });

        FileViewItem root = items.get("/");
        if(root == null) { // no files matched
            root = new FileViewItem();
            root.setText(File.separator);
            root.setValue(File.separator);
        }

        root.setText(String.format("%s (%s)", root.getText(), "master"));

        //debugTree(root, 0);

        FileViewResponse response = new FileViewResponse();
        //
        boolean embedded = this.lassoConfiguration.getProperty("cluster.embedded", boolean.class);
        if (!embedded) {
            FileViewItem clusterRoot = new FileViewItem();
            clusterRoot.setText("cluster");
            clusterRoot.setValue("");

            clusterRoot.addChild(root);

            response.setRoot(clusterRoot);

            if (LOG.isInfoEnabled()) {
                LOG.info("Retrieving remote file listings");
            }
            //

            for (ClusterNode workerNode : clusterEngine.getWorkerNodes().nodes()) {
                Optional<String> ipOp = clusterEngine.getIpAddress(workerNode);
                LassoWorkerClient lassoWorkerClient = getWorkerClient(ipOp.get());

                try {
                    FileViewResponse workerResponse = lassoWorkerClient.getFiles(executionId, request);

                    FileViewItem workerRoot = workerResponse.getRoot();
                    workerRoot.setText(String.format("%s (%s)", workerRoot.getText(), workerNode.id().toString()));

                    clusterRoot.getChildren().add(workerRoot);
                } catch (Throwable e) {
                    LOG.warn("LassoWorkerClient failed", e);
                }
            }
        } else {
            response.setRoot(root);
        }

        return response;
    }

    private LassoWorkerClient getWorkerClient(String ipAddress) {
        Auth auth = new Auth();
        auth.setUser(this.lassoConfiguration.getProperty("worker.rest.user", String.class));
        auth.setPassword(this.lassoConfiguration.getProperty("worker.rest.password", String.class));
        LassoWorkerClient lassoWorkerClient = new LassoWorkerClient(String.format("http://%s:9988/", ipAddress), auth);

        return lassoWorkerClient;
    }

    public LSLInfoResponse getLSLInfo(UserInfo userInfo) {
        ActionManager actionManager = lassoEngine.getActionManager();

        LSLInfoResponse lslInfoResponse = new LSLInfoResponse();

        Map<String, Class<? extends DefaultAction>> registry = actionManager.getRegistry();

        Map<String, Map> actions = new LinkedHashMap<>();
        registry.forEach((k, v) -> {
            Map<String, Object> actionInfo = new HashMap<>();

            // is it stable?
            Stable stable = v.getAnnotation(Stable.class);

            actionInfo.put("state", stable != null ? "stable" : "unstable");
            actionInfo.put("type", v.getSimpleName());
            actionInfo.put("description", v.getAnnotation(LassoAction.class).desc());
            actionInfo.put("distributable", v.getAnnotation(Local.class) == null);
            actionInfo.put("disablePartitioning", v.getAnnotation(DisablePartitioning.class) != null);

            Map<String, Object> configuration = new HashMap<>();

            actionInfo.put("configuration", configuration);
            try {
                for (Field field : actionManager.getInputFields(v)) {
                    field.setAccessible(true);

                    LassoInput lassoInput = field.getAnnotation(LassoInput.class);
                    if (lassoInput != null) {
                        Map<String, Object> input = new HashMap<>();

                        //input.put("name", field.getName());
                        input.put("type", field.getType().getName());
                        input.put("description", lassoInput.desc());
                        input.put("optional", lassoInput.optional());

                        configuration.put(field.getName(), input);
                    }
                }

            } catch (Throwable e) {
                //
            }

            actions.put(v.getSimpleName(), actionInfo);
        });

        lslInfoResponse.setActions(actions);

        return lslInfoResponse;
    }

    public int getPipelineJobs() {
        return pipelineJobs;
    }

    public void setPipelineJobs(int pipelineJobs) {
        this.pipelineJobs = pipelineJobs;
    }
}
