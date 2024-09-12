package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedMethod;
import de.uni_mannheim.swt.lasso.arena.adaptation.conversion.Converter;

import java.lang.reflect.Method;

/**
 *
 * @author Marcus Kessel
 */
public class PassThroughMethod extends AdaptedMethod {

    public PassThroughMethod(MethodSignature specification, ClassUnderTest adaptee, Method method) {
        super(specification, adaptee, method);
    }

    @Override
    public boolean canConvert(Class<?> fromClazz, Class<?> toClazz) {
        return false;
    }

    @Override
    public Class<? extends Converter> getConverterClass(Class<?> fromClazz, Class<?> toClazz) {
        return null;
    }
}
