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

import java.util.List;

/**
 * An "adaptee" permutation (unique combination of methods)
 * 
 * @author Marcus Kessel
 * 
 */
public class Permutation {

    private int id;
    
    private Candidate constructor;
    private List<Candidate> methods;
    
    private Double nameScore;

    public Permutation(Candidate constructor, List<Candidate> methods) {
        this.constructor = constructor;
        this.methods = methods;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the methods
     */
    public List<Candidate> getMethods() {
        return methods;
    }

    /**
     * @return the constructor
     */
    public Candidate getConstructor() {
        return constructor;
    }

    /**
     * @return the nameScore
     */
    public Double getNameScore() {
        return nameScore;
    }

    /**
     * @param nameScore the nameScore to set
     */
    public void setNameScore(Double nameScore) {
        this.nameScore = nameScore;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((constructor == null) ? 0 : constructor.hashCode());
        result = prime * result + ((methods == null) ? 0 : methods.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Permutation other = (Permutation) obj;
        if (constructor == null) {
            if (other.constructor != null)
                return false;
        } else if (!constructor.equals(other.constructor))
            return false;
        if (methods == null) {
            if (other.methods != null)
                return false;
        } else if (!methods.equals(other.methods))
            return false;

        return true;
    }

    @Override
    public String toString() {
        return "Permutation{" +
                "id=" + id +
                ", constructor=" + constructor +
                ", methods=" + methods +
                ", nameScore=" + nameScore +
                '}';
    }
}
