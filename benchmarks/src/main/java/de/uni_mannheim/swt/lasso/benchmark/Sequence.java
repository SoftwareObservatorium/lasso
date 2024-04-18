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
package de.uni_mannheim.swt.lasso.benchmark;

import com.google.gson.Gson;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple sequence of statements
 *
 * @author Marcus Kessel
 */
public class Sequence {

    private String id;

    private String description;

    private List<Statement> statements;

    public void addStatement(Statement statement) {
        if(statements == null) {
            statements = new LinkedList<>();
        }

        statements.add(statement);
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void setStatements(List<Statement> sequence) {
        this.statements = sequence;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(Statement stmt : getStatements()) {
            sb.append(stmt.getInputs().stream().map(Value::getCode).collect(Collectors.joining(", ")));
            sb.append(" -> ");
            sb.append(stmt.getExpectedOutputs().stream().map(Value::getCode).collect(Collectors.joining(",")));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String toSequenceString() {
        Gson gson = new Gson();
        StringBuilder sb = new StringBuilder();
        for(Statement stmt : getStatements()) {
            sb.append(stmt.getOperation());
            sb.append("(");
            sb.append(stmt.getInputs().stream().map(Value::getValue)
                    .map(gson::toJson)
                    .collect(Collectors.joining(", ")));
            sb.append(")");
            sb.append("->");
            sb.append(stmt.getExpectedOutputs().stream()
                    .map(Value::getValue)
                    .map(gson::toJson)
                    .collect(Collectors.joining(",")));
            sb.append("\n");
        }

        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
