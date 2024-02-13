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
package de.uni_mannheim.swt.lasso.analyzer.index;

import java.util.*;
import java.util.stream.Collectors;

import de.uni_mannheim.swt.lasso.analyzer.batch.processor.AnalysisResult;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.ClassType;
import de.uni_mannheim.swt.lasso.analyzer.model.CompilationUnit;
import de.uni_mannheim.swt.lasso.analyzer.model.Method;
import de.uni_mannheim.swt.lasso.analyzer.model.Parameter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solr repository interface for {@link CompilationUnit}s.
 * 
 * @author Marcus Kessel
 *
 */
public class MethodRepository {

    private static final Logger LOG = LoggerFactory.getLogger(MethodRepository.class);

    private final SolrClient solrClient;

    /**
     * Constructor
     * 
     * @param solrClient
     *            {@link SolrClient}
     */
    public MethodRepository(SolrClient solrClient) {
        this.solrClient = solrClient;
    }

    /**
     * Query for specific field, return first result
     * 
     * @param query
     *            Solr Query
     * @param field
     *            SolR field
     * @param rows
     *            Max. no. of rows to fetch in each page iteration
     * @return {@link Object} value instance
     * @throws IOException
     *             Query failed
     */

    /**
     * Save {@link CompilationUnit}s.
     * 
     * @param analysisResult
     *            {@link AnalysisResult} instance
     * @param compilationUnit {@link CompilationUnit}
     * @param parentId Parent
     *                 @return id of document
     */
    public List<String> save(AnalysisResult analysisResult, CompilationUnit compilationUnit, String parentId) {
        MavenArtifact mavenArtifact = analysisResult.getMavenArtifact();

        try {
            // origin
            String uri = mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId() + ":"
                    + mavenArtifact.getVersion();
            
            // is test artifact?
            String classifier = null;
            if (mavenArtifact.isTests()) {
                classifier = "tests";
            }
            
            String origin = mavenArtifact.getSourceJar().getName();

            // check if accepted
            if (!accept(compilationUnit)) {
                // skip
                return null;
            }

            List<String> methodIds = new LinkedList<>();

            // methods
            if (CollectionUtils.isNotEmpty(compilationUnit.getMethods())) {

                // document == method
                for (Method method : compilationUnit.getMethods()) {
                    // create document
                    SolrInputDocument document = createDocument(compilationUnit);

                    // generate ID
                    String mid = UUID.randomUUID().toString();
                    addIfExists(document, "id", mid);

                    methodIds.add(mid);

                    // parent id
                    addIfExists(document, "pid_s", parentId);

                    // parent class has generic signature?
                    addIfExists(document, "generic_b", compilationUnit.isGeneric());

                    // type
                    addIfExists(document, "doctype_s", "method");

                    // version
                    addIfExists(document, "bytecodeversion_i", compilationUnit.getByteCodeVersion());

                    // creationDate
                    Date creationDate = new Date();
                    addIfExists(document, "creationDate", creationDate);

                    // lastModified
                    addIfExists(document, "lastModified", creationDate);

                    // set origin
                    addIfExists(document, "groupId", mavenArtifact.getGroupId());
                    addIfExists(document, "artifactId", mavenArtifact.getArtifactId());
                    addIfExists(document, "version", mavenArtifact.getVersion());

                    addIfExists(document, "uri", uri);
                    // non-version uri for collapsing
                    addIfExists(document, "nvuri_s", mavenArtifact.getGroupId() + ":" + mavenArtifact.getArtifactId());
                    addIfExists(document, "origin", origin);
                    
                    // classifier (i.e., tests)
                    addIfExists(document, "classifier_s", classifier);

                    addIfExists(document, "lang", "java");

                    // is latest version?
                    addIfExists(document, "latestVersion", mavenArtifact.isLatestVersion());

                    // set version head
                    addIfExists(document, "versionHead_ti", mavenArtifact.getVersionHead());

                    // add
                    addMethod(method, document);

                    // save
                    solrClient.add(document);
                }
            }

            // if (LOG.isInfoEnabled()) {
            // LOG.info("Saved Maven Artifact " + uri);
            // }

            // final commit
            //solrClient.commit();

            return methodIds;
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Couldn't save maven artifact " + ToStringBuilder.reflectionToString(mavenArtifact), e);
            }

            return null;
        }
    }

    /**
     * @param compilationUnit
     *            {@link CompilationUnit} instance
     * @return true if unit accepted
     */
    protected boolean accept(CompilationUnit compilationUnit) {
        return !compilationUnit.isAnonymousClass() && !compilationUnit.isInnerClass() && compilationUnit.isSource();
    }

    /**
     * @param compilationUnit
     *            {@link CompilationUnit} instance
     * @return {@link SolrInputDocument} instance
     */
    private SolrInputDocument createDocument(CompilationUnit compilationUnit) {
        // XXX fails in Solr > 6 ( new HashMap<>() to constructor )
        SolrInputDocument document = new SolrInputDocument();

        // name
        addIfExists(document, "name_fq", compilationUnit.getName());
        addIfExists(document, "packagename_fq", compilationUnit.getPackageName());

        // add class type (first char)
        String classType = compilationUnit.getType() != null
                ? StringUtils.lowerCase(StringUtils.substring(compilationUnit.getType().name(), 0, 1))
                : StringUtils.lowerCase(StringUtils.substring(ClassType.CLASS.name(), 0, 1));
        addIfExists(document, "type", classType);

        // add class keywords
        if (CollectionUtils.isNotEmpty(compilationUnit.getJavaKeywords())) {
            compilationUnit.getJavaKeywords().stream().forEach(dep -> addIfExists(document, "keyword_ss", dep));
        }

        return document;
    }

    /**
     * 01.02.2016
     * 
     * private int byteCodeVersion; private String byteCodeName;
     * 
     * private List<String> javaKeywords;
     * 
     * private List<String> dependencies; private Map<String, Double> measures
     */
    private void addNewFields(SolrInputDocument document, Method unit) {
        // uses schema / less
        // deps
        if (CollectionUtils.isNotEmpty(unit.getDependencies())) {
            unit.getDependencies().stream().forEach(dep -> addIfExists(document, "dep_fqs", dep));
            addIfExists(document, "m_deps_td", new Integer(unit.getDependencies().size()).doubleValue());
        } else {
            addIfExists(document, "m_deps_td", 0d);
        }

        if (CollectionUtils.isNotEmpty(unit.getCalls())) {
            unit.getCalls().stream().forEach(dep -> addIfExists(document, "mcall_fqs", dep));
            addIfExists(document, "m_umcalls_td", new Integer(unit.getDependencies().size()).doubleValue());
        } else {
            addIfExists(document, "m_umcalls_td", 0d);
        }

        // measures
        unit.getMeasures().forEach((k, v) -> {
            addIfExists(document, "m_" + k + "_td", v);
        });

        // name
        addIfExists(document, "bytecodename_s", unit.getByteCodeName());
    }

    /**
     * Utility method that checks for null input before insertion
     * 
     * @param document
     *            {@link SolrInputDocument} instance
     * @param name
     *            Filed name
     * @param value
     *            Field value
     */
    private static void addIfExists(SolrInputDocument document, String name, Object value) {
        if (value != null) {
            document.addField(name, value);
        }
    }

    /**
     * Add method to {@link SolrInputDocument}.
     * 
     * @param method
     *            {@link Method} instance
     * @param document
     *            {@link SolrInputDocument} instance
     */
    private void addMethod(Method method, SolrInputDocument document) {
        ArrayList<String> parameters = new ArrayList<String>();
        if (method.getParameters() != null) {
            for (Parameter parameter : method.getParameters()) {
                if (StringUtils.isEmpty(parameter.getType())) {
                    parameters.add("java.lang.Object");
                } else {
                    parameters.add(parameter.getType());
                }
            }
        }

        // source
        if (StringUtils.isNotEmpty(method.getContent())) {
            addIfExists(document, "content", method.getContent());
            addIfExists(document, "hash", method.getHash());
        }

        // XXX part of keywords!
        String visibility = "";

        // TODO set default for null?
        String returnType = method.getReturnParameter() != null ? method.getReturnParameter().getType()
                : "java.lang.Object";

        // shortend
        String returnTypeShort = StringUtils.substringAfterLast(returnType, ".");
        if (StringUtils.isBlank(returnTypeShort)) {
            returnTypeShort = returnType;
        }

        List<String> parametersShort = parameters.stream().map(p -> {
            String shortend = StringUtils.substringAfterLast(p, ".");
            if (StringUtils.isBlank(shortend)) {
                shortend = p;
            }

            return shortend;
        }).collect(Collectors.toList());
        Signature signature = new Signature(visibility, method.getName(), new ArrayList<>(parametersShort),
                returnTypeShort);

        Signature signatureFq = new Signature(visibility, method.getName(), parameters, returnType);

        // handle constructor methods specifically in addition to usual method
        // handling
        // if (method.isConstructor()) {
        // addIfExists(document, "constructorParameterOrdered_sigs",
        // signature.toStringConstructorParameterOrdered(false));
        // addIfExists(document, "constructorParameterOrdered_sigs",
        // signatureFq.toStringConstructorParameterOrdered(true));
        // addIfExists(document, "constructorParameterOrderedSyntax_sigs",
        // signature.toStringConstructorParameterOrderedSyntax(false));
        // addIfExists(document, "constructorParameterOrderedSyntax_sigs",
        // signatureFq.toStringConstructorParameterOrderedSyntax(true));
        // }

        // for constructors, we always index both
        addIfExists(document, "method_fq", method.getName());
        addIfExists(document, "methodOrigSignature_ssig", signature.toStringOrigSignature(false).trim());
        addIfExists(document, "methodOrigSignatureFq_ssig", signatureFq.toStringOrigSignature(true).trim());

        // addIfExists(document, "methodSignatureParamsOrdered_sigs",
        // signature.toStringParameterOrdered(false));
        // addIfExists(document, "methodSignatureParamsOrdered_sigs",
        // signature.toStringParameterOrdered(true));
        // addIfExists(document, "methodSignatureParamsOrderedVisibility_sigs",
        // signature.toStringParameterOrderedVisibility(false));
        // addIfExists(document, "methodSignatureParamsOrderedVisibility_sigs",
        // signature.toStringParameterOrderedVisibility(true));

        // add keywords
        List<String> keywords = new LinkedList<>();
        if(method.getJavaKeywords() != null) {
            keywords.addAll(method.getJavaKeywords());
        }

        // add no. of parameters
        keywords.add("ps" + parameters.size());

        // also add as metric
        addIfExists(document, "m_paramsize_td", parameters.size());

        String keyStr = "";
        if (CollectionUtils.isNotEmpty(keywords)) {
            keyStr = ";" + keywords.stream().map(s -> "kw_" + s).collect(Collectors.joining(";"));
        }

        addIfExists(document, "methodSignatureParamsOrderedKeywords_ssig",
                signature.toStringParameterOrdered(false) + keyStr);
        addIfExists(document, "methodSignatureParamsOrderedKeywordsFq_ssig",
                signatureFq.toStringParameterOrdered(true) + keyStr);

        // without keywords
        addIfExists(document, "methodSignatureParamsOrdered_ssig", signature.toStringParameterOrdered(false));
        addIfExists(document, "methodSignatureParamsOrderedFq_ssig", signatureFq.toStringParameterOrdered(true));

        addIfExists(document, "methodSignatureParamsOrderedSyntax_ssig",
                signature.toStringParameterOrderedSyntax(false));
        addIfExists(document, "methodSignatureParamsOrderedSyntaxFq_ssig",
                signatureFq.toStringParameterOrderedSyntax(true));

        addIfExists(document, "methodSignatureParamsOrderedSyntaxKeywords_ssig",
                signature.toStringParameterOrderedSyntax(false)  + keyStr);
        addIfExists(document, "methodSignatureParamsOrderedSyntaxKeywordsFq_ssig",
                signatureFq.toStringParameterOrderedSyntax(true)  + keyStr);

        // new
        addNewFields(document, method);
    }
}
