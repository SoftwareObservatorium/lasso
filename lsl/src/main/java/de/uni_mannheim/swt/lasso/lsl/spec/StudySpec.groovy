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

/**
 * Represents a study block in LSL.
 *
 * <code>
 *      study(map) {closure}
 * </code>
 *
 * @author Marcus Kessel
 */
class StudySpec extends LassoSpec {

    /**
     * Properties passed to study block
     */
    Map<String, ?> map
    /**
     * Closure which is called to populate study block
     */
    Closure<StudySpec> closure

    /**
     *
     * @return study name
     */
    String getName() {
        map.name
    }

    /**
     * Call the closure
     */
    void apply() {
        this.with(closure)
    }

    /**
     * Add new action
     *
     * @param map Properties of action (name, type etc.)
     * @param closure Closure to call
     * @return
     */
    def action(Map<String, ?> map, Closure<ActionSpec> closure) {
        if(!map.containsKey('type')) {
            map.put('type', 'NoOp') // default to NoOp
        }

        ActionSpec actionSpec = new ActionSpec(map:map, closure: closure)

        lasso.registerAction(actionSpec)

        // apply action immediately
        actionSpec.apply()
    }

    /**
     * Add new profile (i.e., target execution profile)
     *
     * @param name Unique name of execution profile
     * @param closure Closure to call
     * @return
     */
    def profile(String name, Closure<ProfileSpec> closure) {
        ProfileSpec profileSpec = new ProfileSpec(name:name)
        callRehydrate(closure, profileSpec, this, null)

        lasso.registerProfile(profileSpec)
    }
}
