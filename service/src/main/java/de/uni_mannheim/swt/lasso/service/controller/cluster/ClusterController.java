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
package de.uni_mannheim.swt.lasso.service.controller.cluster;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.service.app.logging.LogReader;
import de.uni_mannheim.swt.lasso.service.controller.BaseApi;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * LASSO's cluster API.
 * 
 * @author Marcus Kessel
 *
 */
@RestController
@RequestMapping(value = "/api/v1/lasso/cluster")
@Tag(name = "Cluster API")
public class ClusterController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(ClusterController.class);

    @Autowired
    ClusterEngine clusterEngine;

    @Autowired
    private Environment env;

    LogReader logReader = new LogReader();

    @Operation(summary = "Get X last lines of Log", description = "Get X last lines of Log")
    @RequestMapping(value = "/log", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<List<String>> getLog(
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {

            //
            List<String> lines = logReader.tail(new File(env.getProperty("lasso.logging.file", String.class)), 100);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning log response to '{}'",
                        userInfo.getRemoteIpAddress());
            }

            // 200
            return ResponseEntity.ok(lines);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get log response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get log response"), e);
        }
    }

    @Operation(summary = "Get Cluster Metrics", description = "Gets metrics about the current cluster")
    @RequestMapping(value = "/metrics", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<ClusterMetrics> getClusterMetrics(
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {

            //
            ClusterMetrics clusterMetrics = clusterEngine.getIgnite().cluster().metrics();

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning cluster metrics response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(clusterMetrics));
            }

            // 200
            return ResponseEntity.ok(clusterMetrics);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get cluster metrics response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get cluster metrics response"), e);
        }
    }

    @Operation(summary = "Get Cluster Nodes", description = "Gets Nodes of current cluster")
    @RequestMapping(value = "/nodes", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<Cluster> getNodes(
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        try {

            //
            Collection<ClusterNode> nodes = clusterEngine.getIgnite().cluster().nodes();

            Cluster cluster = new Cluster();

            cluster.setNodes(nodes.stream().map(n -> {
                Node node = new Node();
                node.setId(n.id());
                node.setAddresses(n.addresses());
                // toString to avoid serialization issues
                node.setAttributes(
                        n.attributes().keySet().stream()
                                .collect(Collectors.toMap(k -> k, k -> {
                                    Object o = n.attributes().get(k);
                                    if(o == null) {
                                        return "n/a";
                                    } else {
                                        return o.toString();
                                    }
                                })));
                node.setClusterMetrics(n.metrics());
                node.setHostNames(n.hostNames());

                return node;
            }).collect(Collectors.toList()));

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning cluster nodes response to '{}':\n{}",
                        userInfo.getRemoteIpAddress(),
                        ToStringBuilder
                                .reflectionToString(cluster));
            }

            // 200
            return ResponseEntity.ok(cluster);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not get cluster nodes response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not get cluster nodes response"), e);
        }
    }

    /**
     *
     * @author Marcus Kessel
     */
    public static class Cluster {
        private List<Node> nodes;

        public List<Node> getNodes() {
            return nodes;
        }

        public void setNodes(List<Node> nodes) {
            this.nodes = nodes;
        }
    }

    /**
     *
     * @author Marcus Kessel
     */
    public static class Node {
        private UUID id;
        private Map<String, Object> attributes;
        private Collection<String> addresses;
        private Collection<String> hostNames;

        private ClusterMetrics clusterMetrics;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public Map<String, Object> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        public Collection<String> getAddresses() {
            return addresses;
        }

        public void setAddresses(Collection<String> addresses) {
            this.addresses = addresses;
        }

        public Collection<String> getHostNames() {
            return hostNames;
        }

        public void setHostNames(Collection<String> hostNames) {
            this.hostNames = hostNames;
        }

        public ClusterMetrics getClusterMetrics() {
            return clusterMetrics;
        }

        public void setClusterMetrics(ClusterMetrics clusterMetrics) {
            this.clusterMetrics = clusterMetrics;
        }
    }
}
