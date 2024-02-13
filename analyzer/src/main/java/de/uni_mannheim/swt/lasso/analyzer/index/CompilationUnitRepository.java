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

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import de.uni_mannheim.swt.lasso.analyzer.batch.processor.AnalysisResult;
import de.uni_mannheim.swt.lasso.analyzer.batch.reader.MavenArtifact;
import de.uni_mannheim.swt.lasso.analyzer.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Solr repository interface for {@link CompilationUnit}s.
 *
 * @author Marcus Kessel
 */
public class CompilationUnitRepository {

    private static final Logger LOG = LoggerFactory.getLogger(CompilationUnitRepository.class);

    private final SolrClient solrClient;

    private final MethodRepository methodRepository;

    /**
     * Constructor
     *
     * @param solrClient     {@link SolrClient}
     */
    public CompilationUnitRepository(SolrClient solrClient) {
        this.solrClient = solrClient;

        this.methodRepository = new MethodRepository(solrClient);
    }

    /**
     * Check if artifact is stored in index
     *
     * @param uri Artifact URI (Maven URI, i.e., groupId:artifactId:version)
     * @return true if non-empty result returned by SolR
     * @throws IOException Solr Query error
     */
    public boolean isArtifactAvailable(String uri) throws IOException {
        try {
            SolrQuery solrQuery = new SolrQuery();
            // escape ":"
            solrQuery.setQuery("uri:" + StringUtils.replace(uri, ":", "\\:"));
            solrQuery.setFields("uri");
            // only one row
            solrQuery.setRows(1);

            // execute query
            SolrDocumentList solrDocList = solrClient.query(solrQuery).getResults();

            return solrDocList.getNumFound() > 0;
        } catch (Throwable e) {
            throw new IOException("Could not execute isArtifactAvailable query for field " + uri, e);
        }
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
     * @param analysisResult {@link AnalysisResult} instance
     */
    public void save(AnalysisResult analysisResult) {
        MavenArtifact mavenArtifact = analysisResult.getMavenArtifact();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving analysis result for " + ToStringBuilder.reflectionToString(mavenArtifact));
        }

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

            // transform all units to documents
            for (CompilationUnit compilationUnit : analysisResult.getCompilationUnits()) {
                // check if accepted
                if (!accept(compilationUnit)) {
                    // skip
                    continue;
                }

                // create document
                SolrInputDocument document = createDocument(compilationUnit);

                // generate ID
                String id = UUID.randomUUID().toString();

                addIfExists(document, "id", id);

                // type
                addIfExists(document, "doctype_s", "class");

                // has generic signature?
                addIfExists(document, "generic_b", compilationUnit.isGeneric());

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

                // save method ids in parent document
                List<String> methodIds = methodRepository.save(analysisResult, compilationUnit, id);

                addIfExists(document, "methodids_ss", methodIds);

                // add metadata
                addMetaData(analysisResult.getMetaData(), document);

                // add meta (e.g., benchmark etc.)
                addMeta(analysisResult.getMeta(), document);

                // add custom
                addCustom(document);

                // save
                solrClient.add(document);
            }

            // update compilation unit with method ids
            //updateCompilationUnitDocument(docId, methodIds);

            if (LOG.isInfoEnabled()) {
                LOG.info("Saved Maven Artifact " + uri);
            }

            // final commit
            //solrClient.commit();
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Couldn't save maven artifact " + ToStringBuilder.reflectionToString(mavenArtifact), e);
            }
        }
    }

    private void addMeta(Map<String, String> meta, SolrInputDocument document) {
        if (MapUtils.isEmpty(meta)) {
            return;
        }

        try {
            if(MapUtils.isNotEmpty(meta)) {
                for(String m : meta.keySet()) {
                    addIfExists(document, m, meta.get(m));
                }
            }
        } catch (Throwable e) {
            //
        }
    }

    protected void addCustom(SolrInputDocument document) {
        //                 addIfExists(document, "lang", "java");
    }

    private void addMetaData(MetaData metaData, SolrInputDocument document) {
        if (metaData == null || MapUtils.isEmpty(metaData.getValues())) {
            return;
        }

        try {
            metaData.getValues().forEach((k, v) -> {
                if (v != null & v instanceof List<?>) {
                    List<?> list = (List<?>) v;
                    if (CollectionUtils.isNotEmpty(list)) {
                        addIfExists(document, "meta_" + k + "_ss", list);
                    }

                } else if (v != null) {
                    addIfExists(document, "meta_" + k + "_s", v);
                }
            });
        } catch (Throwable e) {
            //
        }
    }

    /**
     * @param compilationUnit {@link CompilationUnit} instance
     * @return true if unit accepted
     */
    protected boolean accept(CompilationUnit compilationUnit) {
        return !compilationUnit.isAnonymousClass() && !compilationUnit.isInnerClass() && compilationUnit.isSource();
    }

    /**
     * @param compilationUnit {@link CompilationUnit} instance
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

        // methods
        if (CollectionUtils.isNotEmpty(compilationUnit.getMethods())) {
            for (Method method : compilationUnit.getMethods()) {
                // add
                addMethod(method, document, false);
            }
        }

        // inherited methods
        if (CollectionUtils.isNotEmpty(compilationUnit.getInheritedMethods())) {
            try {
                for (Method method : compilationUnit.getInheritedMethods()) {
                    // add
                    addMethod(method, document, true);
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        // source
        if (StringUtils.isNotEmpty(compilationUnit.getSourceCode())) {
            addIfExists(document, "content", compilationUnit.getSourceCode());
            addIfExists(document, "hash", compilationUnit.getHash());
        }

        // super classes
        if (CollectionUtils.isNotEmpty(compilationUnit.getSuperClassNames())) {
            for (String superClassName : compilationUnit.getSuperClassNames()) {
                addIfExists(document, "superclass_fqs", superClassName);
            }
        }

        // interfaces
        if (CollectionUtils.isNotEmpty(compilationUnit.getInterfaceNames())) {
            for (String interfaceName : compilationUnit.getInterfaceNames()) {
                addIfExists(document, "directinterface_fqs", interfaceName);
            }
        }

        // interfaces
        if (CollectionUtils.isNotEmpty(compilationUnit.getInterfaceClassNames())) {
            for (String interfaceName : compilationUnit.getInterfaceClassNames()) {
                addIfExists(document, "interface_fqs", interfaceName);
            }
        }

        // directsuperclass
        addIfExists(document, "directsuperclass_fq", compilationUnit.getSuperClassName());

        // modifiers
        if (CollectionUtils.isNotEmpty(compilationUnit.getModifiers())) {
            for (ModifierType type : compilationUnit.getModifiers()) {
                switch (type) {
                    case PUBLIC:
                        addIfExists(document, "visibility", type.toString().toLowerCase());
                        break;
                    case PROTECTED:
                        addIfExists(document, "visibility", type.toString().toLowerCase());
                        break;
                    case PRIVATE:
                        addIfExists(document, "visibility", type.toString().toLowerCase());
                        break;

                    case PACKAGEPRIVATE:
                        addIfExists(document, "visibility", type.toString().toLowerCase());
                        break;
                    default:
                        addIfExists(document, "modifier", type.toString().toLowerCase());
                }
            }
        }

        // new
        addNewFields(document, compilationUnit);

        return document;
    }

    /**
     * 01.02.2016
     * <p>
     * private int byteCodeVersion; private String byteCodeName;
     * <p>
     * private List<String> javaKeywords;
     * <p>
     * private List<String> dependencies; private Map<String, Double> measures
     */
    private void addNewFields(SolrInputDocument document, CompilationUnit unit) {
        // uses schema / less

        // deps
        if (CollectionUtils.isNotEmpty(unit.getDependencies())) {
            unit.getDependencies().stream().forEach(dep -> addIfExists(document, "dep_fqs", dep));
            addIfExists(document, "m_deps_td", new Integer(unit.getDependencies().size()).doubleValue());
        } else {
            addIfExists(document, "m_deps_td", 0d);
        }

        // keywords
        if (CollectionUtils.isNotEmpty(unit.getJavaKeywords())) {
            unit.getJavaKeywords().stream().forEach(dep -> addIfExists(document, "keyword_ss", dep));
        }

        // name
        addIfExists(document, "bytecodename_s", unit.getByteCodeName());
        // version
        addIfExists(document, "bytecodeversion_i", unit.getByteCodeVersion());

        // measures
        unit.getMeasures().forEach((k, v) -> {
            addIfExists(document, "m_" + k + "_td", v);
        });

        // metrics

    }

    /**
     * Utility method that checks for null input before insertion
     *
     * @param document {@link SolrInputDocument} instance
     * @param name     Filed name
     * @param value    Field value
     */
    protected static void addIfExists(SolrInputDocument document, String name, Object value) {
        if (value != null) {
            document.addField(name, value);
        }
    }

