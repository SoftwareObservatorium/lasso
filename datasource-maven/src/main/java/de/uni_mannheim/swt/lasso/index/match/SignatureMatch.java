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
package de.uni_mannheim.swt.lasso.index.match;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * A signature match based on Solr's highlighting input.
 *
 * @author Marcus Kessel
 */
public class SignatureMatch {

    private String returnType;
    private String methodName;

    private String visibility = "public";

    private List<String> parameterTypes = new LinkedList<>();

    private Set<String> modifiers = new HashSet<>();

    private Map<String, Boolean> successMap = new HashMap<>();

    private boolean inherited;

    public SignatureMatch(String solrSnippet) {
        fromSolrSnippet(solrSnippet);
    }

    private void fromSolrSnippet(String solrSnippet) {
        StringTokenizer st = new StringTokenizer(solrSnippet, ";_");
        while (st.hasMoreTokens()) {
            String nt = st.nextToken();

            if (isToken("vs", nt)) {
                // visibility (deprecated)
                visibility = st.nextToken();

                successMap.put("vs", hasMatch(visibility));

                if (successMap.get("vs")) {
                    visibility = extract(visibility);
                }
            } else if (isToken("rv", nt)) {
                // return type
                returnType = st.nextToken();

                successMap.put("rv", hasMatch(returnType));

                if (successMap.get("rv")) {
                    returnType = extract(returnType);
                }
            } else if (isToken("mn", nt)) {
                // method name
                methodName = st.nextToken();

                successMap.put("mn", hasMatch(methodName));

                if (successMap.get("mn")) {
                    methodName = extract(methodName);
                }
            } else if (isToken("pt", nt)) {
                // parameter type
                String pType = st.nextToken();

                successMap.put("pt" + parameterTypes.size(), hasMatch(pType));

                if (successMap.get("pt" + parameterTypes.size())) {
                    pType = extract(pType);
                }

                parameterTypes.add(pType);
            } else if (isToken("kw", nt)) {
                // keywords
                String token = st.nextToken();
                if (isVisibility(token)) {
                    visibility = token;

                    successMap.put("vs", hasMatch(visibility));

                    if (successMap.get("vs")) {
                        visibility = extract(visibility);
                    }
                } else if (isParameterSize(token)) {
                    // care about match of parameters
                    successMap.put("ps", hasMatch(token));
                } else {
                    String modifier = extract(token);
                    successMap.put(modifier, hasMatch(token));

                    modifiers.add(modifier);
                }
            }
        }
    }

    public boolean isInherited() {
        return inherited;
    }

    public void setInherited(boolean inherited) {
        this.inherited = inherited;
    }

    public Map<String, Boolean> getSuccessMap() {
        return successMap;
    }

    public boolean isConstructor() {
        return hasName() && StringUtils.equals(methodName, "<init>");
    }

    public int getParameterSize() {
        return parameterTypes.size();
    }

    public boolean hasName() {
        return successMap.containsKey("mn");
    }

    private boolean isToken(String expected, String actual) {
        // remove prefix
        if (StringUtils.startsWith(actual, "<em>")) {
            return StringUtils.equals(expected, StringUtils.substringAfter(actual, "<em>"));
        }

        return StringUtils.equals(expected, actual);
    }

    private String extract(String token) {
        return StringUtils.substringBeforeLast(token, "</em>");
    }

    private boolean hasMatch(String token) {
        return StringUtils.endsWith(token, "</em>");
    }

    private boolean isVisibility(String token) {
        return StringUtils.containsAny(token, "private", "protected", "public");
    }

    private boolean isParameterSize(String token) {
        return StringUtils.startsWith(token, "ps");
    }

    public boolean hasParameterSizeMatch() {
        return hasSuccessfulMatch("ps");
    }

    public boolean hasParameterTypeMatch(int param) {
        return hasSuccessfulMatch("pt" + param);
    }

    public boolean hasReturnTypeMatch() {
        return hasSuccessfulMatch("rv");
    }

    public boolean hasMethodNameMatch() {
        return hasSuccessfulMatch("mn");
    }

    public boolean hasModifierMatch(String modifier) {
        return hasSuccessfulMatch(modifier);
    }

    public boolean hasVisibilityMatch() {
        return hasSuccessfulMatch("vs");
    }

    private boolean hasSuccessfulMatch(String key) {
        return successMap.containsKey(key) && successMap.get(key);
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(List<String> parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Set<String> getModifiers() {
        return modifiers;
    }

    public void setModifiers(Set<String> modifiers) {
        this.modifiers = modifiers;
    }
}
