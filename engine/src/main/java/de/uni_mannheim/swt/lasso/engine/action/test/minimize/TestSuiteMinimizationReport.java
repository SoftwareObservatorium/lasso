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
package de.uni_mannheim.swt.lasso.engine.action.test.minimize;

import de.uni_mannheim.swt.lasso.core.data.LassoReport;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

/**
 *
 * @author Marcus Kessel
 */
@Deprecated
public class TestSuiteMinimizationReport extends LassoReport {

    //@QuerySqlField(index = false)
    //private int allTestsTotal;

    @QuerySqlField(index = false)
    private int minKillingSeedTestsTotal;

    @QuerySqlField(index = false)
    private int minKillingAmpTestsTotal;

    @QuerySqlField(index = false)
    private String killingAmpTests;

    @QuerySqlField(index = false)
    private String killingSeedTests;

    @QuerySqlField(index = false)
    private int killingImplementationsTotal;

    @QuerySqlField(index = false)
    private String killingImplementations;

//    public int getAllTestsTotal() {
//        return allTestsTotal;
//    }
//
//    public void setAllTestsTotal(int allTestsTotal) {
//        this.allTestsTotal = allTestsTotal;
//    }

    public String getKillingAmpTests() {
        return killingAmpTests;
    }

    public void setKillingAmpTests(String killingAmpTests) {
        this.killingAmpTests = killingAmpTests;
    }

    public String getKillingSeedTests() {
        return killingSeedTests;
    }

    public void setKillingSeedTests(String killingSeedTests) {
        this.killingSeedTests = killingSeedTests;
    }

    public String getKillingImplementations() {
        return killingImplementations;
    }

    public void setKillingImplementations(String killingImplementations) {
        this.killingImplementations = killingImplementations;
    }

    public int getMinKillingSeedTestsTotal() {
        return minKillingSeedTestsTotal;
    }

    public void setMinKillingSeedTestsTotal(int minKillingSeedTestsTotal) {
        this.minKillingSeedTestsTotal = minKillingSeedTestsTotal;
    }

    public int getMinKillingAmpTestsTotal() {
        return minKillingAmpTestsTotal;
    }

    public void setMinKillingAmpTestsTotal(int minKillingAmpTestsTotal) {
        this.minKillingAmpTestsTotal = minKillingAmpTestsTotal;
    }

    public int getKillingImplementationsTotal() {
        return killingImplementationsTotal;
    }

    public void setKillingImplementationsTotal(int killingImplementationsTotal) {
        this.killingImplementationsTotal = killingImplementationsTotal;
    }
}
