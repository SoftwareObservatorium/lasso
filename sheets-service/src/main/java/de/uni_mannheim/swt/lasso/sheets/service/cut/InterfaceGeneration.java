package de.uni_mannheim.swt.lasso.sheets.service.cut;

import de.uni_mannheim.swt.lasso.arena.sequence.sheetengine.interpreter.util.LQLUtils;
import de.uni_mannheim.swt.lasso.gai.openai.util.ContentParser;
import de.uni_mannheim.swt.lasso.lql.parser.LQLParseResult;
import de.uni_mannheim.swt.lasso.sheets.service.dto.LQLGenerationRequest;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 * @author Marcus Kessel
 */
public class InterfaceGeneration {

    private static final Logger LOG = LoggerFactory
            .getLogger(InterfaceGeneration.class);

    private final String ollamaBaseUrl;

    public InterfaceGeneration(String ollamaBaseUrl) {
        this.ollamaBaseUrl = ollamaBaseUrl;
    }

    public String promptOllama(LQLGenerationRequest request) {
        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(request.getModel())
                //.temperature(0.7)
                .build();

        String chatResponse = ollamaChatModel.generate(request.getPrompt());

        ContentParser contentParser = new ContentParser();
        List<String> generatedCode = contentParser.extractCode(chatResponse, "java");

        String lql = StringUtils.trim(generatedCode.get(0));

        LOG.debug("LQL generated\n{}", lql);

        // FIXME validate syntax with parser
        try {
            LQLParseResult parseResult = LQLUtils.parseLQL(lql);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return lql;
    }
}
