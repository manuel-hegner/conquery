package com.bakdata.conquery.resources.hierarchies;

import javax.annotation.PostConstruct;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.bakdata.conquery.models.auth.subjects.User;

import io.dropwizard.auth.Auth;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public abstract class HAuthorized {

	@Auth
	protected User user;
	
	@PostConstruct
	public void init() {
		if(user == null) {
			throw new WebApplicationException(Status.UNAUTHORIZED);
		}
	}
}