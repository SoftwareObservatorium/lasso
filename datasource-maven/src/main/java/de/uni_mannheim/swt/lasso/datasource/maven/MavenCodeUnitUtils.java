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
package de.uni_mannheim.swt.lasso.datasource.maven;

import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.Clazz;
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class MavenCodeUnitUtils {

    private static final Logger LOG = LoggerFactory
            .getLogger(MavenCodeUnitUtils.class);

    public static CodeUnit toImplementation(SolrDocument doc) {
        CodeUnit i = new CodeUnit();

        i.setDocType((String) doc.getFieldValue("doctype_s"));
        i.setUnitType("method".equals(i.getDocType()) ? CodeUnit.CodeUnitType.METHOD : CodeUnit.CodeUnitType.CLASS);

        i.setId((String) doc.getFirstValue("id"));
        i.setParentId((String) doc.getFirstValue("pid_s"));
        i.setName((String) doc.getFirstValue("name_sexact"));
        i.setPackagename((String) doc.getFirstValue("packagename_sexact"));
        i.setBytecodeName((String) doc.getFirstValue("bytecodename_s"));
        i.setGroupId((String) doc.getFirstValue("groupId"));
        i.setArtifactId((String) doc.getFirstValue("artifactId"));
        i.setVersion((String) doc.getFirstValue("version"));
        i.setScore((float) doc.getFieldValue("score"));
        i.setContent((String) doc.getFieldValue("content"));
        i.setHash((String) doc.getFieldValue("hash"));
        i.setType((String) doc.getFieldValue("type"));

        i.setMethods(doc.containsKey("methodOrigSignature_sigs_exact") ?
                doc.getFieldValues("methodOrigSignature_sigs_exact")
                        .stream().map(o -> o.toString()).filter(o -> !StringUtils.startsWith(o,"access$")).collect(Collectors.toList())
        : Collections.emptyList());

        i.setSuperClasses(doc.containsKey("superclass_exact") ?
                doc.getFieldValues("superclass_exact").stream().map(s -> StringUtils.replace((String) s, "/", ".")).collect(Collectors.toList()) : null);

        i.setInterfaces(doc.containsKey("interface_exact") ?
                doc.getFieldValues("interface_exact").stream().map(s -> StringUtils.replace((String) s, "/", ".")).collect(Collectors.toList()) : null);

        i.setDependencies(doc.containsKey("dep_exact") ?
                doc.getFieldValues("dep_exact").stream()
                        .map(s -> StringUtils.replace((String) s, "/", ".")).collect(Collectors.toList()) : null);

        // add default metric(s) if necessary
        Map<String, Double> measures = new TreeMap<String, Double>();
        doc.keySet().stream().filter(k -> StringUtils.startsWith(k, "m_")).forEach(m -> {
            // XXX make human readable
            Object obj = doc.getFirstValue(m);

            // primitive wrapper
            if (obj instanceof Number) {
                double d = ((Number) obj).doubleValue();
                measures.put(m, d);
            }

            // primitive
            if(obj != null && obj.getClass().isPrimitive()) {
                measures.put(m, (double) doc.getFirstValue(m));
            }
        });
        // add
        measures.put("m_luceneScore", (double) i.getScore());
        i.setMeasures(measures);

        // add metadata
        Map<String, Object> metaData = new HashMap<>();

        doc.keySet().stream().filter(k -> StringUtils.startsWith(k, "meta_")).forEach(m -> {
            Collection values = doc.getFieldValues(m);

            if(values.size() == 1) {
                Object value = values.iterator().next();

                String stringValue = (String) value;

                // substitute placeholders
                if(StringUtils.contains(stringValue, "${")) {
                    Map<String, String> map = new HashMap<>();
                    map.put("project.groupId", i.getGroupId());
                    map.put("project.artifactId", i.getArtifactId());
                    map.put("project.version", i.getVersion());

                    values = Arrays.asList(StrSubstitutor.replace(stringValue, map));
                }
            }

            metaData.put(m, values);
        });
        i.setMetaData(metaData);

        i.setInheritedMethods(doc.containsKey("inheritedmethodOrigSignature_sigs_exact") ?
                doc.getFieldValues("inheritedmethodOrigSignature_sigs_exact")
                        .stream().map(o -> o.toString()).filter(o -> !StringUtils.startsWith(o,"access$")).collect(Collectors.toList()): Collections.emptyList());

        List<SolrDocument> alternativeList = doc.containsKey("alternatives") ? (SolrDocumentList) doc.get("alternatives") : Collections.emptyList();
        i.setAlternatives(alternativeList.stream().map(MavenCodeUnitUtils::toImplementation).collect(Collectors.toList()));

        List<SolrDocument> cloneList = doc.containsKey("clone") ? (SolrDocumentList) doc.get("clone") : Collections.emptyList();
        i.setClones(cloneList.stream().map(MavenCodeUnitUtils::toImplementation).collect(Collectors.toList()));

        List<SolrDocument> similarList = doc.containsKey("similar") ? (SolrDocumentList) doc.get("similar") : Collections.emptyList();
        i.setSimilar(similarList.stream().map(MavenCodeUnitUtils::toImplementation).collect(Collectors.toList()));

        i.setClassifier((String) doc.get("classifier_s"));

        String methodSignatureParamsOrderedKeywords = i.getUnitType() == CodeUnit.CodeUnitType.METHOD ? "methodSignatureParamsOrderedKeywordsFq_ssig" : "methodSignatureParamsOrderedKeywordsFq_sigs";
        i.setMethodSignatureParamsOrderedKeywordsFq(doc.containsKey(methodSignatureParamsOrderedKeywords) ?
                        doc.getFieldValues(methodSignatureParamsOrderedKeywords)
                    .stream().map(Object::toString).filter(o -> !StringUtils.contains(o,"access$")).collect(Collectors.toList()): Collections.emptyList());

        String methodNames = i.getUnitType() == CodeUnit.CodeUnitType.METHOD ? "method_fq" : "method_fqs";
        i.setMethodNames(doc.containsKey(methodNames) ?
                        doc.getFieldValues(methodNames)
                    .stream().map(Object::toString).filter(o -> !StringUtils.startsWith(o,"access$")).collect(Collectors.toList()) : Collections.emptyList());

        boolean method = i.getUnitType() == CodeUnit.CodeUnitType.METHOD;
        if(method) {
            i.setMethodBytecodeNames(Arrays.asList(i.getBytecodeName()));
        } else {
            String fieldName = "bytecodemethodname_ss";
            i.setMethodBytecodeNames(doc.containsKey(fieldName) ?
                            doc.getFieldValues(fieldName)
                    .stream().map(Object::toString).collect(Collectors.toList()) : Collections.emptyList());
        }

        // set LQL
        try {
            Clazz clazz = SignatureUtils.create(i);

            i.setLql(clazz.toLQL(true, true));
        } catch (Throwable e) {
            LOG.warn("Failed to set LQL for '{}'", i.getId());
            LOG.warn("Stack trace", e);

            i.setLql("n/a");
        }

        return i;
    }
}
