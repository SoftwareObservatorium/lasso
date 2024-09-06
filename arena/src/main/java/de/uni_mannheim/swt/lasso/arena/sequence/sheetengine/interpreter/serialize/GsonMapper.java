package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.serialize;

import com.google.gson.Gson;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.ExecutedInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Invocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.Obj;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.CodeInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.InstanceInvocation;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.invocation.MethodInvocation;
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
    public String writeOutput(ExecutedInvocation executedInvocation) throws IOException {
        Obj output = executedInvocation.getOutput();

        String serializedStr = toString(output);

        return serializedStr;
    }

    @Override
    public String writeInput(ExecutedInvocation executedInvocation, int p) throws IOException {
        Obj input = executedInvocation.getInputs().get(p);
        String serializedStr = toString(input);

        return serializedStr;
    }

    @Override
    public String writeTarget(ExecutedInvocation executedInvocation) throws IOException {
        if(executedInvocation.getInvocation().isMethodInvocation()) {
            Obj targetInstance = executedInvocation.resolveTargetInstance();
            String serializedStr = toString(targetInstance);

            return serializedStr;
        } else if(executedInvocation.getInvocation().isInstanceInvocation()) {
            Class targetClass = executedInvocation.getInvocation().getTargetClass();

            return gson.toJson(targetClass.getCanonicalName());
        }

        throw new IllegalArgumentException("unknown invocation type");
    }

    @Override
    public String writeOp(ExecutedInvocation executedInvocation) throws IOException {
        Invocation invocation = executedInvocation.getInvocation();
        // FIXME write CUT operation (i.e. LQL signature)
        if(invocation.isCodeInvocation()) {
            CodeInvocation codeInvocation = (CodeInvocation) invocation;

            return gson.toJson(codeInvocation.getCodeExpression());
        } else if(invocation.isInstanceInvocation()) {
            InstanceInvocation instanceInvocation = (InstanceInvocation) invocation;

            return gson.toJson(instanceInvocation.getAsConstructor().toString());
        } else if(invocation.isMethodInvocation()) {
            MethodInvocation methodInvocation = (MethodInvocation) invocation;

            return gson.toJson(methodInvocation.getMethod().toString());
        }

        throw new IllegalArgumentException("unknown invocation type");
    }

    @Override
    public String writeAdaptedOp(ExecutedInvocation executedInvocation, AdaptedImplementation adaptedImplementation) throws IOException {
        Invocation invocation = executedInvocation.getInvocation();
        // FIXME write CUT operation (i.e. LQL signature)
        if(invocation.isCodeInvocation()) {
            CodeInvocation codeInvocation = (CodeInvocation) invocation;

            return gson.toJson(codeInvocation.getCodeExpression());
        } else if(invocation.isInstanceInvocation()) {
            InstanceInvocation instanceInvocation = (InstanceInvocation) invocation;

            //AdaptedInitializer adaptedInitializer = executedInvocation.resolveAdaptedInitializer(adaptedImplementation);

            return gson.toJson(instanceInvocation.getAsConstructor().toString());
        } else if(invocation.isMethodInvocation()) {
            MethodInvocation methodInvocation = (MethodInvocation) invocation;

            return gson.toJson(methodInvocation.getMethod().toString());
        }

        throw new IllegalArgumentException("unknown invocation type");
    }

    @Override
    public Obj readOutput(String value) throws IOException {
        // FIXME implement
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Obj readInput(String value) throws IOException {
        // FIXME implement
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Obj readOp(String value) throws IOException {
        // FIXME implement
        throw new UnsupportedOperationException("not implemented");
    }

    String toString(Obj obj) {
        String serializedStr;
        if(obj.hasException()) {
            Throwable throwable = obj.getException();
            serializedStr = gson.toJson("$EXCEPTION@" + throwable.getClass().getCanonicalName() + "@" + throwable.getMessage());
        } else if(obj.isNull()) {
            serializedStr = gson.toJson(null); // FIXME allowed?
        } else if(obj.isCutProxyReference()) {
            // CUT (i.e., proxy instance of cut)

            // serialize identifier of reference
            // format: ClassName@ROW

            // FIXME we need to keep track of unique objects!!!
            serializedStr = gson.toJson("$CUT@" + obj.getTypeAsName() + "@" + obj.getProducerIndex());
        } else {
            // an object needs to be serialized
            // TODO primitive vs complex objects
            if(ClassUtils.isPrimitiveOrWrapper(obj.getType())) {
                // FIXME special handling of primitive types?
                serializedStr = gson.toJson(obj.getValue());
            } else if(TypeUtils.isAssignable(obj.getType(), String.class)) {
                // FIXME use Charsequence instead?
                // TODO if string, use additional double-quotes as in Sequence sheets in JSONL notation
                // e.g. "'Hello World!'"
                serializedStr = gson.toJson(StringUtils.wrap((String) obj.getValue(), "'"));
            } else if(TypeUtils.isAssignable(obj.getType(), Void.class, true)) {
                // FIXME notation: use "{}" (empty object)?
                serializedStr = gson.toJson(new Object());
            } else {
                // FIXME any value
                serializedStr = gson.toJson(obj.getValue());
            }
        }

        return serializedStr;
    }
}
