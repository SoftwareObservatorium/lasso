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
package de.uni_mannheim.swt.lasso.engine.action.test.support.surefire;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * Surefire test case.
 * 
 * @author Marcus Kessel
 *
 */
public class SurefireTestCase implements Serializable {

    private String name;
    private String className;
    private double time;

    private String type;

    /**
     * 0 success, 1 failure, 2 error
     */
    private int result = 0;

    private String message;

    public boolean hasPassed() {
        return result == 0;
    }

    public boolean hasFailed() {
        return result == 1;
    }

    public boolean hasError() {
        return result == 2;
    }

    public boolean hasPermId() {
        return getPermutationId() > -1;
    }

    /**
     *
     * @return >= 0 success, < 0 fail
     */
    public int getPermutationId() {
        int index = StringUtils.indexOf(getName(), '_');
        if(index > 0) {
            try {
                return Integer.parseInt(StringUtils.substring(getName(), 0, index));
            } catch (NumberFormatException e) {
                //
            }
        }

        return -1;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }

    public int getResult() {
        return result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