//    /**
//     * Add method to {@link SolrInputDocument}.
//     *
//     * @param method   {@link Method} instance
//     * @param document {@link SolrInputDocument} instance
//     * @deprecated #addMethod
//     */
//    @Deprecated
//    private void deprecated_addMethod(Method method, SolrInputDocument document) {
//        ArrayList<String> parameters = new ArrayList<String>();
//        if (method.getParameters() != null) {
//            for (Parameter parameter : method.getParameters()) {
//                if (StringUtils.isEmpty(parameter.getType())) {
//                    parameters.add("java.lang.Object");
//                } else {
//                    parameters.add(parameter.getType());
//                }
//            }
//        }
//
//        // TODO package private??
//        String visibility = getVisibility(method);
//
//        // TODO set default for null?
//        String returnType = method.getReturnParameter() != null ? method.getReturnParameter().getType()
//                : "java.lang.Object";
//
//        // shortend
//        String returnTypeShort = StringUtils.substringAfterLast(returnType, ".");
//        if (StringUtils.isBlank(returnTypeShort)) {
//            returnTypeShort = returnType;
//        }
//
//        List<String> parametersShort = parameters.stream().map(p -> {
//            String shortend = StringUtils.substringAfterLast(p, ".");
//            if (StringUtils.isBlank(shortend)) {
//                shortend = p;
//            }
//
//            return shortend;
//        }).collect(Collectors.toList());
//        Signature signature = new Signature(visibility, method.getName(), new ArrayList<>(parametersShort),
//                returnTypeShort);
//
//        Signature signatureFq = new Signature(visibility, method.getName(), parameters, returnType);
//
//        // for constructors, we always index both
//        addIfExists(document, "method_fqs", method.getName());
//        addIfExists(document, "methodOrigSignature_sig", signature.toStringOrigSignature(false).trim());
//        addIfExists(document, "methodOrigSignatureFq_sig", signatureFq.toStringOrigSignature(true).trim());
//
//        // addIfExists(document, "methodSignatureParamsOrdered_sigs",
//        // signature.toStringParameterOrdered(false));
//        // addIfExists(document, "methodSignatureParamsOrdered_sigs",
//        // signature.toStringParameterOrdered(true));
//        // addIfExists(document, "methodSignatureParamsOrderedVisibility_sigs",
//        // signature.toStringParameterOrderedVisibility(false));
//        // addIfExists(document, "methodSignatureParamsOrderedVisibility_sigs",
//        // signature.toStringParameterOrderedVisibility(true));
//
//        // add keywords
//        List<String> keywords = method.getJavaKeywords();
//        String keyStr = "";
//        if (CollectionUtils.isNotEmpty(keywords)) {
//            keyStr = ";" + keywords.stream().map(s -> "kw_" + s).collect(Collectors.joining(";"));
//        }
//        addIfExists(document, "methodSignatureParamsOrderedKeywords_sig",
//                signature.toStringParameterOrdered(false) + keyStr);
//        addIfExists(document, "methodSignatureParamsOrderedKeywordsFq_sig",
//                signatureFq.toStringParameterOrdered(true) + keyStr);
//
//        // without keywords
//        addIfExists(document, "methodSignatureParamsOrdered_sig", signature.toStringParameterOrdered(false));
//        addIfExists(document, "methodSignatureParamsOrderedFq_sig", signatureFq.toStringParameterOrdered(true));
//
//        addIfExists(document, "methodSignatureParamsOrderedSyntax_sig",
//                signature.toStringParameterOrderedSyntax(false));
//        addIfExists(document, "methodSignatureParamsOrderedSyntaxFq_sig",
//                signatureFq.toStringParameterOrderedSyntax(true));
//
//        addIfExists(document, "methodSignatureParamsOrderedSyntaxKeywords_sig",
//                signature.toStringParameterOrderedSyntax(false) + keyStr);
//        addIfExists(document, "methodSignatureParamsOrderedSyntaxKeywordsFq_sig",
//                signatureFq.toStringParameterOrderedSyntax(true) + keyStr);
//    }

    /**
     * Add method to {@link SolrInputDocument}.
     *
     * @param method   {@link Method} instance
     * @param document {@link SolrInputDocument} instance
     */
    private void addMethod(Method method, SolrInputDocument document, boolean inherited) {
        String prefix = inherited ? "inherited" : "";

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
            //addIfExists(document, "content", method.getContent());
            addIfExists(document, prefix+"hash_ss", method.getHash());
        }

        // TODO package private??
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
        addIfExists(document, prefix+"method_fqs", method.getName());
        addIfExists(document, prefix+"methodOrigSignature_sigs", signature.toStringOrigSignature(false).trim());
        addIfExists(document, prefix+"methodOrigSignatureFq_sigs", signatureFq.toStringOrigSignature(true).trim());

        // addIfExists(document, "methodSignatureParamsOrdered_sigs",
        // signature.toStringParameterOrdered(false));
        // addIfExists(document, "methodSignatureParamsOrdered_sigs",
        // signature.toStringParameterOrdered(true));
