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
package de.uni_mannheim.swt.lasso.analyzer.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

/**
 * Based on historical format for indexing signatures.
 *
 * @author Marcus Kessel
 */
public class Signature {

    private String returnValue;
    private String methodName;
    private String visibility;
    private ArrayList<String> parameterTypes;

    public Signature(String visibility, String methodName,
            ArrayList<String> parameterTypes, String returnValue) {
        this.returnValue = returnValue;
        this.methodName = methodName;
        this.visibility = visibility;
        this.parameterTypes = parameterTypes;
    }

    public String toString() {
        StringBuffer sig = new StringBuffer();

        sig.append("vs_" + visibility);
        sig.append(";rv_" + returnValue);
        sig.append(";mn_" + methodName);

        for (Iterator<String> i = parameterTypes.iterator(); i.hasNext();) {
            sig.append(";pt_" + i.next());
        }
        return sig.toString().toLowerCase();
    }

    public String toStringParameterOrdered(boolean isFq) {
        StringBuffer sig = new StringBuffer();

        sig.append("rv_" + getParameterType(returnValue, isFq));
        sig.append(";mn_" + methodName);

        ArrayList<String> sortedParams = sortParameters(parameterTypes);

        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
            sig.append(";pt_" + getParameterType(i.next(), isFq));
        }
        return sig.toString().toLowerCase();
    }

    public String toStringParameterOrderedSyntax(boolean isFq) {
        StringBuffer sig = new StringBuffer();

        sig.append("rv_" + getParameterType(returnValue, isFq));

        ArrayList<String> sortedParams = sortParameters(parameterTypes);
        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
            sig.append(";pt_" + getParameterType(i.next(), isFq));
        }
        return sig.toString().toLowerCase();
    }

    /**
     * @param string
     *            String
     * @return string after last period (if existent), original string otherwise
     */
    private static String getClassName(String string) {
        return StringUtils.contains(string, '.') ? StringUtils
                .substringAfterLast(string, ".") : string;
    }

    private static String getParameterType(String string, boolean isFq) {
        return isFq ? string : getClassName(string);
    }

    public String toStringOrigSignature(boolean isFq) {
        StringBuffer sig = new StringBuffer();

        if (visibility != null && visibility.length() > 0)
            sig.append(visibility + " ");
        sig.append(getParameterType(returnValue, isFq) + " ");
        sig.append(methodName + "(");

        int count = 0;

        for (Iterator<String> i = parameterTypes.iterator(); i.hasNext();) {
            if (count == 0) {
                sig.append(getParameterType(i.next(), isFq));
            } else {
                sig.append(", " + getParameterType(i.next(), isFq));
            }
            count++;
        }
        sig.append(")");
        return sig.toString();
    }

    public ArrayList<String> sortParameters(ArrayList<String> parameters) {
        Collections.sort(parameters, String.CASE_INSENSITIVE_ORDER);
        return parameters;
    }

}

