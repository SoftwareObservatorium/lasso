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
package de.uni_mannheim.swt.lasso.arena.sequence;

import de.uni_mannheim.swt.lasso.arena.Observation;
import de.uni_mannheim.swt.lasso.arena.adaptation.AdaptedImplementation;
import de.uni_mannheim.swt.lasso.arena.search.InterfaceSpecification;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Execution results of executing (multiple) sequence(s).
 *
 * @author Marcus Kessel
 */
public class SequenceExecutionRecords {

    private final AdaptedImplementation implementation;
    private final InterfaceSpecification specification;

    private List<SequenceExecutionRecord> records = new LinkedList<>();

    private Map<String, Observation> observations;

    public SequenceExecutionRecords(AdaptedImplementation implementation, InterfaceSpecification specification) {
        this.implementation = implementation;
        this.specification = specification;

        this.observations = new LinkedHashMap<>();
    }

    public void addObservation(String name, Observation observation) {
        observations.put(name, observation);
    }

    public void add(SequenceExecutionRecord record) {
        records.add(record);
    }

    public List<SequenceExecutionRecord> getRecords() {
        return records;
    }

    public Map<String, Observation> getObservations() {
        return observations;
    }

    public AdaptedImplementation getImplementation() {
        return implementation;
    }

    public InterfaceSpecification getSpecification() {
        return specification;
    }

    public void setRecords(List<SequenceExecutionRecord> records) {
        this.records = records;
    }

    public void setObservations(Map<String, Observation> observations) {
        this.observations = observations;
    }
}
