package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.read;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.CodeSearch;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.SolrInstance;
import de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.*;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.sheet.SpreadSheet;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 *
 *
 * @author Marcus Kessel
 */
public class SequenceSheetParserTest {

    private static final Logger LOG = LoggerFactory.getLogger(SequenceSheetParserTest.class);

    String mavenRepoUrl = NexusInstance.LOCAL_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);
    CodeSearch codeSearch = new CodeSearch(SolrInstance.local());

    @Test
    public void testToInterfaceSpecifcation() throws DuplicateRealmException, IOException {
        SpreadSheet sheet1 = new SpreadSheet(
                new File("/home/marcus/development/repositories/github/lasso/arena/sheets/evolution/stack.xlsx"));

        SequenceSheetParser parser = new SequenceSheetParser();

        InterfaceSpecification interfaceSpecification = parser.toInterfaceSpecification(sheet1.getFirstSheet());

        LOG.debug(interfaceSpecification.toLQL());

        Assertions.assertEquals("Stack", interfaceSpecification.getClassName());
        Assertions.assertEquals(1, interfaceSpecification.getConstructors().size());
        Assertions.assertEquals(3, interfaceSpecification.getMethods().size());
    }

    @Test
    public void testToTestSpec_Stack() throws DuplicateRealmException, IOException {
        SpreadSheet sheet1 = new SpreadSheet(
                new File("/home/marcus/development/repositories/github/lasso/arena/sheets/evolution/stack.xlsx"));

        ClassUnderTest classUnderTest = GroovySheetEngineIntegrationTest.createExample(Stack.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        SequenceSheetParser parser = new SequenceSheetParser();

        // CUT
        TestSpec testSpec = parser.toTestSpec(sheet1.getFirstSheet(), classUnderTest);

        InterfaceSpecification interfaceSpecification = testSpec.getInterfaces().get("Stack");
        LOG.debug(testSpec.getInterfaces().get("Stack").toLQL());

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        int limitAdapters = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);

        AdaptedImplementation first = adaptedImplementations.get(0);
        System.out.println(ToStringBuilder.reflectionToString(first));

        //
        GroovySheetEngine groovySheetEngine = new GroovySheetEngine(classUnderTest.getProject().getContainer());

        ExecutedSequence executedSequence = groovySheetEngine.run(testSpec, first);

        debug(executedSequence);

        // all statements including inlined ones
        Assertions.assertEquals(5, executedSequence.getNoOfExecutedStatements());
        Assertions.assertEquals("[]", executedSequence.getStatement(0).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", executedSequence.getStatement(1).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", executedSequence.getStatement(2).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", executedSequence.getStatement(3).getSerializedOutput());
        Assertions.assertEquals("1", executedSequence.getStatement(4).getSerializedOutput());

        // sheet rows
        List<ExecutedStatement> rowStatements = executedSequence.getRowStatements();
        Assertions.assertEquals(4, rowStatements.size());
        Assertions.assertEquals("[]", rowStatements.get(0).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", rowStatements.get(1).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", rowStatements.get(2).getSerializedOutput());
        Assertions.assertEquals("1", rowStatements.get(3).getSerializedOutput());
    }

    @Test
    public void testToTestSpec_List_non_empty_constructor() throws DuplicateRealmException, IOException {
        SpreadSheet sheet1 = new SpreadSheet(
                new File("/home/marcus/development/repositories/github/lasso/arena/sheets/evolution/list_non_empty_constructor.xlsx"));

        ClassUnderTest classUnderTest = GroovySheetEngineIntegrationTest.createExample(ArrayList.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        SequenceSheetParser parser = new SequenceSheetParser();

        // CUT
        TestSpec testSpec = parser.toTestSpec(sheet1.getFirstSheet(), classUnderTest);

        InterfaceSpecification interfaceSpecification = testSpec.getInterfaces().get("List");
        LOG.debug(testSpec.getInterfaces().get("List").toLQL());

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        int limitAdapters = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);

        AdaptedImplementation first = adaptedImplementations.get(0);
        System.out.println(ToStringBuilder.reflectionToString(first));

        //
        GroovySheetEngine groovySheetEngine = new GroovySheetEngine(classUnderTest.getProject().getContainer());

        ExecutedSequence executedSequence = groovySheetEngine.run(testSpec, first);

        debug(executedSequence);

        Assertions.assertEquals(5, executedSequence.getNoOfExecutedStatements());
        Assertions.assertEquals("1", executedSequence.getStatement(0).getSerializedOutput());
        Assertions.assertEquals("[]", executedSequence.getStatement(1).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", executedSequence.getStatement(2).getSerializedOutput());
        Assertions.assertEquals("true", executedSequence.getStatement(3).getSerializedOutput());
        Assertions.assertEquals("1", executedSequence.getStatement(4).getSerializedOutput());

        // sheet rows
        List<ExecutedStatement> rowStatements = executedSequence.getRowStatements();
        Assertions.assertEquals(3, rowStatements.size());
        Assertions.assertEquals("[]", rowStatements.get(0).getSerializedOutput());
        Assertions.assertEquals("true", rowStatements.get(1).getSerializedOutput());
        Assertions.assertEquals("1", rowStatements.get(2).getSerializedOutput());
    }

    @Test
    public void testToTestSpec_Stack_complex() throws DuplicateRealmException, IOException {
        SpreadSheet sheet1 = new SpreadSheet(
                new File("/home/marcus/development/repositories/github/lasso/arena/sheets/evolution/stack_complex.xlsx"));

        ClassUnderTest classUnderTest = GroovySheetEngineIntegrationTest.createExample(Stack.class);
        CandidatePool pool = new CandidatePool(new MavenRepository(resolver), Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        SequenceSheetParser parser = new SequenceSheetParser();

        // CUT
        TestSpec testSpec = parser.toTestSpec(sheet1.getFirstSheet(), classUnderTest);

        InterfaceSpecification interfaceSpecification = testSpec.getInterfaces().get("Stack");
        LOG.debug(testSpec.getInterfaces().get("Stack").toLQL());

        DefaultAdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();
        int limitAdapters = 1;
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification, classUnderTest, limitAdapters);

        AdaptedImplementation first = adaptedImplementations.get(0);
        System.out.println(ToStringBuilder.reflectionToString(first));

        //
        GroovySheetEngine groovySheetEngine = new GroovySheetEngine(classUnderTest.getProject().getContainer());

        ExecutedSequence executedSequence = groovySheetEngine.run(testSpec, first);

        debug(executedSequence);

        // all statements including inlined ones
        Assertions.assertEquals(5, executedSequence.getNoOfExecutedStatements());
        Assertions.assertEquals("[]", executedSequence.getStatement(0).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", executedSequence.getStatement(1).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", executedSequence.getStatement(2).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", executedSequence.getStatement(3).getSerializedOutput());
        Assertions.assertEquals("1", executedSequence.getStatement(4).getSerializedOutput());

        // sheet rows
        List<ExecutedStatement> rowStatements = executedSequence.getRowStatements();
        Assertions.assertEquals(4, rowStatements.size());
        Assertions.assertEquals("[]", rowStatements.get(0).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", rowStatements.get(1).getSerializedOutput());
        Assertions.assertEquals("\"hi!\"", rowStatements.get(2).getSerializedOutput());
        Assertions.assertEquals("1", rowStatements.get(3).getSerializedOutput());
    }

    static void debug(ExecutedSequence executedSequence) {
        for(ExecutedStatement executedStatement : executedSequence.getStatements()) {
            LOG.info(ToStringBuilder.reflectionToString(executedStatement, ToStringStyle.JSON_STYLE));
        }
    }
}
