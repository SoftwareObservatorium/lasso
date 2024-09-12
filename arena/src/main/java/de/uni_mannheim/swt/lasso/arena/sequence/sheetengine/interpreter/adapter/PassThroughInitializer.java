package de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.adapter;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedInitializer;
import de.uni_mannheim.swt.lasso.arena.adaptation.conversion.Converter;

import java.lang.reflect.Constructor;

/**
 *
 * @author Marcus Kessel
 */
public class PassThroughInitializer extends AdaptedInitializer {

    public PassThroughInitializer(MethodSignature specification, ClassUnderTest adaptee, Constructor constructor) {
        super(specification, adaptee, constructor);
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
