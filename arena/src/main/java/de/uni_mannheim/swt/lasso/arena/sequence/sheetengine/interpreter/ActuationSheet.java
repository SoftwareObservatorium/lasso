package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public class ActuationSheet {

    private static final Logger LOG = LoggerFactory.getLogger(ActuationSheet.class);

    private AdaptedImplementation adaptedImplementation;
    private ExecutedInvocations executedInvocations;

    public ExecutedInvocations getExecutedInvocations() {
        return executedInvocations;
    }

    public AdaptedImplementation getAdaptedImplementation() {
        return adaptedImplementation;
    }

    public void setAdaptedImplementation(AdaptedImplementation adaptedImplementation) {
        this.adaptedImplementation = adaptedImplementation;
    }

    public void setExecutedInvocations(ExecutedInvocations executedInvocations) {
        this.executedInvocations = executedInvocations;
    }

    public List<Sheet<Integer, Integer, String>> toSheetData(ObjectMapper objectMapper) {
        Sheet<Integer, Integer, String> actuationSheet = new Sheet<>();
        Sheet<Integer, Integer, String> adaptedActuationSheet = new Sheet<>();

        for(int index = 0; index < executedInvocations.getExecutedSequence().size(); index++) {
            ExecutedInvocation executedInvocation = executedInvocations.getExecutedSequence().get(index);

            // write invocation target
            try {
                String serializedValue = objectMapper.writeTarget(executedInvocations.getExecutedInvocation(index));

                LOG.debug("serialized target instance => {}", serializedValue);

                // store in sheet
                actuationSheet.put(index, 2, serializedValue);
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
                    actuationSheet.put(index, columnStart + p, serializedValue);
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
                actuationSheet.put(index, 0, serializedValue);
                // FIXME do the same for proxy, adaptee etc. (i.e., adapted values)
                adaptedActuationSheet.put(index, 0, serializedValue);
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

            // write adapted operation (i.e., adapter signature)
            try {
                String serializedValue = objectMapper.writeAdaptedOp(executedInvocations.getExecutedInvocation(index), adaptedImplementation);

                LOG.debug("serialized adapted op => {}", serializedValue);
                adaptedActuationSheet.put(index, 1, serializedValue);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return Arrays.asList(actuationSheet,adaptedActuationSheet);
    }
}
