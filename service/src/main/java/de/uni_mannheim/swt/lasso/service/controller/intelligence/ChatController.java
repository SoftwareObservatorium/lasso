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
package de.uni_mannheim.swt.lasso.service.controller.intelligence;

import de.uni_mannheim.swt.lasso.cluster.ClusterEngine;
import de.uni_mannheim.swt.lasso.core.dto.ChatRequest;
import de.uni_mannheim.swt.lasso.core.dto.ChatResponse;
import de.uni_mannheim.swt.lasso.core.dto.SearchRequestResponse;
import de.uni_mannheim.swt.lasso.engine.LassoConfiguration;
import de.uni_mannheim.swt.lasso.intelligence.RagService;
import de.uni_mannheim.swt.lasso.service.controller.BaseApi;
import de.uni_mannheim.swt.lasso.service.controller.ds.query.QueryStrategy;
import de.uni_mannheim.swt.lasso.service.controller.ds.query.ScriptQueryStrategy;
import de.uni_mannheim.swt.lasso.service.controller.ds.query.TextualQueryStrategy;
import de.uni_mannheim.swt.lasso.service.dto.UserInfo;
import de.uni_mannheim.swt.lasso.srm.SRHRepository;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import joinery.DataFrame;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Lasso Chat API (ask questions about result sets; e.g., in a code search)
 *
 * @author Marcus Kessel
 */
@RestController
@RequestMapping(value = "/api/v1/lasso/chat")
@Tag(name = "LASSO Chat API")
public class ChatController extends BaseApi {

    private static final Logger LOG = LoggerFactory
            .getLogger(ChatController.class);

    @Autowired
    private Environment env;
    @Autowired
    private LassoConfiguration lassoConfiguration;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SRHRepository srhRepository;

    @Autowired
    ClusterEngine clusterEngine;

    /**
     * Retrieve result again and pass it to assistant
     *
     * @param request
     * @param dataSource
     * @param userDetails
     * @param httpServletRequest
     * @param httpServletResponse
     * @return
     */
    @Operation(summary = "Ask questions about implementations", description = "Ask questions about implementations")
    @RequestMapping(value = "/code/{dataSource}/ask", method = RequestMethod.POST, consumes = "application/json;charset=UTF-8", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<ChatResponse> askCodeUnits(
            @RequestBody ChatRequest request,
            @PathVariable("dataSource") String dataSource,
            /*@ApiIgnore*/ @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        // get user details
        UserInfo userInfo = getUserInfo(httpServletRequest, userDetails);

        SearchRequestResponse response = new SearchRequestResponse();

        try {
            // just show systems of given script execution
            if(StringUtils.isNotBlank(request.getSearchQueryRequest().getExecutionId())) {
                //
                QueryStrategy queryStrategy = new ScriptQueryStrategy(clusterEngine, lassoConfiguration);
                response = queryStrategy.query(request.getSearchQueryRequest(), dataSource);
            } else {
                // classic search
                QueryStrategy queryStrategy = new TextualQueryStrategy(clusterEngine, lassoConfiguration);
                response = queryStrategy.query(request.getSearchQueryRequest(), dataSource);
            }

            // pass to assistant
            ChatResponse chatResponse = askAssistantForCodeUnits(request, response);

            if (LOG.isInfoEnabled()) {
                LOG.info("Returning askCodeUnits response to '{}'",
                        userInfo.getRemoteIpAddress());
            }

            // 200
            return ResponseEntity.ok(chatResponse);
        } catch (Throwable e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("Could not askCodeUnits response"), e);
            }

            // bad request
            throw new RuntimeException(String.format("Could not askCodeUnits response"), e);
        }
    }

    private ChatResponse askAssistantForCodeUnits(ChatRequest chatRequest, SearchRequestResponse response) {
        OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                .baseUrl(env.getProperty("intelligence.ollama.url"))
                .modelName(chatRequest.getModelName())
                .temperature(chatRequest.getTemperature())
                .build();

        DataFrame srm = null;
        if(StringUtils.isNotBlank(chatRequest.getSearchQueryRequest().getExecutionId())) {
            // FIXME arena id (let's assume arena "execute" for now)
            try {
                srm = srhRepository.getActuationSheets(chatRequest.getSearchQueryRequest().getExecutionId(),
                        SRHRepository.ARENA_DEFAULT, SRHRepository.TYPE_VALUE, chatRequest.getSearchQueryRequest().getOracleFilters());

                // FIXME pass to RAG + original sequence sheet?

                // re-construct actuation sheet
                // TODO input_value for actual inputs (but here it is not statements, but multiple input parameters .. so should be concatenated
                // TODO op


                // Stimulus Sheet (written against SPEC) - input->operation->output
                // Actuation Sheet (written against IMPL)
            } catch (Throwable e) {
                LOG.warn("Getting SRM failed", e);
            }

        }

        RagService ragService = new RagService(ollamaChatModel);
        RagService.Assistant assistant = ragService.create(new ArrayList<>(response.getImplementations().values()), srm);
        Result<String> answer = assistant.chat(chatRequest.getMessage());

        if(LOG.isDebugEnabled()) {
            LOG.debug(answer.content());
        }

        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setContent(answer.content());
        chatResponse.setMessage(chatRequest.getMessage());

        return chatResponse;
    }
}
