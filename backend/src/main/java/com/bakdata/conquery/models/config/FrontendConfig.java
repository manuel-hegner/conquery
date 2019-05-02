package com.bakdata.conquery.models.config;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.util.VersionInfo;

import groovy.transform.ToString;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@ToString @Getter @Setter
public class FrontendConfig {
	
	private String version = VersionInfo.INSTANCE.getProjectVersion();
	@Valid @NotNull
	private CurrencyConfig currency = new CurrencyConfig();
	
	@Data
	private static class CurrencyConfig {
		private int factor = 1;
		private String prefix = "€";
		private String thousandSeparator = ".";
		private String decimalSeparator = ",";
		private int decimalScale = 2;
	}
}