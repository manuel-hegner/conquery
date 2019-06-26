package com.bakdata.conquery.io.jersey;

import org.glassfish.jersey.server.ResourceConfig;

import com.bakdata.conquery.io.jackson.PathParamInjector;
import com.bakdata.conquery.io.jetty.CORSResponseFilter;
import com.bakdata.conquery.io.jetty.CachingFilter;
import com.bakdata.conquery.io.jetty.ConqueryJsonExceptionMapper;
import com.bakdata.conquery.io.jetty.JsonValidationExceptionMapper;
import com.bakdata.conquery.models.auth.AuthorizationExceptionMapper;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.ConqueryConfig;

import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.errors.LoggingExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.server.DefaultServerFactory;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RESTServer {

	public static void configure(ConqueryConfig config, ResourceConfig jersey) {
		// Bind User class to REST authentication
		jersey.register(new AuthValueFactoryProvider.Binder(User.class));
		//change exception mapper behavior because of JERSEY-2437
		//https://github.com/eclipse-ee4j/jersey/issues/2709
		((DefaultServerFactory) config.getServerFactory()).setRegisterDefaultExceptionMappers(false);
		// Register custom mapper
		jersey.register(new AuthorizationExceptionMapper());
		jersey.register(new JsonValidationExceptionMapper());
		// default Dropwizard's exception mappers
		jersey.register(new LoggingExceptionMapper<Throwable>() {});
		jersey.register(ConqueryJsonExceptionMapper.class);
		jersey.register(new EarlyEofExceptionMapper());
		//allow cross origin
		if(config.getApi().isAllowCORSRequests()) {
			jersey.register(CORSResponseFilter.class);
		}
		//disable all browser caching if not expressly wanted
		jersey.register(CachingFilter.class);
		
		jersey.register(new PathParamInjector());
	}
}
