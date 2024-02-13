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

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Notification service impl.
 * 
 * @author Marcus Kessel
 *
 */
public class NotificationService {

    private static final Logger LOG = LoggerFactory
            .getLogger(NotificationService.class);

    private final JavaMailSender javaMailSender;
    private final Configuration freemarkerConfiguration;

    private final String fromAddress;

    /**
     * Constructor
     * 
     * @param javaMailSender
     *            {@link JavaMailSender} instance
     * @param freemarkerConfiguration
     *            {@link Configuration} instance
     * @param fromAddress
     *            From email address
     */
    public NotificationService(JavaMailSender javaMailSender,
                               Configuration freemarkerConfiguration, String fromAddress) {
        this.javaMailSender = javaMailSender;
        this.freemarkerConfiguration = freemarkerConfiguration;

        // validate
        isValidAddress(fromAddress);
        this.fromAddress = fromAddress;
    }

    /**
     * Send notification
     * 
     * @param notification
     *            {@link Notification} instance
     * @throws IOException
     *             Template/Send error
     */
    public void send(Notification notification) throws IOException {
        // validate mail address
        Validate.isTrue(isValidAddress(notification.getToMailAddress()),
                "Email address invalid: %s", notification.getToMailAddress());
        Validate.notNull(notification.getModel(),
                "Template model cannot be null");

        // create mime message
        MimeMessage mimeMessage;
        try {
            mimeMessage = createMimeMessage(notification);
        } catch (Throwable e) {
            //
            throw new IOException("Could not prepare template", e);
        }

        // send
        try {
            javaMailSender.send(mimeMessage);

            if (LOG.isInfoEnabled()) {
                LOG.info("Notification mail sent = "
                        + ToStringBuilder.reflectionToString(notification));
            }
        } catch (Throwable e) {
            //
            throw new IOException("Could not send mail", e);
        }
    }

    /**
     * @param emailAddress
     *            Email address
     * @return true if given email address is valid
     * 
     * @see EmailValidator
     */
    public boolean isValidAddress(String emailAddress) {
        return EmailValidator.getInstance().isValid(emailAddress);
    }

    /**
     * Prepare {@link MimeMessage} for sending
     * 
     * @param notification
     *            {@link Notification} instance
     * @return {@link MimeMessage} instance
     * @throws TemplateNotFoundException
     *             Error
     * @throws MalformedTemplateNameException
     *             Error
     * @throws ParseException
     *             Error
     * @throws IOException
     *             Error
     * @throws TemplateException
     *             Error
     * @throws MessagingException
     *             Error
     */
    private MimeMessage createMimeMessage(Notification notification)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException, TemplateException, MessagingException {
        // default message
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();

        // construct mail message
        MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage,
                true/* multipart */, Charset.forName("UTF-8").displayName());

        // from
        messageHelper.setFrom(fromAddress);

        // to
        messageHelper.addTo(notification.getToMailAddress());

        // subject
        messageHelper.setSubject(notification.getSubject());

        // body text
        String text = FreeMarkerTemplateUtils.processTemplateIntoString(
                freemarkerConfiguration.getTemplate(notification
                        .getTemplateId(), Charset.forName("UTF-8").name()),
                notification.getModel());

        messageHelper.setText(text, false);

        // add attachments
        if (CollectionUtils.isNotEmpty(notification.getAttachments())) {
            for (Attachment attachment : notification.getAttachments()) {
                messageHelper.addAttachment(attachment.getFileName(),
                        new ByteArrayDataSource(attachment.getData(),
                                attachment.getContentType()));
            }
        }

        return mimeMessage;
    }
}
