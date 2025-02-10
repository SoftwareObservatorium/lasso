package de.uni_mannheim.swt.lasso.sheets.service.cut;

import de.uni_mannheim.swt.lasso.arena.CandidatePool;
import de.uni_mannheim.swt.lasso.arena.ClassUnderTest;
import de.uni_mannheim.swt.lasso.arena.repository.MavenRepository;
import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.DynamicClassLoader;
import de.uni_mannheim.swt.lasso.core.model.CodeUnit;
import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import de.uni_mannheim.swt.lasso.sheets.service.dto.ClassUnderTestSpec;
import de.uni_mannheim.swt.lasso.sheets.service.dto.CodeGenerationRequest;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class CodeGeneration {

    private static final Logger LOG = LoggerFactory
            .getLogger(CodeGeneration.class);

    private final MavenRepository mavenRepository;
    private final String ollamaBaseUrl;

    private Map<String, CodeUnit> cachedClasses = new HashMap<>();

    public CodeGeneration(MavenRepository mavenRepository, String ollamaBaseUrl) {
        this.mavenRepository = mavenRepository;
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public List<ClassUnderTestSpec> promptOllama(CodeGenerationRequest request) {
        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(request.getModel())
                //.temperature(0.7)
                .build();

        String chatResponse = ollamaChatModel.generate(request.getPrompt());

        ContentParser contentParser = new ContentParser();
        List<String> generatedCode = contentParser.extractCode(chatResponse);

        // useful package names (human readable)
        String pkg = request.getModel().replaceAll("\\W", "");

        List<ClassUnderTestSpec> classesUnderTest = new LinkedList<>();
        for(String sourceCode : generatedCode) {
            LOG.debug("code generated\n{}", sourceCode);

            try {
                ClassUnderTest classUnderTest = DynamicClassLoader.loadBySource(sourceCode, pkg);

                // try if class can be loaded
                CandidatePool pool = new CandidatePool(mavenRepository, new ArrayList<>(Arrays.asList(classUnderTest)));
                pool.initProjects();
                classUnderTest.getProject().getContainer().loadClass(classUnderTest.getClassName());

                CodeUnit codeUnit = classUnderTest.getImplementation().getCode();
                // cache
                cachedClasses.put(classUnderTest.getId(), codeUnit);
                // add
                ClassUnderTestSpec classUnderTestSpec = new ClassUnderTestSpec();
                classUnderTestSpec.setId(classUnderTest.getId());
                classUnderTestSpec.setClassName(classUnderTest.getClassName());
                //classUnderTestSpec.setArtifacts(Arrays.asList(codeUnit.toUri()));
                classUnderTestSpec.setArtifacts(new LinkedList<>());
                classUnderTestSpec.setCodeUnit(codeUnit);
                classUnderTestSpec.setCodeGenerationId(classUnderTest.getId());

                classesUnderTest.add(classUnderTestSpec);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        return classesUnderTest;
    }

    public ClassUnderTest toClassUnderTest(String id) throws IOException {
        if(!cachedClasses.containsKey(id)) {
            throw new NullPointerException("id does not exist");
        }

        CodeUnit codeUnit = getClass(id);
        ClassUnderTest classUnderTest = DynamicClassLoader.loadBySource(codeUnit);
        return classUnderTest;
    }

    public CodeUnit getClass(String id) {
        return cachedClasses.get(id);
    }
}
