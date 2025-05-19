package de.uni_mannheim.swt.lasso.service.persistence;

import jakarta.persistence.*;

import java.util.Date;

/**
 *
 * @author Marcus Kessel
 */
@Entity
@Table(name = "lasso_script_job_permissions")
public class ScriptJobAllowedUser {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "script_id", nullable = false)
    private ScriptJob scriptJob;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Temporal(TemporalType.TIMESTAMP)
    private Date created;
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModified;

    public ScriptJob getScriptJob() {
        return scriptJob;
    }

    public void setScriptJob(ScriptJob scriptJob) {
        this.scriptJob = scriptJob;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}