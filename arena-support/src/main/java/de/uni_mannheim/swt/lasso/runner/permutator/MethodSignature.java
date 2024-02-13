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
package de.uni_mannheim.swt.lasso.runner.permutator;

import java.util.Map;

/**
 * A method signature
 * 
 * @author Marcus Kessel
 *
 */
public class MethodSignature {

    /**
     * The Java-like method signature
     */
    private String signature;
    /**
     * The byte code descriptor representation
     */
    private String descriptor;
    /**
     * Positions of the parameters
     */
    private int[] positions;
    /**
     * Is this a ghost method? (method not available in candidate)
     */
    private boolean ghost;

    private String adaptationStrategyClass;
    private String producerStrategyClass;

    private String returnTypeClass;
    private String[] paramTypeClasses;

    private String memberType;

    private Map<String, String> data;
    
    /**
     * @return the signature
     */
    public String getSignature() {
        return signature;
    }
    /**
     * @param signature the signature to set
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public String getDescriptor() {
        return descriptor;
    }
    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }
    /**
     * @return the positions
     */
    public int[] getPositions() {
        return positions;
    }
    /**
     * @param positions the positions to set
     */
    public void setPositions(int[] positions) {
        this.positions = positions;
    }
    /**
     * @return the ghost
     */
    public boolean isGhost() {
        return ghost;
    }
    /**
     * @param ghost the ghost to set
     */
    public void setGhost(boolean ghost) {
        this.ghost = ghost;
    }

    public String getAdaptationStrategyClass() {
        return adaptationStrategyClass;
    }

    public void setAdaptationStrategyClass(String adaptationStrategyClass) {
        this.adaptationStrategyClass = adaptationStrategyClass;
    }

    public String getProducerStrategyClass() {
        return producerStrategyClass;
    }

    public void setProducerStrategyClass(String producerStrategyClass) {
        this.producerStrategyClass = producerStrategyClass;
    }

    public String getReturnTypeClass() {
        return returnTypeClass;
    }

    public void setReturnTypeClass(String returnTypeClass) {
        this.returnTypeClass = returnTypeClass;
    }

    public String[] getParamTypeClasses() {
        return paramTypeClasses;
    }

    public void setParamTypeClasses(String[] paramTypeClasses) {
        this.paramTypeClasses = paramTypeClasses;
    }

    public String getMemberType() {
        return memberType;
    }

    public void setMemberType(String memberType) {
        this.memberType = memberType;
    }

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}
