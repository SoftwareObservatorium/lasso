package de.uni_mannheim.swt.lasso.arena.sequence.groovyengine;

import com.google.common.collect.Maps;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.classloader.Container;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.groovyengine.read.TestSpec;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class GroovySheetEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GroovySheetEngine.class);

    private final Container container;

    public GroovySheetEngine(Container container) {
        this.container = container;
    }

    public ExecutedSequence run(CallableSequence callableSequence, InterfaceSpecification interfaceSpecification, AdaptedImplementation implementation) {
        TestSpec testSpec = new TestSpec();
        Map<String, InterfaceSpecification> interfaces = new LinkedHashMap<>();
        interfaces.put(interfaceSpecification.getClassName(), interfaceSpecification);
        testSpec.setInterfaces(interfaces);
        testSpec.setCallableSequence(callableSequence);

        return run(testSpec, implementation);
    }

    public ExecutedSequence run(TestSpec testSpec, AdaptedImplementation implementation) {
        Binding sharedData = new Binding();
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(SequenceScript.class.getCanonicalName());
        GroovyShell shell = new GroovyShell(container, sharedData, config);

        CallableSequence callableSequence = testSpec.getCallableSequence();

        ExecutedSequence executedSequence = new ExecutedSequence();
        executedSequence.setStatements(new ArrayList<>(callableSequence.getStatements().size()));

        ExecutionContext context = new ExecutionContext();
        context.setTestSpec(testSpec);
        context.setExecutedSequence(executedSequence);
        context.setImplementation(implementation);

        shell.setProperty("_sequenceCtx", context);

        // statement-by-statement execution
        for(CallableStatement callableStatement : callableSequence.getStatements()) {
            // FIXME move to script base
            List<Object> inputs = new LinkedList<>();
            for(CallableStatement inputStmt : callableStatement.getInputs()) {
                ExecutedStatement execStmt = executedSequence.getStatements().get(inputStmt.getIndex());
                inputs.add(execStmt);
            }

            ExecutedStatement executedStatement = new ExecutedStatement(callableStatement);
            executedSequence.addStatement(executedStatement);

            //Object result = shell.evaluate(callableStatement.getCode());

            Script script = shell.parse(callableStatement.getCode());

            Object result = script.run();

            executedStatement.setOutput(result);

            LOG.info("Result = {}", ToStringBuilder.reflectionToString(result, ToStringStyle.JSON_STYLE));
        }

        return executedSequence;
    }
}
