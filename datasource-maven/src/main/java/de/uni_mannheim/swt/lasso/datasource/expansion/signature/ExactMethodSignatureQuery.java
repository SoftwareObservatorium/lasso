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
package de.uni_mannheim.swt.lasso.datasource.expansion.signature;

import de.uni_mannheim.swt.lasso.core.model.MethodSignature;
import de.uni_mannheim.swt.lasso.index.SearchOptions;
import de.uni_mannheim.swt.lasso.index.query.lql.LQLLuceneStrategy;

import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author Marcus Kessel
 */
public class ExactMethodSignatureQuery implements LQLLuceneStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(ExactMethodSignatureQuery.class);

    protected LQLParseResult lqlParseResult;
    protected String keywordsAndConstraints;

    private final SearchOptions searchOptions;

    @Override
    public boolean isFullyQualified() {
        return fullyQualified;
    }

    @Override
    public void setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
    }

    private boolean fullyQualified;

    public ExactMethodSignatureQuery(SearchOptions searchOptions) {
        //
        this.searchOptions = searchOptions;

        setKeywordsAndConstraints(searchOptions.getKeywordsAndConstraints());
    }

    /**
     * @return Lucene {@link Query}
     * @throws ParseException
     */
    @Override
    public Query getLuceneQuery() throws IOException {
        if (isLQL()) {
            return translateMqlToLuceneQuery();
        } else {
            throw new UnsupportedOperationException("Non-MQL queries are not supported by this strategy");
        }
    }

    /**
     * @return true if MQL (interface signature query), false otherwise
     */
    @Override
    public boolean isLQL() {
        return lqlParseResult.getInterfaceSpecification() != null;
    }

    /**
     * @return true if no query at all
     */
    @Override
    public boolean isEmpty() {
        return !isLQL() && StringUtils.isBlank(keywordsAndConstraints);
    }

    /**
     * @return MQL interface signatures transformed to Lucene {@link Query}
     */
    protected Query translateMqlToLuceneQuery() {
        // MQL
        // currently, limited to first query
        LinkedList<Query> queries = new LinkedList<>();

        //for (SourceClass mClass : mqlQueryComponent.getClasses()) {
        //for (SourceClass mClass : mqlQueryComponent.getClasses()) {
        Interface system = lqlParseResult.getInterfaceSpecification();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> mSignatures = new LinkedList<>();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> cSignatures = new LinkedList<>();
        String className = system.getName();

        // construct ordered signatures for searching
        List<MethodSignature> methods = system.getMethods();
        if (methods != null && methods.size() > 0) {
            //mSignatures = new MethodSignature[methods.size()];
            for (int i = 0; i < methods.size(); i++) {
                MethodSignature method = methods.get(i);
                if(!method.isConstructor()) {
                    mSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", method.getName(),
                            method.getInputs(), method.getOutputs().get(0)));
                } else {
                    cSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", "<init>",
                            method.getInputs(), "void"));
                }
            }
        }

            LOG.info("constraints " + keywordsAndConstraints);

            Query query = createComponentQuery(className, keywordsAndConstraints, cSignatures.toArray(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature[0]), mSignatures.toArray(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature[0]));

            queries.add(query);

            //LOG.info("Adding query "+ query.toString());
        //}

        return queries.getFirst();
    }

    /**
     * Creates an 'OR' like component query to support the concept of
     * 'Functional Sufficiency'.
     *
     * @param className             Subject class name
     * @param constraints           Lucene constraints
     * @param constructorSignatures Constructor signatures
     * @param methodSignatures      Method signatures
     * @return Lucene component {@link Query}
     */
    @Override
    public Query createComponentQuery(String className, String constraints, de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature[] constructorSignatures,
                                      de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature... methodSignatures) {

        if (methodSignatures.length > 1) {
            throw new UnsupportedOperationException("Only one method signature supported right now");
        }

        de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature signature = methodSignatures[0];

//        PermutationIterator<String> permutationIterator = new PermutationIterator<>(signature.getParameterTypes());
//
//        List<MethodSignature> signatures = new LinkedList<>();
//        permutationIterator.forEachRemaining(l -> {
//            MethodSignature sig = new MethodSignature("public", signature.getMethodName(),
//                    l, signature.getReturnValue());
//
//            signatures.add(sig);
//        });

        // OR query
        BooleanQuery.Builder query = new BooleanQuery.Builder();//.setMinimumNumberShouldMatch(1); // ?

        // add boost to naming?
        //query.add(new BoostQuery(new TermQuery(new Term("method_fq", signature.getMethodName())), 2f), Occur.SHOULD);
        query.add(new TermQuery(new Term("method_fq", signature.getMethodName())), Occur.SHOULD);

        // TODO add name concept or something?

//        signatures.stream().map(s -> {
//            StringBuilder sb = new StringBuilder();
//            sb.append("rv_")
//                    .append(s.getReturnValue().toLowerCase());
//
//            s.getParameterTypes().stream().forEach(p -> {
//                sb.append(";")
//                        .append("pt_")
//                        .append(p.toLowerCase());
//            });
//
//            return new TermQuery(new Term("methodSignatureParamsOrderedSyntaxFq_ssigs_sexact", sb.toString()));
//        }).forEach(t -> {
//            query.add(t, Occur.SHOULD);
//        });

        // ordered! (so no perms necessary!)
        List<String> orderedParams = new ArrayList<>(signature.getParameterTypes());
        signature.sortParameters(orderedParams);

        StringBuilder sb = new StringBuilder();
        sb.append("rv_")
                .append(signature.getReturnValue().toLowerCase());

        orderedParams.stream().forEach(p -> {
            sb.append(";")
                    .append("pt_")
                    .append(p.toLowerCase());
        });

        // MUST occur
        query.add(new TermQuery(new Term("methodSignatureParamsOrderedSyntaxFq_ssigs_sexact", sb.toString())), Occur.MUST);

        // add constraints at the end
        QueryParser qp = new QueryParser("content", new SimpleAnalyzer());
        qp.setDefaultOperator(QueryParser.AND_OPERATOR);
        try {
            // add constraints
            if (constraints != null && constraints.length() > 2) {
                query.add(qp.parse(constraints), Occur.MUST);
            }

            LOG.info("constraints added: " + constraints);
        } catch (ParseException e) {
            LOG.warn(e.getMessage(), e);
        }

        return query.build();
    }

    /**
     * @return the keywordsAndConstraints
     */
    @Override
    public String getKeywordsAndConstraints() {
        return keywordsAndConstraints;
    }

    /**
     * @param keywordsAndConstraints the keywordsAndConstraints to set
     */
    @Override
    public void setKeywordsAndConstraints(String keywordsAndConstraints) {
        if (StringUtils.isBlank(keywordsAndConstraints)) {
            return;
        }

        this.keywordsAndConstraints = keywordsAndConstraints;
    }

    @Override
    public LQLParseResult getLQLParseResult() {
        return lqlParseResult;
    }

    @Override
    public void setLQLParseResult(LQLParseResult lqlParseResult) {
        this.lqlParseResult = lqlParseResult;
    }
}
