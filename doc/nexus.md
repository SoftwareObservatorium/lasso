# Nexus and LASSO

This short guide demonstrates how to set up a new artifact repository as part of LASSO's executable corpus.

Important: Possible security risks are not taken into consideration, so do not expose your instances.

## Quickstart guide (docker)

This guide assumes that a working docker installation (user space level, see https://docs.docker.com/engine/install/linux-postinstall/) is present on the local machine.

Important: Possible security risks are not taken into consideration, so do not expose your instances.

For this guide, we use the official docker image of Sonatype's Nexus OSS (see https://hub.docker.com/r/sonatype/nexus3)

```bash
# start nexus in a container
docker run -d -p 8081:8081 --name nexus sonatype/nexus3

# NOTE: be patient (!), nexus takes some time to start

# get password for user 'admin'
# (you need to change in the dashboard after the first login)
docker exec -it nexus bash
cat sonatype-work/nexus3/admin.password
# terminate bash - ctrl-d
```

Tested with _Sonatype Nexus OSS 3.65.0-02_.

### Nexus configuration

Open your web browser and go to http://localhost:8081/ and login as 'admin' using the aforementioned password.

After a successful login, Nexus starts a quick wizard. Make sure to enable anonymous access.

### Deployment of subject artifacts within LASSO

Actions like `GenerativeAI` and `GitImport` assume a (Nexus) artifact repository to be present for deployment of artifacts under analysis (on-the-fly). 

This requires the presence of a repository in which artifacts can be deployed.

1. Log in to your Nexus Repository manager using your admin account (e.g., http://localhost:8081/)
2. Go to http://localhost:8081/#admin/repository/repositories
3. Click `Create repository`
4. Choose repository type `maven2 (hosted)`
5. Assign a unique identifier (e.g., `lasso-deploy`)
6. Set version policy to `mixed`, layout policy to `permissive`, and deployment policy to `allow redeploy`
7. Go to http://localhost:8081/#admin/repository/repositories:maven-public and add your newly created repository to _Members_, so we can retrieve deloyed artifacts via the _maven-public_ repository

Change the deployment url, user and password in [corpus.json](lasso_config%2Fcorpus.json) (requires new LASSO service instance!)

```json
  "artifactRepository": {
    "id": "lasso_quickstart_nexus",
    "name": "Quickstart nexus",
    "url": "http://localhost:8081/repository/maven-public/",
    "deploymentUrl": "http://localhost:8081/repository/lasso-deploy/",
    "user": "XXX",
    "pass": "XXX",
    "description": "quickstart repository of LASSO"
  }
```

You have to restart LASSO with the updated `corpus.json` configuration file. It might be necessary to update LASSO's Maven configuration in `lasso-work/repository/settings.xml` as well (alternatively, change or removing LASSO's existing work directory).

Note: For advanced use cases with LASSO, setting up a deployment _user_ is a better choice instead of using the admin user.

### Deployment of LASSO's support libraries

Certain features of LASSO rely on deployed support libraries. To deploy them (see configuration above), run the following command to deploy all LASSO related artifacts to your local Nexus repository

```bash
# set your path to LASSO's repository
./mvnw -s doc/nexus_config/settings_plugin_deploy.xml -gs doc/nexus_config/settings_plugin_deploy.xml -DskipTests -Dfrontend.build=embedded -DaltDeploymentRepository=lasso_quickstart_nexus::default::http://localhost:8081/repository/lasso-deploy/ -DaltReleaseDeploymentRepository=lasso_quickstart_nexus::default::http://localhost:8081/repository/lasso-deploy/ -DaltSnapshotDeploymentRepository=lasso_quickstart_nexus::default::http://localhost:8081/repository/lasso-deploy/ deploy
```

Note: You need to change the password in `doc/nexus_config/settings_plugin_deploy.xml` to your custom Nexus password.