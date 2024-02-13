/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.arena.adaptation;

import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.MethodSignature;
import de.uni_mannheim.swt.lasso.arena.adaptation.conversion.Converter;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Abstract class of an adapted member.
 *
 * @author Marcus Kessel
 */
public abstract class AdaptedMember {

    protected final MethodSignature specification;
    protected final ClassUnderTest adaptee;
    protected final Member member;

    protected int[] positions;

    public AdaptedMember(MethodSignature specification, ClassUnderTest adaptee, Member member) {
        this.specification = specification;
        this.adaptee = adaptee;
        this.member = member;
    }

    public MethodSignature getSpecification() {
        return specification;
    }

    public ClassUnderTest getAdaptee() {
        return adaptee;
    }

    public Member getMember() {
        return member;
    }

    public Class[] getMemberParameterTypes() {
        Class[] params;
        if(member instanceof Constructor<?>) {
            // constructor
            params = ((Constructor) member).getParameterTypes();
        } else {
            // method
            params = ((Method)member).getParameterTypes();
        }

        return params;
    }

    public boolean isStatic() {
        return Modifier.isStatic(member.getModifiers());
    }

    public int[] getPositions() {
        return positions;
    }

    public void setPositions(int[] positions) {
        this.positions = positions;
    }

    public abstract boolean canConvert(Class<?> fromClazz, Class<?> toClazz);
    public abstract Class<? extends Converter> getConverterClass(Class<?> fromClazz, Class<?> toClazz);
}
