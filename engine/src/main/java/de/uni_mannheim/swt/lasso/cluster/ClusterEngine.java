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
package de.uni_mannheim.swt.lasso.cluster;

import de.uni_mannheim.swt.lasso.cluster.client.ClusterArenaJobRepository;
import de.uni_mannheim.swt.lasso.cluster.compute.job.ComputeActionTask;
import de.uni_mannheim.swt.lasso.cluster.data.repository.LassoRepository;
import de.uni_mannheim.swt.lasso.cluster.data.repository.ReportRepository;
import de.uni_mannheim.swt.lasso.cluster.fs.ClusterFileSystem;
import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import de.uni_mannheim.swt.lasso.cluster.event.ProgressEvent;
import de.uni_mannheim.swt.lasso.cluster.event.SessionEvent;
import de.uni_mannheim.swt.lasso.engine.dag.ActionRequest;
import de.uni_mannheim.swt.lasso.engine.dag.ActionResponse;
import de.uni_mannheim.swt.lasso.engine.data.*;
import de.uni_mannheim.swt.lasso.engine.data.fs.LassoFileSystem;
import de.uni_mannheim.swt.lasso.srm.ClusterSRMRepository;
import de.uni_mannheim.swt.lasso.srm.JDBC;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ignite.*;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.QueryEntity;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.compute.ComputeTaskFuture;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.spi.collision.fifoqueue.FifoQueueCollisionSpi;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.loadbalancing.roundrobin.RoundRobinLoadBalancingSpi;
import org.apache.ignite.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Ignite engine specifically configured for LASSO.
 *
 * @author Marcus Kessel
 */
