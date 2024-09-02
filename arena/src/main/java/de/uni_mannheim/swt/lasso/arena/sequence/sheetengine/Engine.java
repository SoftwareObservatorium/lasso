package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.search.LQLMethodSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.ConstructorCallStatement;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.op.InvocableOperation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedCell;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedRow;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet.ActuationSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet.AdapterSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.sheet.StimulusSheet;
import de.uni_mannheim.swt.lasso.core.model.Interface;
import de.uni_mannheim.swt.lasso.core.model.MethodSignature;
import de.uni_mannheim.swt.lasso.lql.parser.LQL;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 *
 * @author Marcus Kessel
 */
public class Engine {

    private static final Logger LOG = LoggerFactory
            .getLogger(Engine.class);

    /**
     * Stage 1: create a {@link StimulusSheet}
     *
     * @param lql
     * @param ssnJsonlStr
     * @return
     * @throws IOException
     */
    public StimulusSheet createStimulusSheetFromLQL(String lql, String ssnJsonlStr) throws IOException {
        // parse LQL interface specification
        Map<String, InterfaceSpecification> interfaceSpecificationMap = lqlToMap(lql);

        // parse sheet in SSN
        ParsedSheet parsedSheet = parseSheet(ssnJsonlStr);

        // stimulus sheet
        StimulusSheet stimulusSheet = new StimulusSheet(interfaceSpecificationMap, parsedSheet);

        return stimulusSheet;
    }

    // FIXME
    public StimulusSheet createStimulusSheetFromJava(String lql) throws IOException {
        throw new UnsupportedOperationException("not implemented");
    }

    /**
     * Stage 2:
     *
     * @param stimulusSheet
     * @param classUnderTest
     * @param adaptationStrategy
     * @param limitAdapters
     * @return
     */
    public List<AdaptedImplementation> createAdapters(StimulusSheet stimulusSheet, ClassUnderTest classUnderTest, AdaptationStrategy adaptationStrategy, int limitAdapters) {
        // FIXME identify (resolve) correct interface specification
        InterfaceSpecification interfaceSpecification = stimulusSheet.getInterfaceSpecifications().entrySet().stream().findFirst().get().getValue();

        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecification,
                classUnderTest, limitAdapters);

