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
package de.uni_mannheim.swt.lasso.datasource.maven.lsl

import de.uni_mannheim.swt.lasso.core.model.System
import de.uni_mannheim.swt.lasso.index.SearchOptions
import de.uni_mannheim.swt.lasso.index.query.lql.old.LQL2LuceneClassQuery
import de.uni_mannheim.swt.lasso.index.query.lql.old.LQL2LuceneClassConceptQuery
import de.uni_mannheim.swt.lasso.index.query.lql.LQL2LuceneClassQueryBareBone
import de.uni_mannheim.swt.lasso.index.query.lql.old.LQL2LuceneClassQueryExtended
import de.uni_mannheim.swt.lasso.index.query.lql.LQLLuceneStrategy
import de.uni_mannheim.swt.lasso.index.query.lql.LQL2LuceneClassQuerySimplified
import de.uni_mannheim.swt.lasso.index.odisse.socora.ranking.Criterion
import de.uni_mannheim.swt.lasso.core.model.CodeUnit
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.Clazz
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.ExactMethodSignatureQuery
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.ExactMethodSignatureQuery2
import de.uni_mannheim.swt.lasso.datasource.expansion.signature.SignatureUtils
import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec
import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.SolrQuery

/**
 * Default query model for Maven-based data sources.
 *
 * @author Marcus Kessel
 */
class MavenQuery extends LassoSpec {

    public static Map<String, Class<? extends LQLLuceneStrategy>> queryStrategies = [:]
    static {
        queryStrategies << ['concept': LQL2LuceneClassConceptQuery.class]
        queryStrategies << ['exact': ExactMethodSignatureQuery.class]
        queryStrategies << ['exact2': ExactMethodSignatureQuery2.class]
        queryStrategies << ['class': LQL2LuceneClassQuery.class]
        queryStrategies << ['class-ext': LQL2LuceneClassQueryExtended.class]

        queryStrategies << ['class-simple': LQL2LuceneClassQuerySimplified.class]
        queryStrategies << ['class-bare': LQL2LuceneClassQueryBareBone.class]
    }

    public static final Map<String, String> FILTER_ALIASES = [:]
    static {
        FILTER_ALIASES << ['complexity': 'm_static_complexity_td']
        FILTER_ALIASES << ['methods': 'm_static_methods_td']
        FILTER_ALIASES << ['branches': 'm_static_branch_td']
    }

    /**
     * Default Constraints
     */
    //public static String[] CONSTRAINTS = ["doctype_s:class", "type:c", /*"!classifier_s:tests",*/ "{!collapse field=hash nullPolicy=expand sort='versionHead_ti asc'}"]
    // XXX disabled collapsing (not needed anymore)
    public static String[] CONSTRAINTS = ["doctype_s:class", "type:c"]

    String query = ''
    String queryType = 'class'

    int rows = 100
    int start = 0

    int expandedRows = 5

    boolean useAlternatives = false

    /**
     * Enable project sampling?
     */
    boolean projectSampling = false

    boolean doCollapseByClass = false

    boolean random = false

    /**
     * Query without cursor functionality
     */
    boolean directly = true

    List<String> constraints = CONSTRAINTS as ArrayList
    SearchOptions options = new SearchOptions()

    String rankingStrategy
    List<Criterion> rankingCriteria

    List<SolrQuery.SortClause> orderByClauses = []

    boolean strictTypeFilter = false

    boolean javaTypeFilter = false

    boolean dropPojo = false

    List<String> allowedInputTypes
    List<String> allowedReturnTypes

    def fullyQualifiedTypes(boolean fullyQualified) {
        options.setFullyQualified(fullyQualified)
    }

    def docType(String type) {
        // replace type
        constraints.removeAll({it.startsWith('doctype_s:')})
        String docType = "doctype_s:${type}"
        constraints << docType
    }

    def excludeClassesByKeywords(List keywords) {
        keywords.each {k ->
            filter "-keyword_ss:\"${k}\""
        }
    }

    /**
     * Exclude any test classes.
     *
     * @return
     */
    def excludeTestClasses() {
        // not from tests classifier
        filter '-classifier_s:"tests"'
        // no JUnit3 tests
        filter '-dep_exact:"junit/framework/TestCase"'
        // no JUnit4 tests
        filter '-dep_exact:"org/junit/Test"'
        // no JUnit5 tests
        filter '-dep_exact:"org/junit/jupiter/api/Test"'
        // no TestNG
        filter '-dep_exact:"org/testng/annotations/Test"'

        // no JEE and internal packages
        filter '-packagename_fq:("java.*" OR "javax.*" OR "com.sun.*" OR "sun.*" OR "org.evosuite.*")'
    }

    /**
     * Exclude exceptions.
     *
     * @return
     */
    def excludeExceptions() {
        filter '-superclass_exact:"java/lang/Exception"'
    }

    /**
     * Exclude internal namespaces
     *
     * @return
     */
    def excludeInternalPkgs() {
        filter '-packagename_fq:("java.*" OR "javax.*" OR "com.sun.*" OR "sun.*" OR "org.evosuite.*")'
    }

    def excludeImplementation(String implId) {
        filter "!id:\"${implId}\""
    }

