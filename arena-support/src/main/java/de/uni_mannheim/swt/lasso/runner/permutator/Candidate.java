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

import de.uni_mannheim.swt.lasso.runner.permutator.strategy.method.AdaptationStrategy;
import de.uni_mannheim.swt.lasso.runner.permutator.strategy.producer.ProducerStrategy;

import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * A method candidate of the adaptee
 * 
 * @author Marcus Kessel
 * 
 */
public class Candidate {

    // supports Method as well as Constructor
    private Member method;

    // param positions (param switching)
    private int[] positions;

    private boolean executed;

    private boolean success;
    
    // is ghost method or not
    private boolean ghost;
    
    private String ghostDescription;
    
    private String[] paramConverterClasses;
    private String returnParamConverterClass;

    private Class<?> returnType;
    private Class<?>[] paramClasses;

    /**
     * Adaptation strategies for producers (i.e instance creators)
     *
     */
    private ProducerStrategy producerStrategy;

    /**
     * Adaptation strategy for methods
     */
    private AdaptationStrategy adaptationStrategy;

    /**
     * Data map
     *
     * @param method
     * @param positions
     */
    private Map<String, Object> data;

    public Candidate(Member method, int[] positions) {
        this.method = method;
        this.positions = positions;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void addValue(String key, Object value) {
        if(data == null) {
            data = new HashMap<>();
        }

        data.put(key, value);
    }

    public Object getValue(String key) {
        if(data == null) {
            return null;
        }

        return data.get(key);
    }

    /**
     * @return the method
     */
    public Member getMethod() {
        return method;
    }

    /**
     * @return the positions
     */
    public int[] getPositions() {
        return positions;
    }

    /**
     * @return the executed
     */
    public boolean isExecuted() {
        return executed;
    }

    /**
     * @param executed
     *            the executed to set
     */
    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param success
     *            the success to set
     */
    public void setSuccess(boolean success) {
        this.success = success;
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

    /**
     * @return the paramConverterClasses
     */
    public String[] getParamConverterClasses() {
        return paramConverterClasses;
    }

    /**
     * @param paramConverterClasses the paramConverterClasses to set
     */
    public void setParamConverterClasses(String[] paramConverterClasses) {
        this.paramConverterClasses = paramConverterClasses;
    }

    /**
     * @return the returnParamConverterClass
     */
    public String getReturnParamConverterClass() {
        return returnParamConverterClass;
    }

    /**
     * @param returnParamConverterClass the returnParamConverterClass to set
     */
    public void setReturnParamConverterClass(String returnParamConverterClass) {
        this.returnParamConverterClass = returnParamConverterClass;
    }

    /**
     * @return the ghostDescription
     */
    public String getGhostDescription() {
        return ghostDescription;
    }

    /**
     * @param ghostDescription the ghostDescription to set
     */
    public void setGhostDescription(String ghostDescription) {
        this.ghostDescription = ghostDescription;
    }

    public AdaptationStrategy getAdaptationStrategy() {
        return adaptationStrategy;
    }

    public void setAdaptationStrategy(AdaptationStrategy adaptationStrategy) {
        this.adaptationStrategy = adaptationStrategy;
    }

    public Class<?> getReturnType() {
        return returnType;
    }

    public void setReturnType(Class<?> returnType) {
        this.returnType = returnType;
    }

    public Class<?>[] getParamClasses() {
        return paramClasses;
    }

    public void setParamClasses(Class<?>[] paramClasses) {
        this.paramClasses = paramClasses;
    }

    public ProducerStrategy getProducerStrategy() {
        return producerStrategy;
    }

    public void setProducerStrategy(ProducerStrategy producerStrategy) {
        this.producerStrategy = producerStrategy;
    }

    @Override
    public String toString() {
        return "Candidate{" +
                "method=" + method +
                ", positions=" + Arrays.toString(positions) +
                ", executed=" + executed +
                ", success=" + success +
                ", ghost=" + ghost +
                ", ghostDescription='" + ghostDescription + '\'' +
                ", paramConverterClasses=" + Arrays.toString(paramConverterClasses) +
                ", returnParamConverterClass='" + returnParamConverterClass + '\'' +
                ", returnType=" + returnType +
                ", paramClasses=" + Arrays.toString(paramClasses) +
                ", producerStrategy=" + producerStrategy +
                ", adaptationStrategy=" + adaptationStrategy +
                ", data=" + data +
                '}';
    }

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//        Candidate candidate = (Candidate) o;
//        return ghost == candidate.ghost && Objects.equals(method, candidate.method) && Arrays.equals(positions, candidate.positions) && Objects.equals(ghostDescription, candidate.ghostDescription) && Arrays.equals(paramConverterClasses, candidate.paramConverterClasses) && Objects.equals(returnParamConverterClass, candidate.returnParamConverterClass) && Objects.equals(returnType, candidate.returnType) && Arrays.equals(paramClasses, candidate.paramClasses) && Objects.equals(producerStrategy, candidate.producerStrategy) && Objects.equals(adaptationStrategy, candidate.adaptationStrategy);
//    }
//
//    @Override
//    public int hashCode() {
//        int result = Objects.hash(method, ghost, ghostDescription, returnParamConverterClass, returnType, producerStrategy, adaptationStrategy);
//        result = 31 * result + Arrays.hashCode(positions);
//        result = 31 * result + Arrays.hashCode(paramConverterClasses);
//        result = 31 * result + Arrays.hashCode(paramClasses);
//        return result;
//    }
}
