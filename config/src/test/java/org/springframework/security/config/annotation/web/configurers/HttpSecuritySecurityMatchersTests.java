/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.config.annotation.web.configurers;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletResponse;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.web.AbstractRequestMatcherRegistry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author Rob Winch
 *
 */
public class HttpSecuritySecurityMatchersTests {

	AnnotationConfigWebApplicationContext context;

	MockHttpServletRequest request;

	MockHttpServletResponse response;

	MockFilterChain chain;

	@Autowired
	FilterChainProxy springSecurityFilterChain;

	@BeforeEach
	public void setup() throws Exception {
		this.request = new MockHttpServletRequest("GET", "");
		this.request.setMethod("GET");
		this.response = new MockHttpServletResponse();
		this.chain = new MockFilterChain();
		mockMvcPresentClasspath(true);
	}

	@AfterEach
	public void cleanup() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void securityMatcherWhenMvcThenMvcMatcher() throws Exception {
		loadConfig(SecurityMatcherMvcConfig.class, LegacyMvcMatchingConfig.class);
		this.request.setServletPath("/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("/path.html");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("/path/");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	public void securityMatcherWhenNoMvcThenAntMatcher() throws Exception {
		mockMvcPresentClasspath(false);
		loadConfig(SecurityMatcherNoMvcConfig.class, LegacyMvcMatchingConfig.class);
		this.request.setServletPath("/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("/path.html");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		setup();
		this.request.setServletPath("/path/");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void securityMatcherWhenMvcMatcherAndGetFiltersNoUnsupportedMethodExceptionFromDummyRequest() {
		loadConfig(SecurityMatcherMvcConfig.class);
		assertThat(this.springSecurityFilterChain.getFilters("/path")).isNotEmpty();
	}

	@Test
	public void securityMatchersWhenMvcThenMvcMatcher() throws Exception {
		loadConfig(SecurityMatchersMvcMatcherConfig.class, LegacyMvcMatchingConfig.class);
		this.request.setServletPath("/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("/path.html");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("/path/");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	public void securityMatchersWhenMvcMatcherInLambdaThenPathIsSecured() throws Exception {
		loadConfig(SecurityMatchersMvcMatcherInLambdaConfig.class, LegacyMvcMatchingConfig.class);
		this.request.setServletPath("/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("/path.html");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("/path/");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	public void securityMatchersMvcMatcherServletPath() throws Exception {
		loadConfig(SecurityMatchersMvcMatcherServletPathConfig.class);
		this.request.setServletPath("/spring");
		this.request.setRequestURI("/spring/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("");
		this.request.setRequestURI("/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		setup();
		this.request.setServletPath("/other");
		this.request.setRequestURI("/other/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void securityMatchersWhensMvcMatcherServletPathInLambdaThenPathIsSecured() throws Exception {
		loadConfig(SecurityMatchersMvcMatcherServletPathInLambdaConfig.class);
		this.request.setServletPath("/spring");
		this.request.setRequestURI("/spring/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setServletPath("");
		this.request.setRequestURI("/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
		setup();
		this.request.setServletPath("/other");
		this.request.setRequestURI("/other/path");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_OK);
	}

	@Test
	public void securityMatchersWhenMultiMvcMatcherInLambdaThenAllPathsAreDenied() throws Exception {
		loadConfig(MultiMvcMatcherInLambdaConfig.class);
		this.request.setRequestURI("/test-1");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setRequestURI("/test-2");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setRequestURI("/test-3");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	@Test
	public void securityMatchersWhenMultiMvcMatcherThenAllPathsAreDenied() throws Exception {
		loadConfig(MultiMvcMatcherConfig.class);
		this.request.setRequestURI("/test-1");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setRequestURI("/test-2");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
		setup();
		this.request.setRequestURI("/test-3");
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);
		assertThat(this.response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
	}

	public void loadConfig(Class<?>... configs) {
		this.context = new AnnotationConfigWebApplicationContext();
		this.context.register(configs);
		MockServletContext servletContext = new MockServletContext();
		servletContext.addServlet("dispatcherServlet", DispatcherServlet.class);
		this.context.setServletContext(servletContext);
		this.context.refresh();
		this.context.getAutowireCapableBeanFactory().autowireBean(this);
	}

	private void mockMvcPresentClasspath(Object newValue) throws Exception {
		mockMvcPresentClasspath(HttpSecurity.class, newValue);
		mockMvcPresentClasspath(AbstractRequestMatcherRegistry.class, newValue);
	}

	private void mockMvcPresentClasspath(Class<?> clazz, Object newValue) throws Exception {
		Field mvcPresentField = clazz.getDeclaredField("mvcPresent");
		mvcPresentField.setAccessible(true);
		Field modifiersField = Field.class.getDeclaredField("modifiers");
		modifiersField.setAccessible(true);
		modifiersField.setInt(mvcPresentField, mvcPresentField.getModifiers() & ~Modifier.FINAL);
		mvcPresentField.set(null, newValue);
	}

	@EnableWebSecurity
	@Configuration
	@EnableWebMvc
	static class MultiMvcMatcherInLambdaConfig {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain first(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.securityMatchers((requests) -> requests
					.requestMatchers("/test-1")
					.requestMatchers("/test-2")
					.requestMatchers("/test-3")
				)
				.authorizeHttpRequests((authorize) -> authorize.anyRequest().denyAll())
				.httpBasic(withDefaults());
			// @formatter:on
			return http.build();
		}

		@Bean
		SecurityFilterChain second(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.securityMatchers((requests) -> requests
					.requestMatchers("/test-1")
				)
				.authorizeHttpRequests((authorize) -> authorize
					.anyRequest().permitAll()
				);
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping({ "/test-1", "/test-2", "/test-3" })
			String path() {
				return "path";
			}

		}

	}

	@EnableWebSecurity
	@Configuration
	@EnableWebMvc
	static class MultiMvcMatcherConfig {

		@Bean
		@Order(Ordered.HIGHEST_PRECEDENCE)
		SecurityFilterChain first(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.securityMatchers()
					.requestMatchers("/test-1")
					.requestMatchers("/test-2")
					.requestMatchers("/test-3")
					.and()
				.authorizeHttpRequests()
					.anyRequest().denyAll()
					.and()
				.httpBasic(withDefaults());
			// @formatter:on
			return http.build();
		}

		@Bean
		SecurityFilterChain second(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.securityMatchers()
					.requestMatchers("/test-1")
					.and()
				.authorizeHttpRequests()
					.anyRequest().permitAll();
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping({ "/test-1", "/test-2", "/test-3" })
			String path() {
				return "path";
			}

		}

	}

	@EnableWebSecurity
	@EnableWebMvc
	@Configuration
	@Import(UsersConfig.class)
	static class SecurityMatcherMvcConfig {

		@Bean
		SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.securityMatcher("/path")
				.httpBasic().and()
				.authorizeHttpRequests()
					.anyRequest().denyAll();
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping("/path")
			String path() {
				return "path";
			}

		}

	}

	@EnableWebSecurity
	@Configuration
	@Import(UsersConfig.class)
	static class SecurityMatcherNoMvcConfig {

		@Bean
		SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
			// @formatter:off
			http
					.securityMatcher("/path")
					.httpBasic().and()
					.authorizeHttpRequests()
					.anyRequest().denyAll();
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping("/path")
			String path() {
				return "path";
			}

		}

	}

	@EnableWebSecurity
	@Configuration
	@EnableWebMvc
	@Import(UsersConfig.class)
	static class SecurityMatchersMvcMatcherConfig {

		@Bean
		SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.securityMatchers()
					.requestMatchers("/path")
					.and()
				.httpBasic().and()
				.authorizeHttpRequests()
					.anyRequest().denyAll();
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping("/path")
			String path() {
				return "path";
			}

		}

	}

	@EnableWebSecurity
	@Configuration
	@EnableWebMvc
	static class SecurityMatchersMvcMatcherInLambdaConfig {

		@Bean
		SecurityFilterChain appSecurity(HttpSecurity http) throws Exception {
			// @formatter:off
			http
				.securityMatchers((matchers) -> matchers
					.requestMatchers("/path")
				)
				.httpBasic(withDefaults())
				.authorizeHttpRequests((authorize) -> authorize
					.anyRequest().denyAll()
				);
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping("/path")
			String path() {
				return "path";
			}

		}

	}

	@EnableWebSecurity
	@Configuration
	@EnableWebMvc
	@Import(UsersConfig.class)
	static class SecurityMatchersMvcMatcherServletPathConfig {

		@Bean
		SecurityFilterChain appSecurity(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
			MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector)
					.servletPath("/spring");
			// @formatter:off
			http
				.securityMatchers()
					.requestMatchers(mvcMatcherBuilder.pattern("/path"))
					.requestMatchers(mvcMatcherBuilder.pattern("/never-match"))
					.and()
				.httpBasic().and()
				.authorizeHttpRequests()
					.anyRequest().denyAll();
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping("/path")
			String path() {
				return "path";
			}

		}

	}

	@EnableWebSecurity
	@Configuration
	@EnableWebMvc
	@Import(UsersConfig.class)
	static class SecurityMatchersMvcMatcherServletPathInLambdaConfig {

		@Bean
		SecurityFilterChain appSecurity(HttpSecurity http, HandlerMappingIntrospector introspector) throws Exception {
			MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector)
					.servletPath("/spring");
			// @formatter:off
			http
				.securityMatchers((matchers) -> matchers
					.requestMatchers(mvcMatcherBuilder.pattern("/path"))
					.requestMatchers(mvcMatcherBuilder.pattern("/never-match"))
				)
				.httpBasic(withDefaults())
				.authorizeHttpRequests((authorize) -> authorize
					.anyRequest().denyAll()
				);
			// @formatter:on
			return http.build();
		}

		@RestController
		static class PathController {

			@RequestMapping("/path")
			String path() {
				return "path";
			}

		}

	}

	@Configuration
	static class UsersConfig {

		@Bean
		UserDetailsService userDetailsService() {
			UserDetails user = User.withDefaultPasswordEncoder().username("user").password("password").roles("USER")
					.build();
			return new InMemoryUserDetailsManager(user);
		}

	}

	@Configuration
	static class LegacyMvcMatchingConfig implements WebMvcConfigurer {

		@Override
		public void configurePathMatch(PathMatchConfigurer configurer) {
			configurer.setUseSuffixPatternMatch(true);
			configurer.setUseTrailingSlashMatch(true);
		}

	}

	private static class MockServletContext extends org.springframework.mock.web.MockServletContext {

		private final Map<String, ServletRegistration> registrations = new LinkedHashMap<>();

		@NotNull
		@Override
		public ServletRegistration.Dynamic addServlet(@NotNull String servletName, Class<? extends Servlet> clazz) {
			ServletRegistration.Dynamic dynamic = mock(ServletRegistration.Dynamic.class);
			given(dynamic.getClassName()).willReturn(clazz.getName());
			this.registrations.put(servletName, dynamic);
			return dynamic;
		}

		@NotNull
		@Override
		public Map<String, ? extends ServletRegistration> getServletRegistrations() {
			return this.registrations;
		}

	}

}