    def collapseBy(String fieldName) {
        // replace fieldName
        constraints.removeAll({it.startsWith('{!collapse field=')})

        if(StringUtils.isNotBlank(fieldName)) {
            String collapse = "{!collapse field=${fieldName} nullPolicy=expand sort='versionHead_ti asc'}"
            constraints << collapse
        }
    }

    def collapseByClass() {
        this.doCollapseByClass = true
    }

    void query(String queryString, String type) {
        this.query = queryString

        this.queryType = type

        setStrategyClass(type)
    }

    def setStrategyClass(String type) {
        // remember GString != String
        Class<? extends LQLLuceneStrategy> strategyClass = queryStrategies.get(type)

        if(strategyClass == null) {
            throw new IllegalArgumentException("'${type}' unknown")
        }

        options.setStrategy(strategyClass)

        debug("option set ${options.getStrategy()}")
    }

    void queryForClasses(String queryString, String type = 'class') {
        this.query = queryString

        this.queryType = type

        setStrategyClass(type)
    }

    void queryForMethods(String queryString, String type = 'class') {
        this.query = queryString

        //
        docType("method")

        this.queryType = type

        setStrategyClass(type)
    }

    void queryForProjectMethods(String queryString, String type = 'class') {
        // set project sampling true
        this.projectSampling = true

        queryForMethods(queryString, type)
    }

    void queryForProjectClasses(String queryString, String type = 'class') {
        // set project sampling true
        this.projectSampling = true

        queryForClasses(queryString, type)
    }

    void queryByExample(System impl, String type, boolean strict = false) {
        CodeUnit implementation = impl.code
        // METHOD handler
        if(implementation.unitType == CodeUnit.CodeUnitType.METHOD) {
            docType("method")

            // limit to exact number of params
            int paramSize = SignatureUtils.create(implementation)
                    .getMethods().get(0)
                    .inputTypes.size()
            filter("m_paramsize_td:${paramSize}")
        } else {
            // CLASS handler
            docType("class")
        }

        // do not retrieve ref impl
        filter "!id:\"${implementation.id}\""

        // query
        Clazz clazz = SignatureUtils.create(implementation)
        query(clazz.toLQL(true), type)

        debug("queryByExample: strict? ${strict}")

        if(strict) {
            strictTypeFilter = true

            allowedInputTypes = []
            allowedReturnTypes = []
            // add strict filtering
            clazz.methods.each {m ->
                allowedInputTypes.addAll(m.inputTypes)
                allowedReturnTypes.add(m.returnType)
            }

            debug("allowed input types: ${allowedInputTypes}")
            debug("allowed return types: ${allowedReturnTypes}")

            debug("setting filters")

            allowedInputTypes.each {t ->
                filter("methodSignatureParamsOrderedSyntaxFq_ssig:pt_${t}")
            }

            allowedReturnTypes.each {t ->
                filter("methodSignatureParamsOrderedSyntaxFq_ssig:rv_${t}")
            }
        }

//            if(strict) {
//                // add strict filtering
//                Clazz clazz = mavenImplementation.toClazz()
//                Signature mSig = clazz.getMethods()[0]
//                String params = mSig.inputTypes.collect {p -> "pt_${p}"}.join(';')
//                // limit to return type and parameter types
//                filter("methodSignatureParamsOrderedSyntaxFq_ssig:rv_${mSig.returnType}")
//                filter("methodSignatureParamsOrderedSyntaxFq_ssig:${params}")
//            }
    }

    def orderBy(String field, String order) {
        SolrQuery.SortClause clause = field == "asc" ? SolrQuery.SortClause.asc(field) : SolrQuery.SortClause.desc(field)

        //System.out.println("SORT ${field} == ${order}")

        orderByClauses << clause
    }

    /**
     * Add constraints
     *
     * @param map
     */
    def constraints(map) {
        // add constraints
        map.each{k, v -> constraints << "${k}:${v}".toString()}
    }

    /**
     * Add Solr query filter
     *
     * @param filter
     */
    def rawConstraint(filter) {
        constraints << filter.toString()
    }

    def filter(filter) {
        String[] parts = StringUtils.split(filter, ":");
        if(FILTER_ALIASES.containsKey(parts[0])) {
            filter = FILTER_ALIASES.get(parts[0]) + ":" + parts[1]

            println("Rewrote filter using alias to '${filter}'")
        }

        constraints << filter.toString()
    }

    /**
     * Ranking criteria
     *
     * @param strategy
     * @param criteria
     * @return
     */
    def rankingCriteria(strategy, criteria) {
        // add ranking criteria
        rankingStrategy = strategy
        rankingCriteria = criteria
    }

    /**
     * Ranking criteria
     *
     * @param criteria
     * @return
     */
    def rankingCriteria(criteria) {
        // add ranking criteria
        rankingCriteria('HDS_SMOOP', criteria)
    }

    def expandMethodNames(int topN) {
        options.expandMethodNames = topN
    }

    /**
     * Add new ranking criterion.
     *
     * @param name
     * @param objective
     * @param priority
     * @return
     */
    Criterion rankingCriterion(String name, double objective, double priority) {
        Criterion criterion = new Criterion(id:name, objective: objective, priority: priority)
        return criterion
    }
}
