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

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

/**
 * A method result.
 *
 * @author Marcus Kessel
 */
public class MethodResult {
    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public List<Match> getMatches() {
        return matches;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
    }

    public List<Member> getMethods() {
        return methods;
    }

    public void setMethods(List<Member> methods) {
        this.methods = methods;
    }

    // method matches
    List<Candidate> candidates = new ArrayList<Candidate>();
    List<Match> matches = new ArrayList<Match>();

    // 1. step: find direct method matches
    List<Member> methods = null;
}
