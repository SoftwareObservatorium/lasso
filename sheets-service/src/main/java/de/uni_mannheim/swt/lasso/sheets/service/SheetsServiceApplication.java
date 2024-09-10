package de.uni_mannheim.swt.lasso.sheets.service;

import de.uni_mannheim.swt.lasso.sheets.service.config.*;
import de.uni_mannheim.swt.lasso.sheets.service.persistence.User;
import de.uni_mannheim.swt.lasso.sheets.service.persistence.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
@EnableAutoConfiguration(exclude = { SolrAutoConfiguration.class })
@ComponentScan(basePackages = { "de.uni_mannheim.swt.lasso.sheets.service" })
@Import({ EngineConfig.class, SecurityConfig.class, WebAppConfig.class, SwaggerConfig.class, JpaConfig.class})
public class SheetsServiceApplication implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(SheetsServiceApplication.class);

	/**
	 * Bootstrap application.
	 *
	 * @param args
	 * @throws InterruptedException
	 */
	public static void main(String[] args) {
		// init Spring boot
		SpringApplication.run(SheetsServiceApplication.class, args);
	}

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	Environment env;

	@Autowired
	ResourceLoader resourceLoader;

	@Override
	public void run(String... args) throws Exception {
		// FIXME setup users should be moved somewhere else
		List<User> users = UserManagementUtils.read(resourceLoader.getResource(env.getProperty("users")).getInputStream(), passwordEncoder);

		// add users to database
		users.forEach(u -> {
			if(!userRepository.existsByUsername(u.getUsername())) {
				userRepository.save(u);
			}
		});

		if(LOG.isDebugEnabled()) {
			userRepository.findAll().forEach(v -> LOG.debug(" User :" + v.toString()));
		}

		if(env.getProperty("cluster.embedded", boolean.class, false)) {
			if(LOG.isInfoEnabled()) {
				LOG.info("Embedded mode enabled");
			}

			//clusterEngine.getIgnite().active(true);
		}
	}

}
