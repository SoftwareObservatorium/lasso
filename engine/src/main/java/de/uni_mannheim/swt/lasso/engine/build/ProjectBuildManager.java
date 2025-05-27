package de.uni_mannheim.swt.lasso.engine.build;

import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.ActionConfiguration;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.engine.LSLExecutionContext;
import de.uni_mannheim.swt.lasso.engine.action.DefaultAction;

import java.util.List;

/**
 *
 * @author Marcus Kessel
 */
public interface ProjectBuildManager {

    void store(DefaultAction action, LSLExecutionContext context, ActionConfiguration actionConfiguration, Abstraction abstraction, List<CodeUnit> units, ProjectBuildConfiguration buildConfiguration);

    CodeUnit parse(String code, String namespace);
}
