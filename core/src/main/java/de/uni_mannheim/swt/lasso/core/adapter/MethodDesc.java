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
package de.uni_mannheim.swt.lasso.core.adapter;

import java.util.Map;

/**
 * Copied from Candidate
 *
 * @author Marcus Kessel
 */
public class MethodDesc {

    private String declaringClass;

    // supports Method as well as Constructor
    private String method;

    // param positions (param switching)
    private int[] positions;

    private boolean executed;

    private boolean success;

    // is ghost method or not
    private boolean ghost;

    private String ghostDescription;

    private String[] paramConverterClasses;
    private String returnParamConverterClass;

    private String returnType;
    private String[] paramClasses;

    /**
     * Adaptation strategies for producers (i.e instance creators)
     *
     */
    private String producerStrategy;

    /**
     * Adaptation strategy for methods
     */
    private String adaptationStrategy;

    private String converterStrategy;

    private boolean constructor;

    /**
     * Data map
     *
     * @param method
     * @param positions
     */
    private Map<String, Object> data;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int[] getPositions() {
        return positions;
    }

    public void setPositions(int[] positions) {
        this.positions = positions;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isGhost() {
        return ghost;
    }

    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    public String getGhostDescription() {
        return ghostDescription;
    }

    public void setGhostDescription(String ghostDescription) {
        this.ghostDescription = ghostDescription;
    }

    public String[] getParamConverterClasses() {
        return paramConverterClasses;
    }

    public void setParamConverterClasses(String[] paramConverterClasses) {
        this.paramConverterClasses = paramConverterClasses;
    }

    public String getReturnParamConverterClass() {
        return returnParamConverterClass;
    }

    public void setReturnParamConverterClass(String returnParamConverterClass) {
        this.returnParamConverterClass = returnParamConverterClass;
    }

    public String getReturnType() {
        return returnType;
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    public String[] getParamClasses() {
        return paramClasses;
    }

    public void setParamClasses(String[] paramClasses) {
        this.paramClasses = paramClasses;
    }

    public String getProducerStrategy() {
        return producerStrategy;
    }

    public void setProducerStrategy(String producerStrategy) {
        this.producerStrategy = producerStrategy;
    }

    public String getAdaptationStrategy() {
        return adaptationStrategy;
    }

    public void setAdaptationStrategy(String adaptationStrategy) {
        this.adaptationStrategy = adaptationStrategy;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getConverterStrategy() {
        return converterStrategy;
    }

    public void setConverterStrategy(String converterStrategy) {
        this.converterStrategy = converterStrategy;
    }

    public boolean isConstructor() {
        return constructor;
    }

    public void setConstructor(boolean constructor) {
        this.constructor = constructor;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public void setDeclaringClass(String declaringClass) {
        this.declaringClass = declaringClass;
    }
}
