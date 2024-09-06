package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize;

import com.google.common.collect.Table;
import com.google.common.collect.TreeBasedTable;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Collect serialized {@link Obj} during sequence execution.
 *
 * @author Marcus Kessel
 */
public class ObjectMapperVisitor extends InvocationVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(ObjectMapperVisitor.class);

    private final ObjectMapper objectMapper;

    private Table<Integer, Integer, String> actuationSheet = TreeBasedTable.create();

    private Table<Integer, Integer, String> adaptedActuationSheet = TreeBasedTable.create();

    public ObjectMapperVisitor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void visitBeforeStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
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
    }

    @Override
    public void visitAfterStatement(ExecutedInvocations executedInvocations, int index, AdaptedImplementation adaptedImplementation) {
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

    public Table<Integer, Integer, String> getActuationSheet() {
        return actuationSheet;
    }

    public Table<Integer, Integer, String> getAdaptedActuationSheet() {
        return adaptedActuationSheet;
    }
}
