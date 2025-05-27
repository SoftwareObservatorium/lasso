package de.uni_mannheim.swt.lasso.engine.build;

import de.uni_mannheim.swt.lasso.corpus.Datasource;

import java.util.Map;

/**
 *
 * @author Marcus Kessel
 */
public class ProjectBuildConfiguration {

    private String id;

    private String repoUrl;
    private String repoId;
    private boolean deploy;

    private String langVersion;

    private String groupId;
    private String artifactId;
    private String version;

    private String projectTemplate;

    private Map<String, String> meta;

    private Datasource dataSource;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public boolean isDeploy() {
        return deploy;
    }

    public void setDeploy(boolean deploy) {
        this.deploy = deploy;
    }

    public String getLangVersion() {
        return langVersion;
    }

    public void setLangVersion(String langVersion) {
        this.langVersion = langVersion;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public String getProjectTemplate() {
        return projectTemplate;
    }

    public void setProjectTemplate(String projectTemplate) {
        this.projectTemplate = projectTemplate;
    }

    public Datasource getDataSource() {
        return dataSource;
    }

    public void setDataSource(Datasource dataSource) {
        this.dataSource = dataSource;
    }
}
