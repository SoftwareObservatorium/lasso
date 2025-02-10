package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.HierarchyMemberResolver;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.MemberResolutionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple pass-through stategy (no adaptation, only delegation, assuming that required signatures are equivalent).
 *
 * @author Marcus Kessel
 */
public class PassThroughAdaptationStrategy implements AdaptationStrategy {

    /**
     * Relaxing naming allowed if no candidate found?
     */
    private boolean relaxMethodNaming;

    /**
     * Only one {@link AdaptedImplementation} is returned.
     *
     * @param specification {@link InterfaceSpecification}
     * @param classUnderTest {@link ClassUnderTest}
     * @param limit max. number of adapters to create
     * @return
     */
    @Override
    public List<AdaptedImplementation> adapt(InterfaceSpecification specification, ClassUnderTest classUnderTest, int limit) {
        try {
            Map<MethodSignature, Method> methodMapping = resolveMethods(specification, classUnderTest);
            Map<MethodSignature, Constructor> initializerMapping = resolveConstructor(specification, classUnderTest);

            PassThroughImplementation passThroughImplementation = new PassThroughImplementation(classUnderTest, methodMapping, initializerMapping);

            return List.of(passThroughImplementation);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    Map<MethodSignature, Method> resolveMethods(InterfaceSpecification specification, ClassUnderTest classUnderTest) throws ClassNotFoundException, NoSuchMethodException {
        Map<MethodSignature, Method> mapping = new LinkedHashMap<>();
        for(MethodSignature m : specification.getMethods()) {
            try {
                Method method = MemberResolutionUtils.resolveDeclaredMethod(classUnderTest.loadClass(), m.getName(), m.getParameterTypes(classUnderTest.loadClass()), true);
                mapping.put(m, method);
            } catch (NoSuchMethodException e) {
                if(isRelaxMethodNaming()) {
                    HierarchyMemberResolver resolver = new HierarchyMemberResolver(classUnderTest.loadClass(), m.getName(), m.getParameterTypes(classUnderTest.loadClass()));
                    resolver.setIgnoreMethodName(true);

                    Method method = resolver.resolveMethod();

                    mapping.put(m, method);
                } else {
                    throw e;
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return mapping;
    }

    Map<MethodSignature, Constructor> resolveConstructor(InterfaceSpecification specification, ClassUnderTest classUnderTest) throws ClassNotFoundException, NoSuchMethodException {
        Map<MethodSignature, Constructor> mapping = new LinkedHashMap<>();
        for(MethodSignature m : specification.getConstructors()) {
            Constructor constructor = MemberResolutionUtils.resolveDeclaredConstructor(classUnderTest.loadClass(), m.getParameterTypes(classUnderTest.loadClass()), true);
            mapping.put(m, constructor);
        }

        return mapping;
    }

    public boolean isRelaxMethodNaming() {
        return relaxMethodNaming;
    }

    public void setRelaxMethodNaming(boolean relaxMethodNaming) {
        this.relaxMethodNaming = relaxMethodNaming;
    }
}
