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
package de.uni_mannheim.swt.lasso.arena;

import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.srm.CellId;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;

/**
 *
 * @author Marcus Kessel
 */
public class ArenaUtils {

    public static CellId cellIdOf(String sheetId, int x, int y, String type, AdaptedImplementation implementation) {
        CellId cell = new CellId();
        cell.setSheetId(sheetId);
        cell.setX(x);
        cell.setY(y);
        cell.setType(type);
        cell.setSystemId(implementation.getAdaptee().getId());
        cell.setVariantId(implementation.getAdaptee().getVariantId());
        cell.setAdapterId(String.valueOf(implementation.getAdapterId()));

        return cell;
    }

    public static CellId cellIdOfOracle(String sheetId, int x, int y, String type) {
        CellId cell = new CellId();
        cell.setSheetId(sheetId);
        cell.setX(x);
        cell.setY(y);
        cell.setType(type);
        cell.setSystemId("oracle");
        cell.setVariantId("oracle");
        cell.setAdapterId("oracle");

        return cell;
    }

    public static boolean equalsMethod(Method method, String bytecodeName) {
        Class<?> declaringClass = method.getDeclaringClass();

        String name = Type.getType(declaringClass).getInternalName();//.replace('/', '.');
        String desc = Type.getMethodDescriptor(method);

        String otherName = String.format("%s.%s%s", name, method.getName(), desc);

        return StringUtils.equals(bytecodeName, otherName);
    }

    /**
     * Load class under test.
     *
     * @param classUnderTest
     * @return
     * @throws ClassNotFoundException
     */
    @Deprecated
    public static Class<?> loadClass(ClassUnderTest classUnderTest) throws ClassNotFoundException {
        return classUnderTest.loadClass();
    }

    public static Class<?> loadClassFor(AdaptedImplementation adaptedImplementation, String name) throws ClassNotFoundException {
        return adaptedImplementation.getAdaptee().getProject().getContainer().loadClass(name);
    }

    public static Class<?> loadClassFor(ClassUnderTest classUnderTest, String name) throws ClassNotFoundException {
        return classUnderTest.getProject().getContainer().loadClass(name);
    }
}
