package com.bakdata.conquery.models.config;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class StatisticConfig {
	@NotEmpty
	private String url;
	@NotNull @Valid
	private Duration timeout = Duration.seconds(60);
}
