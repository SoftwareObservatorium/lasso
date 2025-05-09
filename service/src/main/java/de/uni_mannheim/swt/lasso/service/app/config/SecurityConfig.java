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
package de.uni_mannheim.swt.lasso.service.app.config;

import de.uni_mannheim.swt.lasso.service.app.config.security.JwtTokenFilter;
import de.uni_mannheim.swt.lasso.service.controller.ExceptionHandlerFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Web security
 *
 * @author Marcus Kessel
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true, jsr250Enabled = true)
public class SecurityConfig /*extends WebSecurityConfigurerAdapter*/ {

    @Autowired
    private JwtTokenFilter jwtTokenFilter;

    @Autowired
    private UserDetailsService userDetailsService;

//    @Bean
//    //@Override
//    public AuthenticationManager authenticationManagerBean() throws Exception {
//        return super.authenticationManagerBean();
//    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("authorization", "content-type", "x-auth-token"));
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    //@Override
//    @Bean
//    protected JwtConfigurer configure(HttpSecurity http) throws Exception {
//        return http.cors().and()
//                .csrf().disable()
//                .httpBasic().disable()
//                // https://stackoverflow.com/questions/26220083/h2-database-console-spring-boot-load-denied-by-x-frame-options
//                .headers().frameOptions().sameOrigin().and()
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
//                .authorizeRequests()
//                .requestMatchers("/auth/signin").permitAll()
//                .requestMatchers("/api/**").hasRole("USER")
//
//                // FIXME permit ZIP downloads
//                //.antMatchers("/api/**/records").permitAll()
//
//                // permit new web app (angular webui)
//                .requestMatchers("/webui/**").permitAll()
//
//                // GraphQL FIXME remove
//                .requestMatchers("/graphql").hasRole("USER")
//                .requestMatchers("/graphiql").permitAll()
//
//                // FIXME needs public access
//                .requestMatchers("/publicapi/v1/lasso/analytics/**").permitAll()
//                .requestMatchers("/notebooks/**").permitAll()
//
//                // start swagger
//                .requestMatchers("/swagger-ui.html").permitAll()
//                //.antMatchers("/swagger-resources/**").permitAll()
//                //.antMatchers("/configuration/**").permitAll()
//                .requestMatchers("/v3/api-docs/**").permitAll()
//                .requestMatchers("/api-docs/**").permitAll()
//                .requestMatchers("/swagger-ui/**").permitAll()
//                // end swagger
//                .requestMatchers("/webjars/**").permitAll()
//                .requestMatchers("/docs/**").permitAll()
//                .requestMatchers("/h2-console/**").permitAll()
//                .anyRequest().authenticated()
//                .and()
//                .apply(new JwtConfigurer(jwtTokenProvider));
//    }

    @Bean
    protected SecurityFilterChain configure(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Apply CORS
                .csrf(csrf -> csrf.disable()) // Disable CSRF protection
                //.cors().and()
                //.csrf().disable()
                .httpBasic(hb -> hb.disable())
                // https://stackoverflow.com/questions/26220083/h2-database-console-spring-boot-load-denied-by-x-frame-options
                //.headers().frameOptions().sameOrigin().and()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/signin").permitAll()
                        .requestMatchers("/api/**").hasRole("USER")

                        // FIXME permit ZIP downloads
                        //.antMatchers("/api/**/records").permitAll()

                        // permit new web app (angular webui)
                        .requestMatchers("/webui/**").permitAll()

                        // GraphQL FIXME remove
                        .requestMatchers("/graphql").hasRole("USER")
                        .requestMatchers("/graphiql").permitAll()

                        // FIXME needs public access
                        .requestMatchers("/publicapi/v1/lasso/analytics/**").permitAll()
                        .requestMatchers("/notebooks/**").permitAll()

                        // start swagger
                        .requestMatchers("/swagger-ui.html").permitAll()
                        //.antMatchers("/swagger-resources/**").permitAll()
                        //.antMatchers("/configuration/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        // end swagger
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/docs/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated())
//                .and()
//                .apply(new JwtConfigurer(jwtTokenProvider));
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                //.and()
                .authenticationProvider(authenticationProvider()) // Register the authentication provider
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class) // Add the JWT filter before processing the request
                // add exception filter
                .addFilterBefore(new ExceptionHandlerFilter(), JwtTokenFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setUserDetailsService(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
