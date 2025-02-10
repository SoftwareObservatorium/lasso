package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import com.google.common.collect.Table;
import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.compile.TestSupport;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.JUnitSequenceSpecificationParser;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter.PassThroughAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.event.CompositeInvocationVisitor;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.TestInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.arena.task.SSNExecute;
import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.core.dto.srm.Sheet;
import de.uni_mannheim.swt.lasso.core.dto.srm.SheetInvocation;
import de.uni_mannheim.swt.lasso.core.dto.srm.StimulusResponseMatrix;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class JUnit2SSNTest {

    private static final Logger LOG = LoggerFactory.getLogger(JUnit2SSNTest.class);

    public MavenRepository mavenRepository() {
        // FIXME change maven repo
        String mavenRepoUrl = NexusInstance.MAVEN_CENTRAL;
        File localRepo = new File("/tmp/my_repo/local-repo");
        DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
        return new MavenRepository(resolver);
    }

    @Test
    public void testToSequenceSpecification_ArrayStack__PSEUDO() throws IOException {
        String lql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        // CREATE PSEUDO CUT
        ClassUnderTest pseudo = TestSupport.createPseudoImplementation("Stack");

        CandidatePool pool = new CandidatePool(mavenRepository(), Arrays.asList(pseudo));
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/PseudoStack.java"), StandardCharsets.UTF_8);

        List<InterfaceSpecification> parseResults = LQLUtils.lqlToList(lql);
        InterfaceSpecification specification = parseResults.get(0);

//        AdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();
//        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(specification, pseudo, 1);
//        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClass, pseudo, specification, null, "evo");

        for(Sheet stimulusSheet : stimulusSheets) {
            System.out.println(stimulusSheet.getSignature());
            System.out.println(stimulusSheet.getBody());
            System.out.println("----");
        }
    }

    @Test
    public void testToSequenceSpecification_ArrayStack__javautilStack() throws IOException {
        String lql = "Stack {\n" +
                "push(java.lang.Object)->java.lang.Object\n" +
                "pop()->java.lang.Object\n" +
                "peek()->java.lang.Object\n" +
                "size()->int\n" +
                "}";

        // CREATE CUT
        ClassUnderTest pseudo = CutUtils.createExample(Stack.class);

        CandidatePool pool = new CandidatePool(mavenRepository(), Arrays.asList(pseudo));
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/PseudoStack.java"), StandardCharsets.UTF_8);

        List<InterfaceSpecification> parseResults = LQLUtils.lqlToList(lql);
        InterfaceSpecification specification = parseResults.get(0);

        AdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(specification, pseudo, 1);
        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClass, pseudo, specification, adaptedImplementation, "evo");

        for(Sheet stimulusSheet : stimulusSheets) {
            System.out.println(stimulusSheet.getSignature());
            System.out.println(stimulusSheet.getBody());
            System.out.println("----");
        }

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();
        InterfaceSpecification parsedSpecification = importJUnitClass.toSpecification(testClass, pseudo).get(pseudo.getClassName());
        System.out.println(parsedSpecification.toLQL());
    }

    @Test
    public void testToSequenceSpecification_Base64() throws IOException {
        String lql = """
        Base64 {
            encode(byte[])->byte[]
            decode(java.lang.String)->byte[]
        }""";

        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.resolve("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        CandidatePool pool = new CandidatePool(mavenRepository(), Arrays.asList(classUnderTest));
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/jsonl/Curated_Base64_0_Test.java"), StandardCharsets.UTF_8);

        List<InterfaceSpecification> parseResults = LQLUtils.lqlToList(lql);
        InterfaceSpecification specification = parseResults.get(0);

        AdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(specification, classUnderTest, 1);
        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClass, classUnderTest, specification, adaptedImplementation, "evo");

        for(Sheet stimulusSheet : stimulusSheets) {
            System.out.println(stimulusSheet.getSignature());
            System.out.println(stimulusSheet.getBody());
            System.out.println("----");
        }

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();
        InterfaceSpecification parsedSpecification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());
        System.out.println(parsedSpecification.toLQL());
    }

    @Test
    public void testToSequenceSpecification_Base64_all() throws IOException {
        String lql = """
        Base64 {
            encode(byte[])->byte[]
            decode(java.lang.String)->byte[]
        }""";

        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.resolve("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");

        CandidatePool pool = new CandidatePool(mavenRepository(), Arrays.asList(classUnderTest));
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/jsonl/Base64_0_Test.java"), StandardCharsets.UTF_8);

        List<InterfaceSpecification> parseResults = LQLUtils.lqlToList(lql);
        InterfaceSpecification specification = parseResults.get(0);

        AdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();
        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(specification, classUnderTest, 1);
        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClass, classUnderTest, specification, adaptedImplementation, "evo");

        for(Sheet stimulusSheet : stimulusSheets) {
            System.out.println(stimulusSheet.getSignature());
            System.out.println(stimulusSheet.getBody());
            System.out.println("----");
        }

        JUnitSequenceSpecificationParser importJUnitClass = new JUnitSequenceSpecificationParser();
        InterfaceSpecification parsedSpecification = importJUnitClass.toSpecification(testClass, classUnderTest).get(classUnderTest.getClassName());
        System.out.println(parsedSpecification.toLQL());
    }

    @Test
    public void testToSequenceSpecification_Base64_run() throws IOException {
        String lql = """
        Base64 {
            encodeBase64String(byte[])->java.lang.String
        }""";

        // commons-codec:commons-codec:1.15
        ClassUnderTest classUnderTest = CutUtils.resolve("org.apache.commons.codec.binary.Base64", "commons-codec:commons-codec:1.15");
        classUnderTest.getImplementation().getCode().setId("a2bb2beb-0e4a-492e-93ca-1b0bc35a0012");

        CandidatePool pool = new CandidatePool(mavenRepository(), Arrays.asList(classUnderTest));
        pool.initProjects();

        // generated tests
        File path = new File("sheets/evoSuite_79b401d1-4fec-43db-aa2d-a3f976fb1976/");

        CompositeInvocationVisitor visitor = new CompositeInvocationVisitor(Arrays.asList());

        SSNTestDriver testDriver = new SSNTestDriver();
        testDriver.setAdaptationStrategy(new PassThroughAdaptationStrategy());

        ArenaJob arenaJob = new ArenaJob();
        arenaJob.setSpecification(lql);

        List<Sheet> translatedSheets = SSNExecute.resolveSheetsFromJUnit(testDriver, Arrays.asList(classUnderTest.getImplementation()), path, arenaJob);
        List<SheetInvocation> sheetInvocations = translatedSheets.stream().map(s -> new SheetInvocation(StringUtils.substringBefore(s.getSignature(), "("), "")).toList();

        // SM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, ClassUnderTest, TestInvocation> stimulusMatrix = SSNTestDriver.parseStimulusMatrix(
                translatedSheets, Arrays.asList(classUnderTest), sheetInvocations);

        // SRM
        StimulusResponseMatrix<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> stimulusResponseMatrix = testDriver.runSheets(stimulusMatrix, 1, visitor);

        for(Table.Cell<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.model.Test, AdaptedImplementation, ExecutedInvocations> cell : stimulusResponseMatrix.getTable().cellSet()) {
            ExecutedInvocations executedInvocations = cell.getValue();

            LOG.debug("executed invocations for '{}' \n{}", cell.getColumnKey().getAdaptee().getVariantId(), executedInvocations);
            LOG.debug("signature {}", executedInvocations.getInvocations().getTest().getSignature().toLQL());
            List<de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String>> sheets = TestUtils.createSheets(cell.getColumnKey(), executedInvocations);
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> actuationSheetData = sheets.get(0);
            actuationSheetData.debug();
            de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Sheet<Integer, Integer, String> adaptedActuationSheetData = sheets.get(1);
            adaptedActuationSheetData.debug();

            Invocations invocations = executedInvocations.getInvocations();
        }
    }

    @Test
    public void testToSequenceSpecification_GCD__PSEUDO() throws IOException {
        String lql = "Problem {\n" +
                "greatestCommonDivisor(int,int)->int\n" +
                "}";

        // CREATE PSEUDO CUT
        //ClassUnderTest pseudo = TestSupport.createPseudoImplementation("GCD");
        ClassUnderTest pseudo = CutUtils.createExample("GCD");

        CandidatePool pool = new CandidatePool(mavenRepository(), Arrays.asList(pseudo));
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/PseudoGCD_GAI.java"), StandardCharsets.UTF_8);

        List<InterfaceSpecification> parseResults = LQLUtils.lqlToList(lql);
        InterfaceSpecification specification = parseResults.get(0);

//        AdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();
//        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(specification, pseudo, 1);
//        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClass, pseudo, specification, null, "evo");

        for(Sheet stimulusSheet : stimulusSheets) {
            System.out.println(stimulusSheet.getSignature());
            System.out.println(stimulusSheet.getBody());
            System.out.println("----");
        }
    }

    @Test
    public void testToSequenceSpecification_GCD__PSEUDO_2() throws IOException {
        String lql = "Problem {\n" +
                "greatestCommonDivisor(int,int)->int\n" +
                "}";

        // CREATE PSEUDO CUT
        //ClassUnderTest pseudo = TestSupport.createPseudoImplementation("GCD");
        ClassUnderTest pseudo = CutUtils.createExample("GCD");

        CandidatePool pool = new CandidatePool(mavenRepository(), Arrays.asList(pseudo));
        pool.initProjects();

        String testClass = FileUtils.readFileToString(new File("sheets/PseudoGCD_GAI_2.java"), StandardCharsets.UTF_8);

        List<InterfaceSpecification> parseResults = LQLUtils.lqlToList(lql);
        InterfaceSpecification specification = parseResults.get(0);

//        AdaptationStrategy adaptationStrategy = new PassThroughAdaptationStrategy();
//        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(specification, pseudo, 1);
//        AdaptedImplementation adaptedImplementation = adaptedImplementations.get(0);

        List<Sheet> stimulusSheets = JUnit2SSN.junit2Sheets(testClass, pseudo, specification, null, "evo");

        for(Sheet stimulusSheet : stimulusSheets) {
            System.out.println(stimulusSheet.getSignature());
            System.out.println(stimulusSheet.getBody());
            System.out.println("----");
        }
    }
}
