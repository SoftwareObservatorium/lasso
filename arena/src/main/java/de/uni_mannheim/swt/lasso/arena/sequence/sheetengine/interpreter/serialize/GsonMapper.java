package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize;

import com.google.gson.Gson;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Output;
import de.uni_mannheim.swt.lasso.runner.permutator.TypeUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * A GSON-based mapper.
 *
 * @author Marcus Kessel
 */
public class GsonMapper implements ObjectMapper {

    private Gson gson;

    public GsonMapper() {
        this(new Gson());
    }

    public GsonMapper(Gson gson) {
        this.gson = new Gson();
    }

    /**
     * Uses special notation for complex objects inspired by debugging views.
     *
     * @param executedInvocation
     * @return
     * @throws IOException
     */
    @Override
    public String writeValue(ExecutedInvocation executedInvocation) throws IOException {
        Output output = executedInvocation.getOutput();

        String serializedStr;
        if(output.hasException()) {
            Throwable throwable = output.getException();
            serializedStr = gson.toJson("$EXCEPTION@" + throwable.getClass().getCanonicalName() + "@" + throwable.getMessage());
        } else if(output.isNull()) {
            serializedStr = gson.toJson(null); // FIXME allowed?
        } else if(output.isCutProxyReference()) {
            // CUT (i.e., proxy instance of cut)

            // serialize identifier of reference
            // format: ClassName@ROW
            serializedStr = gson.toJson("$CUT@" + output.getTypeAsName() + "@" + executedInvocation.getInvocation().getIndex());
        } else {
            // an object needs to be serialized
            // TODO primitive vs complex objects
            if(ClassUtils.isPrimitiveOrWrapper(output.getType())) {
                // FIXME special handling of primitive types?
                serializedStr = gson.toJson(output.getValue());
            } else if(TypeUtils.isAssignable(output.getType(), String.class)) {
                // FIXME use Charsequence instead?
                // TODO if string, use additional double-quotes as in Sequence sheets in JSONL notation
                // e.g. "'Hello World!'"
                serializedStr = gson.toJson(StringUtils.wrap((String) output.getValue(), "'"));
            } else if(TypeUtils.isAssignable(output.getType(), Void.class, true)) {
                // FIXME notation: use "{}" (empty object)?
                serializedStr = gson.toJson(new Object());
            } else {
                // FIXME
                serializedStr = gson.toJson(output.getValue());
            }
        }

        return serializedStr;
    }

    @Override
    public Output readValue(String value) throws IOException {
        // FIXME implement
        throw new UnsupportedOperationException("not implemented");
    }
}
