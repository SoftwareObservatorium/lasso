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
package de.uni_mannheim.swt.lasso.service.notification;

import de.uni_mannheim.swt.lasso.engine.LSLExecutionResult;
import de.uni_mannheim.swt.lasso.engine.LSLScript;
import de.uni_mannheim.swt.lasso.engine.workspace.Workspace;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory methods for pre-built notifications
 * 
 * @author Marcus Kessel
 *
 */
public class NotificationFactory {

    public static final int MAX_READ_LINES_LOG = 100;

    public static Notification onScriptExecutionFinished(
            LSLExecutionResult lslExecutionResult, Workspace workspace) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("userIp", lslExecutionResult.getScript().getIpAddress());
        model.put("executionId", lslExecutionResult.getExecutionId());

        Notification notification = new Notification();
        notification.setToMailAddress(lslExecutionResult.getScript().getEmail());
        notification.setModel(model);
        notification.setTemplateId("lsl_script_finished.ftl");
        notification.setSubject("Your LSL Script Execution Has Finished");

        // set attachments (test class source)
        Attachment scriptAttachment = new Attachment();
        scriptAttachment.setContentType("text/plain;charset=UTF-8");
        scriptAttachment.setFileName("yourscript.lasso");
        if(lslExecutionResult.getScript().getContent() == null) {
            scriptAttachment.setData("empty");
        } else {
            scriptAttachment.setData(lslExecutionResult.getScript().getContent());
        }

        // TODO truncate after X lines
        Attachment logAttachment = new Attachment();
        logAttachment.setContentType("text/plain;charset=UTF-8");
        logAttachment.setFileName("log.txt");
        logAttachment.setData(readLog(workspace));

        notification.setAttachments(Arrays.asList(scriptAttachment, logAttachment));

        return notification;
    }

    public static Notification onScriptExecutionFailed(
            LSLScript script, Workspace workspace) throws IOException {
        Map<String, Object> model = new HashMap<>();
        model.put("userIp", script.getIpAddress());
        model.put("executionId", script.getExecutionId());

        Notification notification = new Notification();
        notification.setToMailAddress(script.getEmail());
        notification.setModel(model);
        notification.setTemplateId("lsl_script_failed.ftl");
        notification.setSubject("Your LSL Script Execution Has Failed");

        // set attachments (test class source)
        Attachment scriptAttachment = new Attachment();
        scriptAttachment.setContentType("text/plain;charset=UTF-8");
        scriptAttachment.setFileName("yourscript.lasso");
        if(script.getContent() == null) {
            scriptAttachment.setData("empty");
        } else {
            scriptAttachment.setData(script.getContent());
        }

        // TODO truncate after X lines
        Attachment logAttachment = new Attachment();
        logAttachment.setContentType("text/plain;charset=UTF-8");
        logAttachment.setFileName("log.txt");
        logAttachment.setData(readLog(workspace));

        notification.setAttachments(Arrays.asList(scriptAttachment, logAttachment));

        return notification;
    }

    private static String readLog(Workspace workspace) throws IOException {
        if(workspace.fileExists(Workspace.SCRIPT_EXECUTION_LOG)) {
            String truncated = "# first 100 Lines" + System.lineSeparator();
            return truncated + workspace.readLines(Workspace.SCRIPT_EXECUTION_LOG, MAX_READ_LINES_LOG);
        } else {
            return "empty";
        }
    }
}
