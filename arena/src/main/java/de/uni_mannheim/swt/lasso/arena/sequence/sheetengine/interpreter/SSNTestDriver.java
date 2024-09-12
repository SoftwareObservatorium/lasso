package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.DefaultAdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.repository.DependencyResolver;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.repository.NexusInstance;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionConstructorSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.parser.unit.ReflectionMethodSignature;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.CutUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.ParsedSheet;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.resolve.SSNParser;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * SSN test driver.
 *
 * @author Marcus Kessel
 */
public class SSNTestDriver {

    private static final Logger LOG = LoggerFactory.getLogger(SSNTestDriver.class);

    String mavenRepoUrl = NexusInstance.LOCAL_URL;
    File localRepo = new File("/tmp/lalalamvn/local-repo");

    DependencyResolver resolver = new DependencyResolver(mavenRepoUrl, localRepo.getAbsolutePath());
    MavenRepository mavenRepository = new MavenRepository(resolver);

    public ExecutedInvocations runSheet(String ssnJsonlStr, String lql, Class cutClass, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // FIXME artifacts
        return runSheet(ssnJsonlStr, lql, CutUtils.createExample(cutClass), limitAdapters, executionListener);
    }

    public ExecutedInvocations runSheet(String ssnJsonlStr, String lql, String cutClass, List<String> artifacts, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        // artifacts
        String artifact = null;
        if(CollectionUtils.isNotEmpty(artifacts)) {
            artifact = artifacts.get(0);
        }

        return runSheet(ssnJsonlStr, lql, CutUtils.createExample(cutClass, artifact), limitAdapters, executionListener);
    }

    public ExecutedInvocations runSheet(String ssnJsonlStr, String lql, ClassUnderTest classUnderTest, int limitAdapters, InvocationVisitor executionListener) throws IOException {
        SSNParser ssnParser = new SSNParser();
        ParsedSheet parsedSheet = ssnParser.parseJsonl(ssnJsonlStr);

        Map<String, InterfaceSpecification> interfaceSpecificationMap = LQLUtils.lqlToMap(lql);

        SSNInterpreter interpreter = new SSNInterpreter();

        CandidatePool pool = new CandidatePool(mavenRepository, Collections.singletonList(classUnderTest));
        // automatically resolves project-related artifacts
        pool.initProjects();

        // TODO call with classundertest to set classloader
        Invocations invocations = interpreter.interpret(parsedSheet, interfaceSpecificationMap, classUnderTest);

        AdaptationStrategy adaptationStrategy = new DefaultAdaptationStrategy();

        // FIXME for all CUTs .. here only one
        String faName = interfaceSpecificationMap.keySet().stream().findFirst().get();

        List<AdaptedImplementation> adaptedImplementations = adaptationStrategy.adapt(interfaceSpecificationMap.get(faName), classUnderTest, limitAdapters);

        // run
        ExecutedInvocations executedInvocations = interpreter.run(invocations, adaptedImplementations.get(0), executionListener);

        return executedInvocations;
    }

    public InterfaceSpecification toLQL(String className, List<String> artifacts) {
        // artifacts
        String artifact = null;
        if(CollectionUtils.isNotEmpty(artifacts)) {
            artifact = artifacts.get(0);
        }

        try {
            ClassUnderTest classUnderTest = CutUtils.createExample(className, artifact);
            CandidatePool pool = new CandidatePool(mavenRepository, Collections.singletonList(classUnderTest));
            // automatically resolves project-related artifacts
            pool.initProjects();

            Class<?> clazz = classUnderTest.loadClass();

            InterfaceSpecification interfaceSpecification = new InterfaceSpecification();
            interfaceSpecification.setClassName(clazz.getSimpleName()); // use simple name and not fully-qualified one

            List<MethodSignature> constructors = new LinkedList<>();
            Arrays.stream(clazz.getDeclaredConstructors()).forEach(c -> {
                constructors.add(new ReflectionConstructorSignature(c));
            });
            interfaceSpecification.setConstructors(constructors);

            List<MethodSignature> methods = new LinkedList<>();
            // also detect all inherited, non-overridden super methods
            getAllDeclaredMethods(clazz).forEach(m -> {
                methods.add(new ReflectionMethodSignature(m));
            });

            interfaceSpecification.setMethods(methods);

            return interfaceSpecification;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Method> getAllDeclaredMethods(Class<?> cutClass) {
        List<Method> methods = new ArrayList<>(
                Arrays.asList(cutClass.getDeclaredMethods()));
        // find all protected/public methods from super hierarchy NOT
        // overridden
        List<Method> superMethods = new LinkedList<>();

        // is cutClass included?
        Iterator<Class<?>> mIt = ClassUtils.hierarchy(cutClass).iterator();
        while (mIt.hasNext()) {
            Class<?> superClass = mIt.next();

//            if(superClass.equals(Object.class)) {
//                // skip this one
//                continue;
//            }

            // we are only interested in protected, public members
            for (Method method : superClass.getDeclaredMethods()) {
                // only add if NOT private, so we can actually access it in
                // subclass
                if (!Modifier.isPrivate(method.getModifiers())) {
                    // if(methods.contains(method))
                    superMethods.add(method);
                }
            }
        }

        if (!superMethods.isEmpty()) {
            // remove all methods overridden in cut class
            for (Method superMethod : superMethods) {
                List<Class<?>> superMethodParams = Arrays
                        .asList(superMethod.getParameterTypes());

                boolean overridden = false;
                // check if super methods are overridden in cut class
                for (Method method : methods) {
                    List<Class<?>> methodParams = Arrays
                            .asList(method.getParameterTypes());

                    // same signature?
                    if (method.getName().equals(superMethod.getName())
                            && CollectionUtils.isEqualCollection(
                            methodParams, superMethodParams)) {
                        //
                        overridden = true;

                        break;
                    }
                }

                if (!overridden) {
                    methods.add(superMethod);
                }
            }
        }

        return methods;
    }
}
