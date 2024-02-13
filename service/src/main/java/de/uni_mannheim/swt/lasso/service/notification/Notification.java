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

import java.util.List;
import java.util.Map;

/**
 * Notification model
 * 
 * @author Marcus Kessel
 *
 */
public class Notification {

    /**
     * To e-mail address
     */
    private String toMailAddress;

    /**
     * Mail template
     */
    private String templateId;

    /**
     * Mail template model
     */
    private Map<String, Object> model;

    /**
     * Attachments
     */
    private List<Attachment> attachments;
    
    /**
     * Subject
     */
    private String subject;

    /**
     * @return the toMailAddress
     */
    public String getToMailAddress() {
        return toMailAddress;
    }

    /**
     * @param toMailAddress
     *            the toMailAddress to set
     */
    public void setToMailAddress(String toMailAddress) {
        this.toMailAddress = toMailAddress;
    }

    /**
     * @return the templateId
     */
    public String getTemplateId() {
        return templateId;
    }

    /**
     * @param templateId
     *            the templateId to set
     */
    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    /**
     * @return the model
     */
    public Map<String, Object> getModel() {
        return model;
    }

    /**
     * @param model
     *            the model to set
     */
    public void setModel(Map<String, Object> model) {
        this.model = model;
    }

    /**
     * @return the attachments
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

    /**
     * @param attachments the attachments to set
     */
    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
}
