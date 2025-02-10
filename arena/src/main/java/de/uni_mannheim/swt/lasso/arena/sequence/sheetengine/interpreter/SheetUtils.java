package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Sheet utilities
 *
 * @author Marcus Kessel
 */
public class SheetUtils {

    private static final Logger LOG = LoggerFactory.getLogger(SheetUtils.class);

    public static List<Sheet<Integer, Integer, String>> toSheets(AdaptedImplementation adaptedImplementation, ExecutedInvocations executedInvocations, ObjectMapper objectMapper) {
        return Arrays.asList(toActuationSheet(executedInvocations, objectMapper), toAdaptedActutationSheet(adaptedImplementation, executedInvocations, objectMapper));
    }

    public static Sheet<Integer, Integer, String> toActuationSheet(ExecutedInvocations executedInvocations, ObjectMapper objectMapper) {
        Sheet<Integer, Integer, String> actuationSheet = new Sheet<>();

        for(int index = 0; index < executedInvocations.getExecutedSequence().size(); index++) {
            ExecutedInvocation executedInvocation = executedInvocations.getExecutedSequence().get(index);

            // write invocation target
            try {
                String serializedValue = objectMapper.writeTarget(executedInvocations.getExecutedInvocation(index));

                LOG.debug("serialized target instance => {}", serializedValue);

                // store in sheet
                actuationSheet.put(index, 2, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // write INPUTS! (since they could be mutated by underlying method!)
            int columnStart = 1 /*output*/ + 1 /*op*/ + 1 /*self*/;
            List<Obj> inputs = executedInvocation.getInputs();
            for(int p = 0; p < inputs.size(); p++) {
                try {
                    String serializedValue = objectMapper.writeInput(executedInvocations.getExecutedInvocation(index), p);

                    LOG.debug("serialized input value => {}", serializedValue);

                    // store in sheet
                    actuationSheet.put(index, columnStart + p, serializedValue);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // collect in existing objects or directly stream out?
            try {
                String serializedValue = objectMapper.writeOutput(executedInvocations.getExecutedInvocation(index));

                LOG.debug("serialized output value => {}", serializedValue);

                // store in sheet
                actuationSheet.put(index, 0, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // write operation
            try {
                String serializedValue = objectMapper.writeOp(executedInvocations.getExecutedInvocation(index));

                LOG.debug("serialized op => {}", serializedValue);

                // store in sheet
                actuationSheet.put(index, 1, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return actuationSheet;
    }

    public static Sheet<Integer, Integer, String> toAdaptedActutationSheet(AdaptedImplementation adaptedImplementation, ExecutedInvocations executedInvocations, ObjectMapper objectMapper) {
        Sheet<Integer, Integer, String> adaptedActuationSheet = new Sheet<>();

        for(int index = 0; index < executedInvocations.getExecutedSequence().size(); index++) {
            ExecutedInvocation executedInvocation = executedInvocations.getExecutedSequence().get(index);

            // write invocation target
            try {
                String serializedValue = objectMapper.writeTarget(executedInvocations.getExecutedInvocation(index));

                LOG.debug("serialized target instance => {}", serializedValue);

                // store in sheet
                // FIXME do the same for proxy, adaptee etc. (i.e., adapted values)
                adaptedActuationSheet.put(index, 2, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // write INPUTS! (since they could be mutated by underlying method!)
            int columnStart = 1 /*output*/ + 1 /*op*/ + 1 /*self*/;
            List<Obj> inputs = executedInvocation.getInputs();
            for(int p = 0; p < inputs.size(); p++) {
                try {
                    String serializedValue = objectMapper.writeInput(executedInvocations.getExecutedInvocation(index), p);

                    LOG.debug("serialized input value => {}", serializedValue);

                    // store in sheet
                    // FIXME do the same for proxy, adaptee etc. (i.e., adapted values)
                    adaptedActuationSheet.put(index, columnStart + p, serializedValue);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            // collect in existing objects or directly stream out?
            try {
                String serializedValue = objectMapper.writeOutput(executedInvocations.getExecutedInvocation(index));

                LOG.debug("serialized output value => {}", serializedValue);

                // store in sheet
                // FIXME do the same for proxy, adaptee etc. (i.e., adapted values)
                adaptedActuationSheet.put(index, 0, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // write adapted operation (i.e., adapter signature)
            try {
                String serializedValue = objectMapper.writeAdaptedOp(executedInvocations.getExecutedInvocation(index), adaptedImplementation);

                LOG.debug("serialized adapted op => {}", serializedValue);
                adaptedActuationSheet.put(index, 1, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return adaptedActuationSheet;
    }

    public static ExecutedInvocations toOracle(Invocations invocations) {
        ExecutedInvocations executedInvocations = new ExecutedInvocations(invocations);
        for(Invocation invocation : invocations.getSequence()) {
            ExecutedInvocation executedInvocation = executedInvocations.create(invocation);
            Parameter parameter = invocation.getExpectedOutput();
            executedInvocation.setOutput(Obj.fromValue(parameter.getValue(), Obj.PRODUCER_INDEX_NONE));
        }

        return executedInvocations;
    }

    public static Sheet<Integer, Integer, String> toOracleSheet(ExecutedInvocations executedInvocations, ObjectMapper objectMapper) {
        Sheet<Integer, Integer, String> actuationSheet = new Sheet<>();

        for(int index = 0; index < executedInvocations.getExecutedSequence().size(); index++) {
            ExecutedInvocation executedInvocation = executedInvocations.getExecutedSequence().get(index);
            // collect in existing objects or directly stream out?
            try {
                String serializedValue = objectMapper.writeOutput(executedInvocations.getExecutedInvocation(index));

                LOG.debug("serialized output value => {}", serializedValue);

                // store in sheet
                actuationSheet.put(index, 0, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return actuationSheet;
    }
}
