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

/**
 *
 * @author Marcus Kessel
 */
public class Logger {

    public static boolean DEBUG = true;
    public static boolean INFO = true;
    public static boolean WARN = true;
    public static boolean ERROR = true;

    public static boolean isDebugEnabled() {
        return DEBUG;
    }
    public static boolean isInfoEnabled() {
        return INFO;
    }
    public static boolean isWarnEnabled() {
        return WARN;
    }
    public static boolean isErrorEnabled() {
        return ERROR;
    }

    public static void debug(String msg) {
        System.out.println("debug >> " + msg);
    }

    public static void info(String msg) {
        System.out.println("info >> " + msg);
    }

    public static void warn(String msg, Throwable throwable) {
        System.out.println("warn >> " + msg + " => " + throwable.getMessage());
    }

    public static void error(String msg, Throwable throwable) {
        System.out.println("error >> " + msg + " => " + throwable.getMessage());
    }
}
