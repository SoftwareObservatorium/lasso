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
package de.uni_mannheim.swt.lasso.index.query.lql;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * Based on old merobase index field signatures format.
 * 
 * Modified by mkessel (copy resides in odisse-indexer module!)
 *
 * @author Marcus Kessel
 */
public class MethodSignature {

    public static final String INIT = "<init>";

    private String returnValue;
    private String methodName;
    private String visibility = "public";
    private List<String> parameterTypes;

    private int paramSize = 0;

    public List<MethodSignature> getPermutations() {
        return permutations;
    }

    public void setPermutations(List<MethodSignature> permutations) {
        this.permutations = permutations;
    }

    private List<MethodSignature> permutations;

    public MethodSignature(String signature) {
        parameterTypes = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(signature, ";_");
        while (st.hasMoreTokens()) {
            String nt = st.nextToken();
            if (nt.equals("vs"))
                visibility = st.nextToken();
            else if (nt.equals("rv"))
                returnValue = st.nextToken();
            else if (nt.equals("mn"))
                methodName = st.nextToken();
            else if (nt.equals("pt"))
                parameterTypes.add(st.nextToken());
        } // endwhile

        if(CollectionUtils.isNotEmpty(parameterTypes)) {
            this.paramSize = parameterTypes.size();
        }
    }

    public MethodSignature(String visibility, String methodName,
                           List<String> parameterTypes, String returnValue) {
        this.returnValue = returnValue;
        this.methodName = methodName;
        this.visibility = visibility;
        this.parameterTypes = parameterTypes;

        if(CollectionUtils.isNotEmpty(parameterTypes)) {
            this.paramSize = parameterTypes.size();
        }
    }

    public boolean isConstructor() {
        return StringUtils.equalsIgnoreCase(getMethodName(), INIT);
    }

    public String getVisibility() {
        return this.visibility;
    }

    public String getReturnValue() {
        return this.returnValue;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public List<String> getParameterTypes() {
        return this.parameterTypes;
    }

    public String toString() {
        StringBuffer sig = new StringBuffer();

        sig.append("vs_" + visibility);
        sig.append(";rv_" + returnValue);
        sig.append(";mn_" + methodName);

        for (Iterator i = parameterTypes.iterator(); i.hasNext();) {
            sig.append(";pt_" + (String) i.next());
        }
        return sig.toString().toLowerCase();
    }

    public String toStringParameterOrdered(boolean isFq, boolean isParamSize) {
        StringBuffer sig = new StringBuffer();

        sig.append("rv_" + getParameterType(returnValue, isFq));
        sig.append(";mn_" + methodName);

        List<String> sortedParams = sortParameters(parameterTypes);

        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
            sig.append(";pt_" + getParameterType(i.next(), isFq));
        }

        if(isParamSize) {
            addParamSize(sig);
        }

        return sig.toString().toLowerCase();
    }

    /**
     * Add param size keyword to signature.
     *
     * @param sig
     */
    private void addParamSize(StringBuffer sig) {
        sig.append(";kw_ps" + paramSize);
    }

    public String toStringParameterOrderedVisibility(boolean isFq, boolean isParamSize) {
        StringBuffer sig = new StringBuffer();

        sig.append("vs_" + visibility);
        sig.append(";rv_" + getParameterType(returnValue, isFq));
        sig.append(";mn_" + methodName);

        List<String> sortedParams = sortParameters(parameterTypes);

        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
            sig.append(";pt_" + getParameterType(i.next(), isFq));
        }

        if(isParamSize) {
            addParamSize(sig);
        }

        return sig.toString().toLowerCase();
    }

    public String toStringParameterOrderedSyntax(boolean isFq, boolean isParamSize) {
        StringBuffer sig = new StringBuffer();

        sig.append("rv_" + getParameterType(returnValue, isFq));

        List<String> sortedParams = sortParameters(parameterTypes);
        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
            sig.append(";pt_" + getParameterType(i.next(), isFq));
        }

        if(isParamSize) {
            addParamSize(sig);
        }

        return sig.toString().toLowerCase();
    }

    public String toStringParameterOrderedSyntaxThis(String className,
            boolean isFq, boolean isParamSize) {
        StringBuffer sig = new StringBuffer();

        String next = returnValue;
        if (next.equals(className))
            next = "this";
        sig.append("rv_" + getParameterType(next, isFq));

        List<String> sortedParams = sortParameters(parameterTypes);
        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
            next = i.next();
            if (next.equals(className))
                next = "this";
            sig.append(";pt_" + getParameterType(next, isFq));
        }

        if(isParamSize) {
            addParamSize(sig);
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

    public String toStringParsableSignature(boolean isFq) {
        StringBuilder sig = new StringBuilder();

        if (visibility != null && visibility.length() > 0)
            sig.append(visibility + " ");
        sig.append(getParameterType(returnValue, isFq) + " ");
        sig.append(methodName + "(");

        int count = 0;
        for (Iterator<String> i = parameterTypes.iterator(); i.hasNext();) {
            if (count != 0) {
                sig.append(", ");
            }

            sig.append(getParameterType(i.next(), isFq) + " " + "arg" + ++count);

        }
        sig.append(")");
        return sig.toString();
    }

//    public String toStringConstructorParameterOrdered(boolean isFq) {
//        StringBuffer sig = new StringBuffer();
//
//        sig.append("cn_" + methodName);
//        List<String> sortedParams = sortParameters(parameterTypes);
//
//        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
//            sig.append(";pt_" + getParameterType(i.next(), isFq));
//        }
//        return sig.toString().toLowerCase();
//    }
//
//    public String toStringConstructorParameterOrderedSyntax(boolean isFq) {
//        StringBuffer sig = new StringBuffer();
//
//        List<String> sortedParams = sortParameters(parameterTypes);
//        boolean first = true;
//        for (Iterator<String> i = sortedParams.iterator(); i.hasNext();) {
//            if (first) {
//                sig.append("pt_" + getParameterType(i.next(), isFq));
//                first = false;
//            } else {
//                sig.append(";pt_" + getParameterType(i.next(), isFq));
//            }
//        }
//        return sig.toString().toLowerCase();
//    }

    public List<String> sortParameters(List<String> parameters) {
        Collections.sort(parameters, String.CASE_INSENSITIVE_ORDER);
        return parameters;
    }

    public String getParameterList() {
        StringBuilder parameterList = new StringBuilder();
        for (String sp : parameterTypes) {
            parameterList.append(sp + " ");
        }
        return parameterList.toString();
    }

} // eof

