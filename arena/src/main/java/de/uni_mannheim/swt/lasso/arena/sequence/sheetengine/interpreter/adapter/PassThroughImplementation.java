package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Delegates to precomputed mappings
 *
 * @author Marcus Kessel
 *
 * @see PassThroughAdaptationStrategy
 */
public class PassThroughImplementation extends AdaptedImplementation {


    private final Map<MethodSignature, Method> methodMapping;
    private final Map<MethodSignature, Constructor> initializerMapping;

    public PassThroughImplementation(ClassUnderTest classUnderTest, Map<MethodSignature, Method> methodMapping, Map<MethodSignature, Constructor> initializerMapping) {
        super(classUnderTest);
        
        this.methodMapping = methodMapping;
        this.initializerMapping = initializerMapping;
    }

    @Override
    public AdaptedMethod getMethod(InterfaceSpecification specification, int m) {
        MethodSignature methodSignature = specification.getMethods().get(m);

        return new PassThroughMethod(methodSignature, getAdaptee(), methodMapping.get(methodSignature));
    }

    @Override
    public int getNoOfMethods() {
        return methodMapping.size();
    }

    @Override
    public AdaptedInitializer getInitializer(InterfaceSpecification specification, int c) {
        MethodSignature cSignature = specification.getConstructors().get(c);

        return new PassThroughInitializer(cSignature, getAdaptee(), initializerMapping.get(cSignature));
    }

    @Override
    public AdaptedInitializer getDefaultInitializer() {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String getAdapterId() {
        return "0";
    }
}
