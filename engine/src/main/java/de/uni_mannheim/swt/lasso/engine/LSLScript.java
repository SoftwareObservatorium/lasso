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
package de.uni_mannheim.swt.lasso.engine;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.uni_mannheim.swt.lasso.lsl.LSLLogger;

import java.util.Objects;

/**
 *
 * @author Marcus Kessel
 *
 */
public class LSLScript {

    private String content;

    private String executionId;

    private String email;
    private String ipAddress;

    @JsonIgnore
    private LSLLogger logger;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LSLScript script = (LSLScript) o;
        return Objects.equals(content, script.content) &&
                Objects.equals(executionId, script.executionId) &&
                Objects.equals(email, script.email) &&
                Objects.equals(ipAddress, script.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, executionId, email, ipAddress);
    }

    @Override
    public String toString() {
        return "LSLScript{" +
                "content='" + content + '\'' +
                ", executionId='" + executionId + '\'' +
                ", email='" + email + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }

    public LSLLogger getLogger() {
        return logger;
    }

    public void setLogger(LSLLogger logger) {
        this.logger = logger;
    }
}
