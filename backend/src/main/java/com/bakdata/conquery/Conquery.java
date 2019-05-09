package com.bakdata.conquery;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.tools.ToolProvider;

import org.apache.commons.lang3.StringUtils;

import com.bakdata.conquery.commands.CollectEntitiesCommand;
import com.bakdata.conquery.commands.MasterCommand;
import com.bakdata.conquery.commands.PreprocessorCommand;
import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.commands.StandaloneCommand;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.util.UrlRewriteBundle;

import ch.qos.logback.classic.Level;
import io.dropwizard.Application;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.configuration.JsonConfigurationFactory;
import io.dropwizard.servlets.assets.AssetServlet;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Getter
public class Conquery extends Application<ConqueryConfig> {

	private final String name;
	private MasterCommand master;

	public Conquery() {
		this("Conquery");
	}

	@Override
	public void initialize(Bootstrap<ConqueryConfig> bootstrap) {
		Jackson.configure(bootstrap.getObjectMapper());
		//check for java compiler, needed for the class generation
		if (ToolProvider.getSystemJavaCompiler() == null) {
			throw new IllegalStateException("Conquery requires to be run on either a JDK or a ServerJRE");
		}

		//main config file is json
		bootstrap.setConfigurationFactoryFactory(JsonConfigurationFactory::new);

		bootstrap.addCommand(new SlaveCommand());
		bootstrap.addCommand(new PreprocessorCommand());
		bootstrap.addCommand(new CollectEntitiesCommand());
		bootstrap.addCommand(new StandaloneCommand(this));

		//do some setup in other classes after initialization but before running a command
		bootstrap.addBundle(new ConfiguredBundle<ConqueryConfig>() {
			@Override
			public void run(ConqueryConfig configuration, Environment environment) throws Exception {
				configuration.initializeDatePatterns();
			}

			@Override
			public void initialize(Bootstrap<?> bootstrap) {
			}
		});
		//register frontend
		registerFrontend(bootstrap);

		//freemarker support
		bootstrap.addBundle(new ViewBundle<>());
	}

	protected void registerFrontend(Bootstrap<ConqueryConfig> bootstrap) {
		bootstrap.addBundle(new UrlRewriteBundle());
		bootstrap.addBundle(new ConfiguredBundle<ConqueryConfig>() {
			@Override
			public void run(ConqueryConfig configuration, Environment environment) throws Exception {
				String uriPath = "/app/";
				String language = configuration.getLocale().getFrontend().getLanguage();
				environment.servlets().addServlet(
					"app",
					new AssetServlet(
						"/frontend/app/",
						uriPath,
						String.format(
								"static/index.%s.html",
								StringUtils.defaultIfEmpty(language, Locale.ENGLISH.getLanguage())
						),
						StandardCharsets.UTF_8
					)
				)
				.addMapping(uriPath + '*');
			}

			@Override
			public void initialize(Bootstrap<?> bootstrap) {
			}
		});
	}

	@Override
	protected Level bootstrapLogLevel() {
		return Level.INFO;
	}

	@Override
	public void run(ConqueryConfig configuration, Environment environment) throws Exception {
		master = new MasterCommand();
		master.run(configuration, environment);
	}

	public static void main(String... args) throws Exception {
		new Conquery().run(args);
	}
}
