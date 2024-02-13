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
package de.uni_mannheim.swt.lasso.core.model;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 *
 * @author Marcus Kessel
 *
 */
public class Systems implements Serializable {

    private String actionInstanceId;

    private String abstractionName;
    private List<System> executables;

    private Specification specification;

    public static Systems fromAbstraction(Abstraction abstraction, String actionInstanceId) {
        Systems executables = new Systems();
        executables.setActionInstanceId(actionInstanceId);
        executables.setAbstractionName(abstraction.getName());
        executables.setSpecification(abstraction.getSpecification());

        // set executables
        if(abstraction.getImplementations() != null) {
            executables.setExecutables(abstraction.getImplementations());
        }

        return executables;
    }

    public Abstraction toAbstraction() {
        Abstraction abstraction = new Abstraction();
        abstraction.setName(abstractionName);

        if(executables != null) {
            abstraction.setImplementations(executables);
        }

        abstraction.setSpecification(getSpecification());

        return abstraction;
    }

    public void addSequence(Sequence sequence) {
        specification.getSequences().add(sequence);
    }

    public System getExecutable(String id) {
        if(executables == null) {
            throw new NoSuchElementException();
        }

        Optional<System> executableOptional = executables.stream()
                .filter(e -> StringUtils.equals(e.getId(), id))
                .findFirst();

        return executableOptional.get();
    }

    public boolean remove(String executableId) {
        if(executables == null) {
            return false;
        }

        return executables.removeIf(ex -> StringUtils.equals(executableId, ex.getId()));
    }

    public boolean hasExecutables() {
        return CollectionUtils.isNotEmpty(executables);
    }

    public List<System> getExecutables() {
        return executables;
    }

    public void setExecutables(List<System> executables) {
        this.executables = executables;
    }

    public String getActionInstanceId() {
        return actionInstanceId;
    }

    public void setActionInstanceId(String actionInstanceId) {
        this.actionInstanceId = actionInstanceId;
    }

    public String getAbstractionName() {
        return abstractionName;
    }

    public void setAbstractionName(String abstractionName) {
        this.abstractionName = abstractionName;
    }

    public Specification getSpecification() {
        return specification;
    }

    public void setSpecification(Specification specification) {
        this.specification = specification;
    }
}
