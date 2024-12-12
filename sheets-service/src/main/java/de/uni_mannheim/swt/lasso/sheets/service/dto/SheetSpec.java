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
package de.uni_mannheim.swt.lasso.sheets.service.dto;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 *
 */
public class SheetSpec {

    private String signature;
    private String interfaceSpecification;
    private String body;

    // FIXME add implementation details
    private String implementation;

    private List<String> invocations;

    public String getInterfaceSpecification() {
        return interfaceSpecification;
    }

    public void setInterfaceSpecification(String interfaceSpecification) {
        this.interfaceSpecification = interfaceSpecification;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public List<String> getInvocations() {
        return invocations;
    }

    public void setInvocations(List<String> invocations) {
        this.invocations = invocations;
    }
}
