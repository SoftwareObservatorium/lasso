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
package de.uni_mannheim.swt.lasso.cluster.client;

import de.uni_mannheim.swt.lasso.cluster.LassoClusterClient;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Debugging client access to Ignite.
 *
 * @author Marcus Kessel
 */
public class ExecutablesClientIntegrationTest {

    @Test
    public void test_export_experiment2() throws IOException {
        String ipAddresses = "134.155.95.67:10800"; // SWT100
        exportPitestReports(ipAddresses, "7dff61d0-307e-4be6-b927-c482debbe179", new File("/tmp/exp2_evotime_mapsV2.txt"), false);
    }

    public void exportPitestReports(String ipAddresses, String executionId, File report, boolean refImplOnly) throws IOException {
        try(LassoClusterClient clusterClient = new LassoClusterClient(ipAddresses)) {
            // start
            clusterClient.start();

            ClientLassoRepository clientReportRepository = new ClientLassoRepository(clusterClient);

            Map<String, Systems> abstractions = clientReportRepository.getAbstractions(executionId, "evosuiteAlt");

            System.out.println("SIZE " + abstractions.size());

            abstractions.forEach((k,v) -> {
                v.getExecutables().forEach(e -> System.out.println(e.getId()));
            });

        } catch (Throwable e) {
            System.err.println("failed with error.");
            e.printStackTrace();
        } finally {

        }
    }
}
