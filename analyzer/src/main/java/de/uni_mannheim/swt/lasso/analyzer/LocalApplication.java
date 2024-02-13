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
package de.uni_mannheim.swt.lasso.analyzer;

import de.uni_mannheim.swt.lasso.analyzer.asm.ASMAnalyzer;
import de.uni_mannheim.swt.lasso.analyzer.batch.processor.AnalysisResult;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.analyzer.index.CompilationUnitRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.common.SolrInputDocument;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main entry point for single project analysis.
 *
 * @author Marcus Kessel
 */
public class LocalApplication {

    public static void main(String[] args) throws ParseException, IOException, SolrServerException {
        Options options = createOptions();
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);//, System.getProperties());

        String artifactPath = cmd.getOptionValue("path");
        String solrUrl = cmd.getOptionValue("solrurl");
        String solrUser = cmd.getOptionValue("solruser", "");
        String solrPass = cmd.getOptionValue("solrpass", "");
        String solrCore = cmd.getOptionValue("solrcore");

        String groupId = cmd.getOptionValue("groupId");
        String artifactId = cmd.getOptionValue("artifactId");
        String version = cmd.getOptionValue("version");

        String metadata = cmd.getOptionValue("metadata");

        // metadata=key1,value1|key2,value2 etc.
        final Map<String, String> meta;
        if (StringUtils.isNotBlank(metadata)) {
            String[] pairs = StringUtils.split(metadata, '|');
            meta = Arrays.stream(pairs).map(p -> StringUtils.split(p, ','))
                    .peek(p -> System.out.println(Arrays.toString(p)))
                    .collect(Collectors.toMap(p -> p[0], p -> p[1]));
        } else {
            meta = Collections.emptyMap();
        }

        File parent = new File(artifactPath);

        System.out.println(parent.getAbsolutePath());

        MavenArtifact mavenArtifact = new MavenArtifact(groupId, artifactId, version);
        MavenArtifact testArtifact = new MavenArtifact(groupId, artifactId, version);
        mavenArtifact.setTestArtifact(testArtifact);

        Collection<File> jars = FileUtils.listFiles(parent, new String[]{"jar"}, false);
        for (File jar : jars) {
            if (StringUtils.contains(jar.getName(), "javadoc")) {
                continue;
            }

            if (StringUtils.endsWith(jar.getName(), "-test-sources.jar")) {
                testArtifact.setSourceJar(jar);
            } else if (StringUtils.endsWith(jar.getName(), "-sources.jar")) {
                mavenArtifact.setSourceJar(jar);
            } else if (StringUtils.endsWith(jar.getName(), "-tests.jar")) {
                testArtifact.setBinaryJar(jar);
            } else {
                mavenArtifact.setBinaryJar(jar);
            }
        }

        Collection<File> poms = FileUtils.listFiles(parent, new String[]{"pom"}, false);
        if (CollectionUtils.isNotEmpty(poms)) {
            Optional<File> pom = poms.stream().findFirst();
            if (pom.isPresent()) {
                mavenArtifact.setPomFile(pom.get());
                mavenArtifact.getTestArtifact().setPomFile(pom.get());
            }
        }

        System.out.println(ToStringBuilder.reflectionToString(mavenArtifact));

        ASMAnalyzer analyzer = new ASMAnalyzer(true, true);

        List<CompilationUnit> units = analyzer.analyze(mavenArtifact);

        SolrClient solrServer = solrClient(solrUrl, solrUser, solrPass, solrCore);

        CompilationUnitRepository repository = new CompilationUnitRepository(solrServer) {

            @Override
            protected void addCustom(SolrInputDocument document) {
                String owner = cmd.getOptionValue("owner");
                addIfExists(document, "owner", owner);

                if (MapUtils.isNotEmpty(meta)) {
                    for (String m : meta.keySet()) {
                        addIfExists(document, m, meta.get(m));
                    }
                }
            }
        };

        // FIXME set additional fields
        AnalysisResult analysisResult = new AnalysisResult();
        analysisResult.setCompilationUnits(units);
        analysisResult.setMavenArtifact(mavenArtifact);

        repository.save(analysisResult);

        // final commit
        solrServer.commit();
    }

    static SolrClient solrClient(String solrUrl, String solrUser, String solrPass, String solrCandidatesCore) {
        if (StringUtils.isNotBlank(solrUser)) {
            // do basic auth
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(solrUser, solrPass);
            provider.setCredentials(AuthScope.ANY, credentials);

            HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider)
                    .addInterceptorFirst(new HttpRequestInterceptor() {

                        @Override
                        public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                            //
                            byte[] credentials = Base64
                                    .encodeBase64((solrUser + ":" + solrPass).getBytes(StandardCharsets.UTF_8));
                            request.setHeader("Authorization", "Basic " + new String(credentials, StandardCharsets.UTF_8));
                        }
                    }).build();

            HttpSolrClient solrServer = new HttpSolrClient.Builder(solrUrl + "/" + solrCandidatesCore)
                    .withHttpClient(client).build();

            return solrServer;
        } else {
            return new HttpSolrClient.Builder(solrUrl + "/" + solrCandidatesCore)
                    .build();
        }
    }

    private static Options createOptions() {
        Options options = new Options();

        options.addOption("h", "help", false, "print this message");
        options.addOption("p", "path", true, "Path to artifacts");
        options.addOption("ss", "solrurl", true, "Solr URL");
        options.addOption("su", "solruser", true, "Solr User");
        options.addOption("sp", "solrpass", true, "Solr Pass");
        options.addOption("sc", "solrcore", true, "Solr Core");

        options.addOption("mg", "groupId", true, "Maven groupId");
        options.addOption("ma", "artifactId", true, "Maven artifactId");
        options.addOption("mv", "version", true, "Maven version");

        options.addOption("md", "metadata", true, "additional meta data to store");

        options.addOption("o", "owner", true, "owner (like student)");

        return options;
    }
}
