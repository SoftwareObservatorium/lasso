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

import de.uni_mannheim.swt.lasso.cluster.client.ClientReportRepository;
import de.uni_mannheim.swt.lasso.srm.ClientSRMRepository;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.ssl.SslContextFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Thin client for LASSO cluster.
 *
 * Client simply connects to one or more cluster nodes in order to access in-memory features.
 *
 * @author Marcus Kessel
 *
 * <a href="https://ignite.apache.org/docs/latest/thin-clients/java-thin-client">Ignite Thin Clients</a>
 */
// FIXME authentication
public class LassoClusterClient implements AutoCloseable {

    private final String ipAddresses;

    private IgniteClient client;

    private ClientSRMRepository srmRepository;

    private ClientReportRepository clientReportRepository;

    public LassoClusterClient(String ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public void start() {
        ClientConfiguration cfg = new ClientConfiguration().setAddresses(ipAddresses);

        // TODO specify port range! also for client connection

        // SSL auth
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

        this.client = Ignition.startClient(cfg);
    }

    public IgniteClient getClient() {
        return client;
    }

    @Override
    public void close() throws Exception {
        if(client != null) {
            client.close();
        }
    }

    public synchronized ClientSRMRepository getSrmRepository() {
        if(srmRepository == null) {
            srmRepository = new ClientSRMRepository(this);
        }

        return srmRepository;
    }

    public synchronized ClientReportRepository getClientReportRepository() {
        if(clientReportRepository == null) {
            clientReportRepository = new ClientReportRepository(this);
        }

        return clientReportRepository;
    }
}