public class ClusterEngine {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClusterEngine.class);

    public static final String LSESSION = "lsession";
    public static final String LPROGRESS = "lprogress";

    public static final String L_RECORD_CACHE = "LRecordCache";
    public static final String LASSO_FILE_SYSTEM = "lassoFileSystem";

    public static final String LNAME = "LNAME";
    public static final String LROLE = "LROLE";
    public static final String MASTER = "master";
    public static final String WORKER = "worker";

    private final String role;
    private final String localAddress;
    private final String multicastIp;

    private Ignite ignite;
    
    private final String instanceName;

    private ReportRepository reportRepository;
    private LassoRepository lassoRepository;
    private ClusterArenaJobRepository arenaJobRepository;
    private ClusterSRMRepository srmRepository;

    /**
     * Raw access to database.
     */
    private JDBC jdbc;

    private LassoFileSystem fileSystem;

    // https://apacheignite.readme.io/docs/tcpip-discovery#section-static-ip-finder
    private List<String> remoteAddresses;
    // https://apacheignite.readme.io/docs/tcpip-discovery#section-failure-detection-timeout
    private long failureDetectionTimeout = 30 * 1000L;

    private int parallelJobsNumber = 1;

    private boolean enablePersistence = true;

    public ClusterEngine(String instanceName, String role, String localAddress, String multicastIp) {
        //this.ignite = igniteInstance(instanceName, role, localAddress, multicastIp);
        this.instanceName = instanceName;
        this.role = role;
        this.localAddress = localAddress;
        this.multicastIp = multicastIp;
    }

    // TODO https://apacheignite.readme.io/docs/clients-vs-servers
    public void start() {
        IgniteConfiguration cfg = new IgniteConfiguration();

        // TODO enable auth requirement https://ignite.apache.org/docs/latest/security/authentication
        //cfg.setAuthenticationEnabled(true);

        SslContextFactory sslContextFactory = new SslContextFactory() {
            @Override
            protected InputStream openFileInputStream(String filePath) throws IOException {
                // load from classpath
                return ClusterEngine.class.getResourceAsStream(filePath);
            }
        };
        // use self-singed by default
        sslContextFactory.setKeyStoreType("PKCS12");
        sslContextFactory.setKeyStoreFilePath("/ssl/lasso_selfsigned.jks");
        sslContextFactory.setKeyStorePassword("lassorocks".toCharArray()); // FIXME externalize SSL pass
        sslContextFactory.setTrustStoreType("PKCS12");
        sslContextFactory.setTrustStoreFilePath("/ssl/lasso_selfsigned.jks");
        sslContextFactory.setTrustStorePassword("lassorocks".toCharArray()); // FIXME externalize SSL pass
        sslContextFactory.setTrustManagers(SslContextFactory.getDisabledTrustManager());

        cfg.setSslContextFactory(sslContextFactory);

        // client vs server mode
        cfg.setFailureDetectionTimeout(failureDetectionTimeout);
        cfg.setClientFailureDetectionTimeout(failureDetectionTimeout);

        // FIXME NoOpFailureHandler to prevent silly behaviors of grid (shutdown of grid if garbage collection is active)
        // http://apache-ignite-users.70518.x6.nabble.com/Ignite-2-7-Errors-td27575.html
        //cfg.setFailureHandler(new NoOpFailureHandler());
        // https://apacheignite.readme.io/docs/critical-failures-handling#section-critical-workers-health-check
        // increase period of inactivity to allow for large garbage collection pauses (millis)
        cfg.setSystemWorkerBlockedTimeout(15 * 60 * 1000);

        // set logging
        cfg.setGridLogger(new org.apache.ignite.logger.slf4j.Slf4jLogger());

        // TODO specify port range! also for client connection

        // set IP to listen on
        TcpCommunicationSpi tcp = new TcpCommunicationSpi();
        tcp.setLocalAddress(localAddress);
        tcp.setLocalPort(TcpCommunicationSpi.DFLT_PORT);

        // https://apacheignite.readme.io/docs/tcpip-discovery#section-failure-detection-timeout
        // The timeout automatically controls configuration parameters of TcpDiscoverySpi, such as socket timeout, message acknowledgment timeout and others. If any of these parameters is set explicitly, then the failure timeout setting will be ignored.
        //tcp.setConnectTimeout(10 * 1000L); //
        //tcp.setIdleConnectionTimeout(60 * 1000L); //
        cfg.setCommunicationSpi(tcp);

        TcpDiscoveryVmIpFinder ipFinder;
        // static addresses (local IP must be first!)
        if (CollectionUtils.isNotEmpty(remoteAddresses)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Using static IP addresses: '{}'",
                        remoteAddresses.stream().collect(Collectors.joining(",")));
            }

            // https://apacheignite.readme.io/docs/tcpip-discovery#section-static-ip-finder
            ipFinder = new TcpDiscoveryVmIpFinder();
            ipFinder.setAddresses(remoteAddresses);
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info("Using multicast IP finder on group: '{}'",
                        multicastIp);
            }

            // https://apacheignite.readme.io/docs/tcpip-discovery#section-multicast-ip-finder
            // set multi cast IP
            TcpDiscoveryMulticastIpFinder multicastIpFinder = new TcpDiscoveryMulticastIpFinder();
            multicastIpFinder.setLocalAddress(localAddress);
            multicastIpFinder.setMulticastGroup(multicastIp);

            ipFinder = multicastIpFinder;
        }

        TcpDiscoverySpi disco = new TcpDiscoverySpi();
        disco.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(disco);

        // Setting some custom name for the node.
        cfg.setIgniteInstanceName(instanceName);

        // set cluster node attributes
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(LNAME, instanceName);
        attributes.put(LROLE, role);

        cfg.setUserAttributes(attributes);

        // set max no of parallel compute tasks (jobs)
        //if(isMaster()) {
            // https://apacheignite.readme.io/docs/job-scheduling
            FifoQueueCollisionSpi fifoQueueCollisionSpi = new FifoQueueCollisionSpi();
            fifoQueueCollisionSpi.setParallelJobsNumber(parallelJobsNumber);
            cfg.setCollisionSpi(fifoQueueCollisionSpi);
        //}

        // load balancing strategy
        RoundRobinLoadBalancingSpi spi = new RoundRobinLoadBalancingSpi();
//        spi.setPerTask(true);
//        // these events are required for the per-task mode
//        cfg.setIncludeEventTypes(EventType.EVT_TASK_FINISHED, EventType.EVT_TASK_FAILED, EventType.EVT_JOB_MAPPED);
        // Override default load balancing SPI.
        cfg.setLoadBalancingSpi(spi);

        // Enabling peer-class loading feature.
        cfg.setPeerClassLoadingEnabled(true);

