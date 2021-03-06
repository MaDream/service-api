/*
 * Copyright 2016 EPAM Systems
 * 
 * 
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/service-api
 * 
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.epam.ta.reportportal.core.configs;

import com.epam.ta.reportportal.database.search.CriteriaMap;
import com.epam.ta.reportportal.database.search.CriteriaMapFactory;
import com.epam.ta.reportportal.database.search.Filter;
import com.epam.ta.reportportal.ws.resolver.FilterFor;
import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import springfox.documentation.PathProvider;
import springfox.documentation.schema.ModelReference;
import springfox.documentation.schema.TypeNameExtractor;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.service.ResolvedMethodParameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.contexts.ModelContext;
import springfox.documentation.spi.service.ParameterBuilderPlugin;
import springfox.documentation.spi.service.contexts.ParameterContext;
import springfox.documentation.spring.web.paths.RelativePathProvider;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.servlet.ServletContext;
import java.security.Principal;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;
import static springfox.documentation.schema.ResolvedTypes.modelRefFactory;
import static springfox.documentation.spi.schema.contexts.ModelContext.inputParam;

/**
 * SWAGGER 2.0 UI page configuration for Report Portal application
 *
 * @author dzmitry_kavalets
 * @author Andrei_Ramanchuk
 * @author Andrei Varabyeu
 */
@Configuration
@Conditional(Conditions.NotTestCondition.class)
@EnableSwagger2
@ComponentScan(basePackages = "com.epam.ta.reportportal.ws.controller")
public class Swagger2Configuration {

	@Autowired
	private ServletContext servletContext;

	@Autowired
	@Value("${spring.application.name}")
	private String eurekaName;

	@Autowired
	@Value("${info.build.version}")
	private String buildVersion;

	@Bean
	public Docket docket() {
		/* For more information see default params at {@link ApiInfo} */
		ApiInfo rpInfo = new ApiInfo("Report Portal", "Report Portal API documentation", buildVersion, "urn:tos",
				new Contact("EPAM Systems", "http://epam.com", "Support EPMC-TST Report Portal <SupportEPMC-TSTReportPortal@epam.com>"),
				"GPLv3", "https://www.gnu.org/licenses/licenses.html#GPL");

		// @formatter:off
        Docket rpDocket = new Docket(DocumentationType.SWAGGER_2)
                .ignoredParameterTypes(Principal.class)
                .pathProvider(rpPathProvider())
                .useDefaultResponseMessages(false)
                /* remove default endpoints from listing */
                .select().apis(not(or(
                        basePackage("org.springframework.boot"),
                        basePackage("org.springframework.cloud"))))
                .build();
        //@formatter:on

		rpDocket.apiInfo(rpInfo);
		return rpDocket;
	}

	@Bean
	public PathProvider rpPathProvider() {
		return new RelativePathProvider(servletContext);
	}

	@Bean
	PageableParameterBuilderPlugin pageableParameterBuilderPlugin(TypeNameExtractor nameExtractor, TypeResolver resolver) {
		return new PageableParameterBuilderPlugin(nameExtractor, resolver);
	}

	@Bean(name = "multipartResolver")
	public CommonsMultipartResolver commonsMultipartResolver() {
		return new CommonsMultipartResolver();
	}

	@Bean
	public UiConfiguration uiConfig() {
		return new UiConfiguration(null);
	}

	public static class PageableParameterBuilderPlugin implements ParameterBuilderPlugin {

		private final TypeNameExtractor nameExtractor;
		private final TypeResolver resolver;

		PageableParameterBuilderPlugin(TypeNameExtractor nameExtractor, TypeResolver resolver) {
			this.nameExtractor = nameExtractor;
			this.resolver = resolver;
		}

		@Override
		public boolean supports(DocumentationType delimiter) {
			return true;
		}

		private Function<ResolvedType, ? extends ModelReference> createModelRefFactory(ParameterContext context) {
			ModelContext modelContext = inputParam(context.resolvedMethodParameter().getParameterType(), context.getDocumentationType(),
					context.getAlternateTypeProvider(), context.getGenericNamingStrategy(), context.getIgnorableParameterTypes());
			return modelRefFactory(modelContext, nameExtractor);
		}

		@Override
		public void apply(ParameterContext context) {
			ResolvedMethodParameter parameter = context.resolvedMethodParameter();
			Class<?> type = parameter.getParameterType().getErasedType();
			if (type != null) {
				Function<ResolvedType, ? extends ModelReference> factory = createModelRefFactory(context);
				if (Pageable.class.isAssignableFrom(type)) {

					ModelReference intModel = factory.apply(resolver.resolve(Integer.TYPE));
					ModelReference stringModel = factory.apply(resolver.resolve(List.class, String.class));

					//@formatter:off
					List<Parameter> parameters = newArrayList(
							context.parameterBuilder()
									.parameterType("query")
									.name("page")
									.modelRef(intModel)
									.description("Page number of the requested page").build(),
							context.parameterBuilder()
									.parameterType("query")
									.name("size")
									.modelRef(intModel)
									.description("Size of a page")
									.build(),
							context.parameterBuilder()
									.parameterType("query")
									.name("sort")
									.modelRef(stringModel).allowMultiple(true)
									.description(
											"Sorting criteria in the format: property(,asc|desc). " + "Default sort order is ascending. "
													+ "Multiple sort criteria are supported.").build());

					//@formatter:on
					context.getOperationContext().operationBuilder().parameters(parameters);
				} else if (Filter.class.isAssignableFrom(type)) {
					FilterFor filterClass = parameter.findAnnotation(FilterFor.class).get();
					CriteriaMap<?> criteriaMap = CriteriaMapFactory.DEFAULT_INSTANCE_SUPPLIER.get().getCriteriaMap(filterClass.value());

					//@formatter:off
					List<Parameter> params = criteriaMap.getAllowedSearchCriterias().stream()
							.map(searchCriteria -> context
									.parameterBuilder()
										.parameterType("query")
										.name(searchCriteria)
										.modelRef(factory.apply(resolver.resolve(criteriaMap.getCriteriaHolder(searchCriteria).getDataType())))
									.description("Filters by '" + searchCriteria + "'")
									.build())
							/* if type is not a collection and first letter is not capital (all known to swagger types start from lower case) */
							.filter( p -> !(null == p.getModelRef().getItemType() && Character.isUpperCase(p.getModelRef().getType().toCharArray()[0])))
							.collect(Collectors.toList());
					//@formatter:on

					context.getOperationContext().operationBuilder().parameters(params);

				}
			}
		}
	}

	@SuppressWarnings("unused")
	private static class RPPathProvider extends RelativePathProvider {

		private String gatewayPath;

		RPPathProvider(ServletContext servletContext, String gatewayPath) {
			super(servletContext);
			this.gatewayPath = gatewayPath;
		}

		@Override
		protected String applicationPath() {
			return "/" + gatewayPath + super.applicationPath();
		}
	}

}
