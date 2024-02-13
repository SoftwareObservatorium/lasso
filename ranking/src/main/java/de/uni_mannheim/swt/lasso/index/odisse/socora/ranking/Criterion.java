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
package de.uni_mannheim.swt.lasso.index.odisse.socora.ranking;

/**
 * Model representing a criterion as input for ranking
 * 
 * @author Marcus Kessel
 *
 */
public class Criterion {

    public static final double OBJECTIVE_MAX = 1d;
    public static final double OBJECTIVE_MIN = 0d;

    /**
     * Usually denotes metric name (id)
     */
    private String id;

    /**
     * Objective, either 1d or 0d
     */
    private double objective;

    /**
     * Some weight (e.g. weighted-sum)
     */
    private double weight;

    /**
     * Integers in case of Hybrid, Non-Dominated Sorting
     */
    private double priority;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the objective
     */
    public double getObjective() {
        return objective;
    }

    /**
     * @param objective
     *            the objective to set
     */
    public void setObjective(double objective) {
        this.objective = objective;
    }

    /**
     * @return the weight
     */
    public double getWeight() {
        return weight;
    }

    /**
     * @param weight
     *            the weight to set
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    /**
     * @return the priority
     */
    public double getPriority() {
        return priority;
    }

    /**
     * @param priority
     *            the priority to set
     */
    public void setPriority(double priority) {
        this.priority = priority;
    }

    @Override
    public String toString() {
        return "Criterion{" +
                "id='" + id + '\'' +
                ", objective=" + objective +
                ", weight=" + weight +
                ", priority=" + priority +
                '}';
    }
}
