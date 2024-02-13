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
package de.uni_mannheim.swt.lasso.engine.dag;

import de.uni_mannheim.swt.lasso.lsl.LassoContext;
import de.uni_mannheim.swt.lasso.lsl.spec.ActionSpec;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class ActionNode implements Serializable {

    private String name;
    private String type;

    /**
     * Link to external study
     */
    private String dependsOnStudy;

    /**
     * Transient!
     */
    private transient ActionSpec actionSpec;

    public static ActionNode from(ActionSpec actionSpec, LassoContext lassoContext) {
        ActionNode actionNode = new ActionNode();
        actionNode.setActionSpec(actionSpec);
        actionNode.setName(actionSpec.getName());
        actionNode.setType(actionSpec.getType());
        actionNode.setDependsOnStudy(lassoContext.getExecutionId());

        return actionNode;
    }

    public boolean isSameStudy(LassoContext lassoContext) {
        return StringUtils.equals(dependsOnStudy, lassoContext.getExecutionId());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ActionSpec getActionSpec() {
        return actionSpec;
    }

    public void setActionSpec(ActionSpec actionSpec) {
        this.actionSpec = actionSpec;
    }

    public String getDependsOnStudy() {
        return dependsOnStudy;
    }

    public void setDependsOnStudy(String dependsOnStudy) {
        this.dependsOnStudy = dependsOnStudy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActionNode that = (ActionNode) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type) && Objects.equals(dependsOnStudy, that.dependsOnStudy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, dependsOnStudy);
    }

    @Override
    public String toString() {
        return "ActionNode{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", dependsOnStudy='" + dependsOnStudy + '\'' +
                '}';
    }
}
