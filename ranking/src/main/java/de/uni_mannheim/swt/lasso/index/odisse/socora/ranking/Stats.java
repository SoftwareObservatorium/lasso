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
 * Statistics
 * 
 * @author Marcus Kessel
 *
 */
public class Stats {

    private double mean;
    private double min;
    private double max;
    private double stdev;
    private double sum;
    
    /**
     * @return the mean
     */
    public double getMean() {
        return mean;
    }
    /**
     * @param mean the mean to set
     */
    public void setMean(double mean) {
        this.mean = mean;
    }
    /**
     * @return the min
     */
    public double getMin() {
        return min;
    }
    /**
     * @param min the min to set
     */
    public void setMin(double min) {
        this.min = min;
    }
    /**
     * @return the max
     */
    public double getMax() {
        return max;
    }
    /**
     * @param max the max to set
     */
    public void setMax(double max) {
        this.max = max;
    }
    /**
     * @return the stdev
     */
    public double getStdev() {
        return stdev;
    }
    /**
     * @param stdev the stdev to set
     */
    public void setStdev(double stdev) {
        this.stdev = stdev;
    }
    /**
     * @return the sum
     */
    public double getSum() {
        return sum;
    }
    /**
     * @param sum the sum to set
     */
    public void setSum(double sum) {
        this.sum = sum;
    }
}
