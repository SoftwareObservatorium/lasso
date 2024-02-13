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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;

/**
 * Abstract class of an adapted initializer.
 *
 * @author Marcus Kessel
 */
public abstract class AdaptedInitializer extends AdaptedMember {

    private Constructor<?> alternativeConstructor;

    public AdaptedInitializer(MethodSignature specification, ClassUnderTest adaptee, Member initializer) {
        super(specification, adaptee, initializer);
    }

    public Member getInitializer() {
        return hasAlternativeConstructor() ? alternativeConstructor : getMember();
    }

    public Constructor<?> getAsConstructor() {
        return (Constructor<?>) getInitializer();
    }

    public boolean hasMember() {
        return member != null;
    }

    public boolean isConstructor() {
        return (hasMember() && getMember() instanceof Constructor) || hasAlternativeConstructor();
    }

    public boolean hasAlternativeConstructor() {
        return alternativeConstructor != null;
    }

    public void setAlternativeConstructor(Constructor<?> alternativeConstructor) {
        this.alternativeConstructor = alternativeConstructor;
    }
}