        return adaptedImplementations;
    }

    /**
     * Stage 3: Resolve adapted sheets for execution.
     *
     * @param stimulusSheet
     * @param adaptedImplementations
     * @return
     */
    public List<AdapterSheet> resolveSheets(StimulusSheet stimulusSheet, List<AdaptedImplementation> adaptedImplementations) {
        //
        List<AdapterSheet> adapterSheets = adaptedImplementations.stream()
                .map(adaptedImplementation -> resolve(stimulusSheet, adaptedImplementation))
                .collect(Collectors.toList());

        return adapterSheets;
    }

    /**
     * Stage 4: Execute adapted sheets on {@link AdaptedImplementation}'s.
     *
     * @param adapterSheets
     * @param executionListener
     * @return
     */
    public List<ActuationSheet> execute(List<AdapterSheet> adapterSheets, ExecutionListener executionListener) {
        List<ActuationSheet> actuationSheets = new LinkedList<>();
        for(AdapterSheet adapterSheet : adapterSheets) {
            ActuationSheet actuationSheet = execute(adapterSheet, executionListener);

            actuationSheets.add(actuationSheet);
        }

        return actuationSheets;
    }

    /**
     * Stage 3: Execute stimulus sheet on an {@link AdaptedImplementation}.
     *
     * @param adapterSheet
     * @param executionListener
     * @return
     */
    public ActuationSheet execute(AdapterSheet adapterSheet, ExecutionListener executionListener) {
        // FIXME execute

        return null;
    }

    public Map<String, InterfaceSpecification> lqlToMap(String lql) throws IOException {
        List<InterfaceSpecification> interfaceSpecifications = lqlToList(lql);

        return interfaceSpecifications.stream()
                .collect(Collectors.toMap(InterfaceSpecification::getClassName, v -> v));
    }

    public List<InterfaceSpecification> lqlToList(String lql) throws IOException {
        // LQL
        LQLParseResult lqlParseResult = parseLQL(lql);

        List<InterfaceSpecification> parseResults = new LinkedList<>();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> mSignatures = new LinkedList<>();
        List<de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature> cSignatures = new LinkedList<>();

        Interface system = lqlParseResult.getInterfaceSpecification();
        String className = system.getName();

        // construct ordered signatures for searching
        List<MethodSignature> methods = system.getMethods();
        if (methods != null && methods.size() > 0) {
            for (int i = 0; i < methods.size(); i++) {
                MethodSignature method = methods.get(i);
                if (!method.isConstructor()) {
                    mSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", method.getName(),
                            method.getInputs(), method.getOutputs().get(0)));
                } else {
                    cSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", "<init>",
                            method.getInputs(), "void"));
                }
            }
        }

        if (CollectionUtils.isEmpty(cSignatures)) {
            // add default one
            cSignatures.add(new de.uni_mannheim.swt.lasso.index.query.lql.MethodSignature("public", "<init>",
                    Collections.emptyList(), "void"));
        }

        InterfaceSpecification parseResult = new InterfaceSpecification();
        parseResult.setClassName(className);
        parseResult.setConstructors(cSignatures.stream()
                .map(c -> {
                    LQLMethodSignature lm = new LQLMethodSignature(parseResult, c);
                    //lm.setParent(parseResult);
                    return lm;
                })
                .collect(Collectors.toList()));
        parseResult.setMethods(mSignatures.stream()
                .map(c -> {
                    LQLMethodSignature lm = new LQLMethodSignature(parseResult, c);
                    //lm.setParent(parseResult);
                    return lm;
                })
                .collect(Collectors.toList()));

        parseResults.add(parseResult);
        //}

        return parseResults;
    }

    public LQLParseResult parseLQL(String lql) throws IOException {
        //

        LQLParseResult parseResult = LQL.parse(lql);
        if (parseResult != null) {
            return parseResult;
        }

        throw new IOException("unsupported LQL query " + lql);
    }

    public ParsedSheet parseSheet(String ssnJsonlStr) throws IOException {
        SSNParser ssnParser = new SSNParser();

        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        return parsedSheet;
    }

    /**
     * Resolve invocable signatures
     *
     * FIXME add InvocableOperation instances
     */
    public AdapterSheet resolve(StimulusSheet stimulusSheet, AdaptedImplementation adaptedImplementation) {
        AdapterSheet adapterSheet = new AdapterSheet(stimulusSheet, adaptedImplementation);

        ClassUnderTest classUnderTest = adaptedImplementation.getAdaptee();
        ParsedSheet parsedSheet = stimulusSheet.getParsedSheet();
        Map<String, InterfaceSpecification> interfaceSpecifications = adapterSheet.getStimulusSheet().getInterfaceSpecifications();

        for(ParsedRow parsedRow : parsedSheet.getRows()) {
            ParsedCell outputCell = parsedRow.getOutput();
            List<ParsedCell> inputCells = parsedRow.getInputs();

            String operation = parsedRow.getOperation().getNodeValue().textValue();
            if(StringUtils.equalsIgnoreCase("create", operation)) {
                // FIXME handle creation of instance

                String receiverClass = inputCells.get(0).getNodeValue().textValue(); // that's the instance to create
                // receiver class is the one we test
                if(interfaceSpecifications.containsKey(receiverClass)) {
                    InterfaceSpecification interfaceSpecification = interfaceSpecifications.get(receiverClass);

                    LOG.debug("Found CUT '{}'", interfaceSpecification.getClassName());

                    int numConstructors = interfaceSpecification.getConstructors().size();

                    Optional<de.uni_mannheim.swt.lasso.arena.MethodSignature> methodSignatureOp;
                    if(numConstructors == 1) { // only one constructor
                        methodSignatureOp = Optional.ofNullable(interfaceSpecification.getConstructors().get(0));
                    } else { // multiple constructors
                        // input parameters
                        int params = inputCells.size() - 1;
                        methodSignatureOp = interfaceSpecification.getConstructors().stream().filter(m -> {
                            // FIXME more precise: based on type matching as well
                            LOG.debug("Constructor params {} vs {}", m.getParameterTypes(classUnderTest.loadClassUnsafe()).length, inputCells.size() - 1);
                            //if (StringUtils.equals(m.getName(), sig.getName())) {
                            if(m.getParameterTypes(classUnderTest.loadClassUnsafe()).length == params) {
                                LOG.debug("Matched interface constructor '{}'", m.toLQL());

                                return true;
                            }
                            //}

                            return false;
                        }).findFirst();
                    }

                    if(methodSignatureOp.isPresent()) {
                        de.uni_mannheim.swt.lasso.arena.MethodSignature methodSignature = methodSignatureOp.get();

                        ConstructorCallStatement callStatement = new ConstructorCallStatement(methodSignature);
                        callStatement.setClassUnderTest(true);

                        //context.getLocalFields().add(result.getAddress());
                        //int position = context.getLocalFields().lastIndexOf(result.getAddress());

                        //context.getSequenceSpecification().addStatement(callStatement, position);

                        InvocableOperation invocableOperation = new InvocableOperation();
                        // FIXME position
                        adapterSheet.getInvocableOperations().add(invocableOperation);

                        // now determine inputs
                        if(methodSignature.getParameterTypes(classUnderTest.loadClassUnsafe()).length > 0) {
                            int p = 0;
                            for(ParsedCell inputCell : inputCells.subList(1, inputCells.size())) {
                                // reference
                                if(inputCell.isValueReference()) {
//                                    //CellAddress inputRef = input.getAddress();
//                                    CellAddress inputRef = new CellAddress(input.getStringCellValue());
//                                    int inputPos = context.getLocalFields().lastIndexOf(inputRef);
//                                    SpecificationStatement inputStmt = context.getSequenceSpecification().getStatement(inputPos);
//                                    callStatement.addInput(inputStmt);

                                    String refCellId = inputCell.getNodeValue().textValue();

                                    // TODO resolve to ParsedRow + ParsedCell
                                    ParsedCell parsedCell = parsedSheet.resolve(refCellId);
                                    // resolve
                                    InvocableOperation refInvocable;
                                } else {
                                    // direct value passing
                                    Class<?> expectedType = methodSignature.getParameterTypes(classUnderTest.loadClassUnsafe())[p];

//                                    ValueStatement valueStatement = processValue(context, expectedType, input);
//                                    //context.getSequenceSpecification().addStatement(callStatement, context.getSequenceSpecification().getNextPosition());
//                                    callStatement.addInput(valueStatement);
                                }

                                p++;
                            }
                        }
                    }

                    // TODO

                } else {
                    // otherwise it is some known class from JDK (e.g. java.lang.String)
                }

                if(inputCells.size() > 1) {
                    inputCells.subList(1, inputCells.size()); // remaining input parameters
                }


            } else {
                // FIXME resolve method
            }

            // FIXME resolve code expressions
        }

        return adapterSheet;
    }
}
