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
package de.uni_mannheim.swt.lasso.index.repo;

import de.uni_mannheim.swt.lasso.index.match.SignatureMatch;
import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This realizes lazy loading of document values.
 *
 * @author Marcus Kessel
 */
public class SolrCandidateDocument implements CandidateDocument {

    private final SolrDocument doc;

    private Map<String, Double> metrics;

    private List<CandidateDocument> alternatives;
    private List<CandidateDocument> clones;
    private List<CandidateDocument> similar;

    public SolrCandidateDocument(SolrDocument doc) {
        this.doc = doc;
    }

    public SolrDocument getSolrDocument() {
        return doc;
    }

    @Override
    public Map<String, Object> getValues() {
        return doc;
    }

    @Override
    public String getId() {
        return (String) doc.getFirstValue("id");
    }

    @Override
    public String getParentId() {
        return (String) doc.getFirstValue("pid_s");
    }

    @Override
    public String getName() {
        return (String) doc.getFirstValue("name_sexact");
    }

    @Override
    public String getPackagename() {
        return (String) doc.getFirstValue("packagename_sexact");
    }

    @Override
    public String getBytecodeName() {
        return (String) doc.getFirstValue("bytecodename_s");
    }

    @Override
    public String getGroupId() {
        return (String) doc.getFirstValue("groupId");
    }

    @Override
    public String getArtifactId() {
        return (String) doc.getFirstValue("artifactId");
    }

    @Override
    public String getVersion() {
        return (String) doc.getFirstValue("version");
    }

    @Override
    public double getScore() {
        return (float) doc.getFieldValue("score");
    }

    @Override
    public String getContent() {
        return (String) doc.getFieldValue("content");
    }

    @Override
    public String getHash() {
        return (String) doc.getFieldValue("hash");
    }

    @Override
    public String getDocType() {
        return (String) doc.getFieldValue("doctype_s");
    }

    @Override
    public String getType() {
        return (String) doc.getFieldValue("type");
    }

    @Override
    public String getMethodSignature() {
        return getMethods().stream().map(o -> o.toString()).collect(Collectors.joining("|"));
    }

    @Override
    public List<String> getMethods() {
        if(doc.containsKey("methodOrigSignature_sigs_exact")) {
            return doc.getFieldValues("methodOrigSignature_sigs_exact")
                    .stream().map(o -> o.toString()).filter(o -> !StringUtils.startsWith(o,"access$")).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> getSuperClasses() {
        if(doc.containsKey("superclass_exact")) {
            List<String> list = doc.getFieldValues("superclass_exact").stream().map(s -> StringUtils.replace((String) s, "/", ".")).collect(Collectors.toList());

            return list;
        }

        return null;
    }

    @Override
    public List<String> getInterfaces() {
        // interfaces
        if (doc.containsKey("interface_exact")) {
            List<String> list = doc.getFieldValues("interface_exact").stream().map(s -> StringUtils.replace((String) s, "/", ".")).collect(Collectors.toList());

            return list;
        }

        return null;
    }

    @Override
    public List<String> getDependencies() {
        if (doc.containsKey("dep_exact")) {
            List<String> list = doc.getFieldValues("dep_exact").stream().map(s -> StringUtils.replace((String) s, "/", ".")).collect(Collectors.toList());

            return list;
        }

        return null;
    }

    @Override
    public Map<String, Double> getMetrics() {
        if(metrics != null) {
            return metrics;
        }
        
        // add default metric(s) if necessary
        metrics = new TreeMap<String, Double>();
        doc.keySet().stream().filter(k -> StringUtils.startsWith(k, "m_")).forEach(m -> {
            metrics.put(m, (double) doc.getFirstValue(m));
        });
        // add
        metrics.put("m_luceneScore", (double) getScore());

        return metrics;
    }

    @Override
    public Map<String, Object> getMetaData() {
        // add metadata
        Map<String, Object> metaData = new HashMap<>();

        doc.keySet().stream().filter(k -> StringUtils.startsWith(k, "meta_")).forEach(m -> {
            metaData.put(m, doc.getFieldValues(m));
        });

        return metaData;
    }

    @Override
    public String getInheritedMethodSignature() {
        return getInheritedMethods().stream().map(o -> o.toString()).collect(Collectors.joining("|"));
    }

    @Override
    public List<String> getInheritedMethods() {
        if(doc.containsKey("inheritedmethodOrigSignature_sigs_exact")) {
            return doc.getFieldValues("inheritedmethodOrigSignature_sigs_exact")
                    .stream().map(o -> o.toString()).filter(o -> !StringUtils.startsWith(o,"access$")).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    @Override
    public List<SignatureMatch> getMethodSignatureMatches() {
        return null;
    }

    @Override
    public List<SignatureMatch> getConstructorSignatureMatches() {
        return null;
    }

    @Override
    public List<CandidateDocument> getAlternatives() {
        if(alternatives != null) {
            return alternatives;
        }

        if(!doc.containsKey("alternatives")) {
            alternatives = Collections.emptyList();
        } else {
            // populate
            SolrDocumentList alternativeList = (SolrDocumentList) doc.get("alternatives");
            alternatives = alternativeList.stream().map(a -> new SolrCandidateDocument(a)).collect(Collectors.toList());
        }

        return alternatives;
    }

    @Override
    public List<CandidateDocument> getClones() {
        if(clones != null) {
            return clones;
        }

        if(!doc.containsKey("clone")) {
            clones = Collections.emptyList();
        } else {
            // populate
            SolrDocumentList cloneList = (SolrDocumentList) doc.get("clone");
            clones = cloneList.stream().map(a -> new SolrCandidateDocument(a)).collect(Collectors.toList());
        }

        return clones;
    }

    @Override
    public List<CandidateDocument> getSimilar() {
        if(similar != null) {
            return similar;
        }

        if(!doc.containsKey("similar")) {
            similar = Collections.emptyList();
        } else {
            // populate
            SolrDocumentList similarList = (SolrDocumentList) doc.get("similar");
            similar = similarList.stream().map(a -> new SolrCandidateDocument(a)).collect(Collectors.toList());
        }

        return similar;
    }
}
