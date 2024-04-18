# Development


### Configuring LASSO

For further configuration options (i.e., system properties like `-Dlasso.workspace.root=/some/path`) see:
* [service application.properties](..%2Fservice%2Fsrc%2Fmain%2Fresources%2Fapplication.properties)
* [worker application.properties](..%2Fworker%2Fsrc%2Fmain%2Fresources%2Fapplication.properties)

### Development & Debugging (IntelliJ IDEA)

1. Clone this repository
2. Click `File->Open` select your cloned repository to import all modules

#### Run LASSO

3. Run LASSO platform in embedded (standalone) mode (`service` module):

```
# Edit run configuration in your IDE to
de.uni_mannheim.swt.lasso.service.app.LassoApplication
```

##### Embedded Mode

* _-Dcluster.embedded=true_ - sets the platfrom into standalone mode in which a local worker node is started in addition to the manager node
* _-Dusers=classpath:/users.json_ - points to user accounts
* _-Dcorpus=classpath:/corpus.json_ - points to the current executable corpus configurations

##### code2vec

If text-based search is used, the following method name vectors from _code2vec_ need to be present

* [code2vec](https://github.com/tech-srl/code2vec) -> https://s3.amazonaws.com/code2vec/model/target_vecs.tar.gz
* _-Dmodels.embedding.code2vec=/home/marcus/Downloads/target_vecs.txt_

#### Testing LASSO

Note: Depending on the module, unit tests are either written in JUnit4 or JUnit5.

Action, script and system (integration tests) are located in module `service`:

* `de.uni_mannheim.swt.lasso.service.systemtests.integration`

Note: Some packages come with their own set of tests.

Note: Integration tests run in embedded mode (manager + worker nodes are deployed on the same machine).

Make sure that the following properties are passed (see above)

* _-Dusers=classpath:/users.json_ - points to user accounts
* _-Dcorpus=classpath:/corpus.json_ - points to the current executable corpus configurations


##### Running Ignite with Java 11 or later

see https://ignite.apache.org/docs/latest/quick-start/java#running-ignite-with-java-11-or-later

```bash
--add-opens=java.base/jdk.internal.access=ALL-UNNAMED
--add-opens=java.base/jdk.internal.misc=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/sun.util.calendar=ALL-UNNAMED
--add-opens=java.management/com.sun.jmx.mbeanserver=ALL-UNNAMED
--add-opens=jdk.internal.jvmstat/sun.jvmstat.monitor=ALL-UNNAMED
--add-opens=java.base/sun.reflect.generics.reflectiveObjects=ALL-UNNAMED
--add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.locks=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED
--add-opens=java.base/java.math=ALL-UNNAMED
--add-opens=java.sql/java.sql=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.time=ALL-UNNAMED
--add-opens=java.base/java.text=ALL-UNNAMED
--add-opens=java.management/sun.management=ALL-UNNAMED
--add-opens java.desktop/java.awt.font=ALL-UNNAMED
```

#### Main Integration Points

`engine` module:

* LASSO actions: `de.uni_mannheim.swt.lasso.engine.action.DefaultAction` and `de.uni_mannheim.swt.lasso.engine.action.maven.MavenAction` for Maven-based actions.
* LASSO record collectors: `de.uni_mannheim.swt.lasso.engine.collect.RecordCollector`

Note: Many LASSO Actions and examples of LASSO's Action API can be found in `de.uni_mannheim.swt.lasso.engine.action.*`.

### LASSO (RESTful) API

The API is secured using bearer tokens (jwt + Spring Security).

For interactive API testing, swagger-ui (OpenAPI V3) is available

* http://localhost:10222/swagger-ui/index.html

The RESTful API can be tested directly in the swagger UI. First sign in using _/auth/signin_ to obtain a bearer token and then "Authorize" (top-right button).

### LASSO Database (User/Workspace Model Persistence)

User and workspace descriptions are stored in an embedded database.

* http://localhost:10222/h2-console/
* connect with: _jdbc:h2:~/lasso-work/lasso-db_

## LSL (Lasso Scripting Language)

To enable DAG visualization (for debugging purposes), _graphviz_ is required (used by plantuml):

* _apt install graphviz_

### LASSO Dashboard - Webapp (GUI)

#### LASSO webui (current)

An Angular 16 (https://angular.io/) web application using Material (https://material.angular.io/).

It is located in [webui](..%2Fwebui).

Start the test server with Angular CLI:

```
./webui/search/SERVE.sh
```

LASSO's service URL can be defined in `environments/`.

nodejs and npm are automatically downloaded as part of webui's _pom.xml_.

##### URLs

* `local` - http://localhost:4200
* `remote` - e.g., http://localhost:10222/webui/

##### Out of Memory Issues

On some machines, nodejs may run into out of memory issues, see

* https://github.com/angular/angular-cli/issues/21338

To fix it, modify

* frontend/lasso-app/package.json#L7

and set the following

```json
"build": "node --max_old_space_size=4096 ./node_modules/.bin/ng build"
```