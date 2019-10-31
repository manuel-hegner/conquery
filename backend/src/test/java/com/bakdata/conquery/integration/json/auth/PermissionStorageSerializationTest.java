package com.bakdata.conquery.integration.json.auth;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.integration.common.RequiredUser;
import com.bakdata.conquery.integration.json.ConqueryTestSpec;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.StoreInfo;
import com.bakdata.conquery.io.xodus.stores.MPStore;
import com.bakdata.conquery.models.auth.subjects.Role;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.identifiable.ids.specific.RoleId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.Setter;

/**
 * This testspec checks if permission related data can be cleanly serialized and deserialized
 * using the provided store.
 */
@CPSType(id="PERMISSION_STORAGE_SERIALIZATION_TEST",base=ConqueryTestSpec.class)
@Getter @Setter
public class PermissionStorageSerializationTest extends ConqueryTestSpec {
	private static final String STORE_SUFFIX = "SERIALIZATION_TEST";
	@Valid
	private Role [] roles = new Role[0];
	@Valid @NotNull @JsonProperty("users")
	private RequiredUser [] rUsers;
	
	@JsonIgnore
	private MPStore<RoleId, Role> authMandator;
	@JsonIgnore
	private MPStore<UserId, User> authUser;
	@JsonIgnore
	private File directory;
	
	@Override
	public void importRequiredData(StandaloneSupport support) throws Exception {
		Validator validator = support.getStandaloneCommand().getMaster().getValidator();
		StorageConfig config = support.getStandaloneCommand().getMaster().getConfig().getStorage();
		directory = new File(config.getDirectory(), STORE_SUFFIX);
		directory.deleteOnExit();
		Environment env = Environments.newInstance(directory, config.getXodus().createConfig());
		this.authMandator = new MPStore<>(validator, env, StoreInfo.AUTH_ROLE);
		this.authUser = new MPStore<>(validator, env, StoreInfo.AUTH_USER);
	}
	
	/**
	 * Serialize by filling the storage
	 * @throws JSONException
	 */
	public void serializeData() throws JSONException {
		for(Role mandator : roles) {
			authMandator.add(mandator.getId(), mandator);
		}
		for(RequiredUser rUser : rUsers) {
			User user = rUser.getUser();
			authUser.add(user.getId(), user);
		}
	}
	
	/**
	 * Reloading the cached store forces deserialization
	 */
	@JsonIgnore
	public void deserializeData() {
		List<Role> storedMandators = new ArrayList<>();
		authMandator.forEach(e -> storedMandators.add(e.getValue()));
		assertThat(storedMandators).containsExactlyInAnyOrderElementsOf(Arrays.asList(roles));

		List<User> storedUser = new ArrayList<>();
		authUser.forEach(e -> storedUser.add(e.getValue()));
		assertThat(storedUser).containsExactlyInAnyOrderElementsOf(Arrays.stream(rUsers).map(rU -> rU.getUser()).collect(Collectors.toList()));
		
	}

	@Override
	public void executeTest(StandaloneSupport support) throws Exception {
		serializeData();
		deserializeData();
		
		// No exceptions until here, everything is fine
		assert true;
	}
	
}