//        addIfExists(document, "methodSignatureParamsOrderedSyntax_sigs",
//                signature.toStringParameterOrderedSyntax(false));
//        addIfExists(document, "methodSignatureParamsOrderedSyntaxFq_sigs",
//                signatureFq.toStringParameterOrderedSyntax(true));
        // addIfExists(document, "methodSignatureParamsOrderedVisibility_sigs",
        // signature.toStringParameterOrderedVisibility(false));
        // addIfExists(document, "methodSignatureParamsOrderedVisibility_sigs",
        // signature.toStringParameterOrderedVisibility(true));

        // add keywords
        List<String> keywords = new LinkedList<>();
        if (method.getJavaKeywords() != null) {
            keywords.addAll(method.getJavaKeywords());
        }

        // add no. of parameters
        keywords.add("ps" + parameters.size());

        // also add as metric
        //addIfExists(document, "m_paramsize_td", parameters.size());

        String keyStr = "";
        if (CollectionUtils.isNotEmpty(keywords)) {
            keyStr = ";" + keywords.stream().map(s -> "kw_" + s).collect(Collectors.joining(";"));
        }

        addIfExists(document, prefix+"methodSignatureParamsOrderedKeywords_sigs",
                signature.toStringParameterOrdered(false) + keyStr);
        addIfExists(document, prefix+"methodSignatureParamsOrderedKeywordsFq_sigs",
                signatureFq.toStringParameterOrdered(true) + keyStr);

        // without keywords
        addIfExists(document, prefix+"methodSignatureParamsOrdered_sigs", signature.toStringParameterOrdered(false));
        addIfExists(document, prefix+"methodSignatureParamsOrderedFq_sigs", signatureFq.toStringParameterOrdered(true));

        addIfExists(document, prefix+"methodSignatureParamsOrderedSyntax_sigs",
                signature.toStringParameterOrderedSyntax(false));
        addIfExists(document, prefix+"methodSignatureParamsOrderedSyntaxFq_sigs",
                signatureFq.toStringParameterOrderedSyntax(true));

        addIfExists(document, prefix+"methodSignatureParamsOrderedSyntaxKeywords_sigs",
                signature.toStringParameterOrderedSyntax(false) + keyStr);
        addIfExists(document, prefix+"methodSignatureParamsOrderedSyntaxKeywordsFq_sigs",
                signatureFq.toStringParameterOrderedSyntax(true) + keyStr);

        // new
        //addNewFields(document, method);

        // bytecode sigs
        addIfExists(document, prefix+"bytecodemethodname_ss", method.getByteCodeName());
    }

    /**
     * @param method {@link Method} instance
     * @return visibility modifier for given {@link Method}
     */
    public static String getVisibility(Method method) {
        if (method.getModifiers() == null) {
            return "";
        }

        // visibility
        for (ModifierType type : method.getModifiers()) {
            switch (type) {
                case PUBLIC:
                    return type.toString().toLowerCase();
                case PROTECTED:
                    return type.toString().toLowerCase();
                case PRIVATE:
                    return type.toString().toLowerCase();
                default:
                    //
            }
        }

        return "";
    }
}
