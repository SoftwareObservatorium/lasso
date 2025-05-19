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
package de.uni_mannheim.swt.lasso.service.persistence;

import jakarta.persistence.*;
import java.util.Date;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
@Entity
@Table(name = "lasso_script_jobs")
public class ScriptJob extends LassoEntity {

    @Column(name="execution_id", nullable = false)
    private String executionId;

    @Column(name="name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(name="status")
    private ScriptJobStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date start;

    @Temporal(TemporalType.TIMESTAMP)
    private Date end;

    @Column(name = "content", columnDefinition="CLOB")
    private String content;

    @Column(name="label")
    private String label;
    @Column(name="description", columnDefinition="CLOB")
    private String description;

    @ElementCollection
    @CollectionTable(name = "script_job_tags") // Adjust table name
    @Column(name = "tag")  // Column name for the tag value
    private List<String> tags;

    @Column(name = "permission_type")
    @Enumerated(EnumType.STRING)
    private JobPermissionType permissionType;

    @OneToMany(mappedBy = "scriptJob", fetch = FetchType.LAZY)
    private List<ScriptJobAllowedUser> allowedUsers;

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public ScriptJobStatus getStatus() {
        return status;
    }

    public void setStatus(ScriptJobStatus status) {
        this.status = status;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getEnd() {
        return end;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public JobPermissionType getPermissionType() {
        return permissionType;
    }

    public void setPermissionType(JobPermissionType permissionType) {
        this.permissionType = permissionType;
    }

    public List<ScriptJobAllowedUser> getAllowedUsers() {
        return allowedUsers;
    }

    public void setAllowedUsers(List<ScriptJobAllowedUser> allowedUsers) {
        this.allowedUsers = allowedUsers;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}
