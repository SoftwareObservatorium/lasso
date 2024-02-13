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
package de.uni_mannheim.swt.lasso.lsl


import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionContainerSpec
import de.uni_mannheim.swt.lasso.lsl.spec.AbstractionSpec
import de.uni_mannheim.swt.lasso.lsl.spec.ActionContainerSpec
import de.uni_mannheim.swt.lasso.lsl.spec.ActionSpec
import de.uni_mannheim.swt.lasso.lsl.spec.ProfileContainerSpec
import de.uni_mannheim.swt.lasso.lsl.spec.LassoSpec
import de.uni_mannheim.swt.lasso.lsl.spec.ProfileSpec
import de.uni_mannheim.swt.lasso.lsl.spec.StudyContainerSpec
import de.uni_mannheim.swt.lasso.lsl.spec.StudySpec

/**
 *
 * @author Marcus Kessel
 */
class LassoContext {

    String executionId

    LSLLogger logger

    File workspaceRoot

    List<String> dataSources = []

    StudyContainerSpec studyContainerSpec = new StudyContainerSpec()

    Object executionContext

    @Deprecated
    AbstractionContainerSpec abstractionContainerSpec = new AbstractionContainerSpec()

    ActionContainerSpec actionContainerSpec = new ActionContainerSpec()
    ProfileContainerSpec profileContainerSpec = new ProfileContainerSpec()

    void register(LassoSpec lassoSpec) {
        lassoSpec.lasso = this

        println("Registered spec ${lassoSpec}")
    }

    void registerDataSource(String dataSource) {
        dataSources << dataSource

        println("Registered dataSource ${dataSource}")
    }

    void registerDataSourceQuery(String dataSource, LassoSpec queryModel) {
        queryModel.lasso = this

        println("Registered dataSource query ${dataSource}")
    }

    void registerStudy(StudySpec studySpec) {
        studySpec.lasso = this

        println("Registered StudySpec ${studySpec.name}")

        studyContainerSpec.studies.put(studySpec.name, studySpec)
    }

    void registerAction(ActionSpec actionSpec) {
        actionSpec.lasso = this

        println("Registered ActionSpec ${actionSpec.name}")

        actionContainerSpec.actions.put(actionSpec.name, actionSpec)
    }

    void registerProfile(ProfileSpec profileSpec) {
        profileSpec.lasso = this

        println("Registered ProfileSpec ${profileSpec.name}")

        profileContainerSpec.profiles.put(profileSpec.name, profileSpec)
    }

    void registerAbstraction(AbstractionSpec abstractionSpec) {
        abstractionSpec.lasso = this

        println("Registered AbstractionSpec ${abstractionSpec.name}")

        abstractionContainerSpec.abstractions.put(abstractionSpec.name, abstractionSpec)
    }

    /**
     * Add well-known navigation paths.
     *
     * @param name
     * @return
     */
    def propertyMissing(String name) {
        if(name == "studies") {
            return studyContainerSpec.studies
        }

        if(name == "abstractions") {
            return abstractionContainerSpec.abstractions
        }

        if(name == "actions") {
            return actionContainerSpec.actions
        }

        if(name == "profiles") {
            return profileContainerSpec.profiles
        }

        throw new MissingPropertyException(name, LassoContext)
    }
}
