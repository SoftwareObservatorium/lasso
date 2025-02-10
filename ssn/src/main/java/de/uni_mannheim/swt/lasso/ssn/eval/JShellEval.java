package de.uni_mannheim.swt.lasso.ssn.eval;

import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.LocalExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
// FIXME classloader
public class JShellEval implements Eval {

    private ClassLoader classLoader;

    private Object result = null;

    JShell create() {
        JShell shell = JShell.builder()
                .executionEngine(new ExecutionControlProvider() {
                    @Override
                    public String name() {
                        return "custom";
                    }

                    @Override
                    public ExecutionControl generate(ExecutionEnv ee, Map<String, String> map) throws Throwable {
                        return new LocalExecutionControl() {

                            @Override
                            protected String invoke(Method doitMethod) throws Exception {
                                Object res = doitMethod.invoke((Object)null);

//                                System.out.println(doitMethod);
//                                System.out.println(res.getClass());

                                result = res;

                                return valueString(res);
                            }
                        };
                    }
                }, null)
                .build();

        return shell;
    }

    @Override
    public Object eval(String expression) throws EvalException {
        JShell jShell = create();

        List<SnippetEvent> snippetList = jShell.eval(expression);

//        System.out.println(snippetList.get(0).value());

        return result;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class resolveClass(String className) throws ClassNotFoundException {
        return this.classLoader.loadClass(className);
    }
}