//        // Defining and creating a new cache to be used by Ignite Spring Data repository.
//        CacheConfiguration cacheConfig = new CacheConfiguration(L_RECORD_CACHE);
//
//        // Setting SQL schema for the cache.
//        cacheConfig.setIndexedTypes(
//                //DataKey.class, StringValue.class,
//                //DataKey.class, DoubleValue.class,
//                ReportKey.class, SerializableValue.class);
//
//        cfg.setCacheConfiguration(cacheConfig);

//        FileSystemConfiguration fileSystemCfg = new FileSystemConfiguration();
//        fileSystemCfg.setName(LASSO_FILE_SYSTEM);
                //        fileSystemCfg.setMetaCacheConfiguration(
                //                new CacheConfiguration("lassoMetaCache"));
                //        fileSystemCfg.setDataCacheConfiguration(
                //                new CacheConfiguration("lassoDataCache"));

//        cfg.setFileSystemConfiguration(fileSystemCfg);

        // thin client and JDBC connections
        // FIXME enable in the near future
//        ClientConnectorConfiguration clientConnectorCfg = new ClientConnectorConfiguration();
//        clientConnectorCfg.setSslEnabled(true);
//        //clientConnectorCfg.setSslContextFactory(sslContextFactory);
//        cfg.setClientConnectorConfiguration(clientConnectorCfg);

        // persistence
        if(enablePersistence) {
            // Ignite persistence configuration.
            DataStorageConfiguration storageCfg = new DataStorageConfiguration();
            // Enabling the persistence.
            storageCfg.getDefaultDataRegionConfiguration().setPersistenceEnabled(true);
            // Applying settings.
            cfg.setDataStorageConfiguration(storageCfg);
        }

        this.ignite = Ignition.start(cfg);
    }

    public boolean isMaster() {
        return StringUtils.equals(role, MASTER);
    }

    public boolean isWorker() {
        return StringUtils.equals(role, WORKER);
    }

    public IgniteCache getOrCreateReportCache(String executionId, Class<? extends LassoReport> type) {
        CacheConfiguration cacheConfig = new CacheConfiguration(toCacheName(executionId, type.getName()));

        cacheConfig.setIndexedTypes(ReportKey.class, type);
        // set schema -- may be ambiguous (since we use only class name not FQ)
        cacheConfig.setSqlSchema(String.format("%s_%s", executionId, type.getSimpleName()));

        return ignite.getOrCreateCache(cacheConfig);
    }

    public String toCacheName(String executionId, String reportName) {
        return String.format("%s_%s", executionId, reportName);
    }

    public IgniteCache getOrCreateBinaryReportCache(String executionId, String reportName, Map<String, String> valueTypes) {
        CacheConfiguration<ReportKey, BinaryObject> cacheConfig = new CacheConfiguration<>(toCacheName(executionId, reportName));
        cacheConfig.setQueryEntities(new ArrayList<QueryEntity>() {{
            QueryEntity e = new QueryEntity();
            e.setKeyType("de.uni_mannheim.swt.lasso.engine.data.ReportKey");
            e.setValueType(reportName);

            LinkedHashMap<String, String> fields = new LinkedHashMap<>(valueTypes);
            fields.put("LASTMODIFIED", "java.util.Date");
            fields.put("system", "java.lang.String");
            fields.put("abstraction", "java.lang.String");
            fields.put("permId", "java.lang.Integer");
            fields.put("action", "java.lang.String");
            fields.put("dataSource", "java.lang.String");

            e.setFields(fields);
            add(e);
        }});

        // set schema -- may be ambiguous (since we use only class name not FQ)
        cacheConfig.setSqlSchema(toCacheName(executionId, reportName));

        return ignite.getOrCreateCache(cacheConfig);
    }

    public Set<String> getReportCaches(String executionId) {
        return ignite.cacheNames().stream()
                .filter(c -> StringUtils.startsWith(c, executionId)).collect(Collectors.toSet());
    }

    public void removeReportCaches(String executionId) {
        getReportCaches(executionId).stream()
                // destroy
                .forEach(c -> ignite.destroyCache(c));
    }

    public List<ActionResponse> compute(List<ActionRequest> requests) {
        IgniteCompute compute = ignite.compute(getWorkerNodes());

        return compute.execute(ComputeActionTask.class, requests);
    }

    public List<ActionResponse> compute(List<ActionRequest> requests, long timeoutInMillis) {
        IgniteCompute compute = ignite.compute(getWorkerNodes());

        return compute.withTimeout(timeoutInMillis).execute(ComputeActionTask.class, requests);
    }

    public ComputeTaskFuture<List<ActionResponse>> computeAsync(List<ActionRequest> requests) {
        IgniteCompute compute = ignite.compute(getWorkerNodes());

        return compute.executeAsync(ComputeActionTask.class, requests);
    }

    public ClusterGroup getWorkerNodes() {
        return ignite.cluster().forAttribute(LROLE, WORKER);
    }

    public Optional<String> getIpAddress(ClusterNode node) {
        Optional<String> ipOp = node.addresses().stream()
                // get correct IP
                .peek(LOG::info)
                .filter(ip -> StringUtils.startsWithAny(ip, "134.", "192."))
                .findFirst();
        return ipOp;
    }

    public ClusterGroup getMasterNode() {
        return ignite.cluster().forAttribute(LROLE, MASTER);
    }

    public ClusterGroup getAllNodes() {
        ClusterGroup workers = getWorkerNodes();
        ClusterGroup masters = getMasterNode();

        List<ClusterNode> nodes = new LinkedList<>();
        nodes.addAll(workers.nodes());
        nodes.addAll(masters.nodes());

        return ignite.cluster().forNodes(nodes);
    };

    public void sendSessionEvent(SessionEvent event) {
        ignite.message(getWorkerNodes()).sendOrdered(LSESSION, event, 0);
    }

    @Deprecated
    public void addRemoteSessionListener(IgniteBiPredicate<UUID, SessionEvent> listener) {
        ignite.message(getWorkerNodes()).remoteListen(LSESSION, listener);
    }

    public void addLocalSessionListener(IgniteBiPredicate<UUID, SessionEvent> listener) {
        ignite.message(getWorkerNodes()).localListen(LSESSION, listener);
    }

    public void sendProgressEvent(ProgressEvent event) {
        ignite.message(getMasterNode()).sendOrdered(LPROGRESS, event, 0);
    }

    public void addRemoteProgressListener(IgniteBiPredicate<UUID, ProgressEvent> listener) {
        ignite.message(getMasterNode()).remoteListen(LPROGRESS, listener);
    }

    public synchronized ReportRepository getReportRepository() {
        if(reportRepository == null) {
            reportRepository = new ReportRepository(this);
        }

        return reportRepository;
    }

    public synchronized ClusterArenaJobRepository getArenaJobRepository() {
        if(arenaJobRepository == null) {
            arenaJobRepository = new ClusterArenaJobRepository(this);
        }

        return arenaJobRepository;
    }

    public synchronized ClusterSRMRepository getClusterSRMRepository() {
        if(srmRepository == null) {
            srmRepository = new ClusterSRMRepository(this);
        }

        return srmRepository;
    }

    public synchronized JDBC getJDBC() {
        if(jdbc == null) {
            jdbc = new JDBC();
        }

        return jdbc;
    }

    public LassoFileSystem getFileSystem() {
        if(fileSystem == null) {
            fileSystem = new ClusterFileSystem(this);
        }

        return fileSystem;
    }

    public Ignite getIgnite() {
        return ignite;
    }

    /**
     * Close
     */
    public void close() {
        ignite.close();
    }

    public List<String> getRemoteAddresses() {
        return remoteAddresses;
    }

    public void setRemoteAddresses(List<String> remoteAddresses) {
        this.remoteAddresses = remoteAddresses;
    }

    public long getFailureDetectionTimeout() {
        return failureDetectionTimeout;
    }

    public void setFailureDetectionTimeout(long failureDetectionTimeout) {
        this.failureDetectionTimeout = failureDetectionTimeout;
    }

    public synchronized LassoRepository getLassoRepository() {
        if(lassoRepository == null) {
            lassoRepository = new LassoRepository(this);
        }

        return lassoRepository;
    }

    public int getParallelJobsNumber() {
        return parallelJobsNumber;
    }

    public void setParallelJobsNumber(int parallelJobsNumber) {
        this.parallelJobsNumber = parallelJobsNumber;
    }

    public boolean isEnablePersistence() {
        return enablePersistence;
    }

    public void setEnablePersistence(boolean enablePersistence) {
        this.enablePersistence = enablePersistence;
    }
}
