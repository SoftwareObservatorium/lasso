package de.uni_mannheim.swt.lasso.engine.action.arena;

import de.uni_mannheim.swt.lasso.cluster.client.ArenaJob;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.Systems;
import de.uni_mannheim.swt.lasso.corpus.ExecutableCorpus;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;
import de.uni_mannheim.swt.lasso.engine.environment.ExecutionEnvironment;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public interface ArenaProjectManager extends ProjectManager {

    Systems initNew(DefaultAction action, String actionInstanceId, Abstraction abstraction, String pomTemplate, ProjectManager.ProjectSettingsHandler mavenProjectPomHandler, ProjectManager.ExecutableFilter executableFilter) throws IOException;

    ExecutionEnvironment createExecutionEnvironment(DefaultAction action, ActionConfiguration actionConfiguration, ArenaJob job, ExecutableCorpus corpus, String task, List<String> features, long containerTimeout, long implementationTimeout, int threads);

    LSLExecutionContext getLslExecutionContext();

    Workspace getWorkspace();

    File getRepository();
}
