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
package de.uni_mannheim.swt.lasso.lsl.spec

import de.uni_mannheim.swt.lasso.lsl.LassoContext

/**
 * Base class of all Specs in the DSL. Provides basic functionality like logging.
 *
 * @author Marcus Kessel
 */
abstract class LassoSpec {

    transient LassoContext lasso

    /**
     * Clone and call closure on passed model.
     *
     * @param closure
     * @param model
     * @param owner
     * @param args
     * @return
     */
    def callRehydrate(Closure closure, model, owner, args) {
        // closure defined?
        if(!closure) {
            if(defaultValue) {
                return defaultValue
            }

            return null
        }

        // clone action
        Closure cloneCl = closure.rehydrate(model, owner, this)
        cloneCl.resolveStrategy = Closure.DELEGATE_FIRST
        if(args) {
            // arr
            return cloneCl(args)
        } else {
            return cloneCl()
        }
    }

    /**
     * Debug messages
     *
     * @param msg
     * @return
     */
    def debug(def msg) {
        lasso.logger.log("DEBUG => ${msg}")
    }


    /**
     * Log messages
     *
     * @param msg
     * @return
     */
    def log(def msg) {
        lasso.logger.log("INFO => ${msg}")
    }
}
