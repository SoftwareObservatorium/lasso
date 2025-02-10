package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;
import de.uni_mannheim.swt.lasso.core.adapter.AdapterDesc;
import de.uni_mannheim.swt.lasso.core.adapter.MethodDesc;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
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

    public AdapterDesc toDescription(InterfaceSpecification interfaceSpecification) {
        AdapterDesc adapterDesc = new AdapterDesc();
        adapterDesc.setAdapterId(getAdapterId());
        adapterDesc.setSystemId(getAdaptee().getId());
        adapterDesc.setVariantId(getAdaptee().getVariantId());
        adapterDesc.setClassName(getAdaptee().getClassName());

        List<MethodDesc> initializers = new ArrayList<>(interfaceSpecification.getConstructors().size());
        adapterDesc.setInitializers(initializers);
        for(int m = 0; m <interfaceSpecification.getConstructors().size(); m++) {
            try {
                PassThroughInitializer init = (PassThroughInitializer) getInitializer(interfaceSpecification, m);

                MethodDesc methodDesc = new MethodDesc();
                initializers.add(methodDesc);
                methodDesc.setConstructor(init.isConstructor());
                if(init.hasMember()) {
                    methodDesc.setMethod(init.getMember().toString());
                    methodDesc.setDeclaringClass(init.getMember().getDeclaringClass().getName());
                } else {
                    methodDesc.setMethod("_UNKNOWN_");
                }
            } catch (Throwable e) {
                MethodDesc methodDesc = new MethodDesc();
                initializers.add(methodDesc);
                //methodDesc.setConstructor(init.isConstructor());
                methodDesc.setMethod("_NA_");
            }
        }

        List<MethodDesc> methods = new ArrayList<>(interfaceSpecification.getConstructors().size());
        adapterDesc.setMethods(methods);
        for(int m = 0; m < interfaceSpecification.getMethods().size(); m++) {
            PassThroughMethod am = (PassThroughMethod) getMethod(interfaceSpecification, m);

            MethodDesc methodDesc = new MethodDesc();
            methods.add(methodDesc);
            methodDesc.setConstructor(false);
            methodDesc.setMethod(am.getMember().toString());

            methodDesc.setDeclaringClass(am.getMember().getDeclaringClass().getName());
        }

        return adapterDesc;
    }
}
