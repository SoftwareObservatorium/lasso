package de.uni_mannheim.swt.lasso.sheets.service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 *
 * @author Marcus Kessel
 */
@Configuration
public class WebAppConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // redirect to sheets-ui
        registry.addRedirectViewController("/", "index.html");
    }

}