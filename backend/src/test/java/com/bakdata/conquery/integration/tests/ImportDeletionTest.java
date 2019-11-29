package com.bakdata.conquery.integration.tests;

import com.bakdata.conquery.commands.SlaveCommand;
import com.bakdata.conquery.integration.json.JsonIntegrationTest;
import com.bakdata.conquery.integration.json.QueryTest;
import com.bakdata.conquery.io.xodus.MasterMetaStorage;
import com.bakdata.conquery.io.xodus.WorkerStorage;
import com.bakdata.conquery.models.auth.DevAuthConfig;
import com.bakdata.conquery.models.auth.permissions.Ability;
import com.bakdata.conquery.models.auth.permissions.ConqueryPermission;
import com.bakdata.conquery.models.auth.permissions.DatasetPermission;
import com.bakdata.conquery.models.auth.subjects.Mandator;
import com.bakdata.conquery.models.auth.subjects.User;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.identifiable.ids.specific.ImportId;
import com.bakdata.conquery.models.identifiable.ids.specific.MandatorId;
import com.bakdata.conquery.models.identifiable.ids.specific.UserId;
import com.bakdata.conquery.models.messages.namespaces.specific.RemoveImportJob;
import com.bakdata.conquery.models.query.IQuery;
import com.bakdata.conquery.models.query.ManagedQuery;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Worker;
import com.bakdata.conquery.models.worker.WorkerInformation;
import com.bakdata.conquery.util.support.StandaloneSupport;
import com.bakdata.conquery.util.support.TestConquery;
import com.github.powerlibraries.io.In;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test if Imports can be deleted and safely queried.
 *
 */
@Slf4j
public class ImportDeletionTest implements ProgrammaticIntegrationTest {

	private Mandator mandator = new Mandator("testMandatorName", "testMandatorLabel");
	private MandatorId mandatorId = mandator.getId();
	private User user = new User("testUser@test.de", "testUserName");
	private UserId userId = user.getId();
	private ConqueryPermission permission = new DatasetPermission(Ability.READ.asSet(), new DatasetId("testDatasetId"));

	@Override
	public void execute(String name, TestConquery testConquery) throws Exception {
		MasterMetaStorage storage = null;

		try (StandaloneSupport conquery = testConquery.getSupport(name)){
			storage = conquery.getStandaloneCommand().getMaster().getStorage();

			final String testJson = In.resource("/tests/query/DELETE_IMPORT_TESTS/SIMPLE_TREECONCEPT_Query.test.json").withUTF8().readAll();

			final DatasetId dataset = conquery.getDataset().getId();
			final Namespace namespace = storage.getNamespaces().get(dataset);

			final ImportId importId = ImportId.Parser.INSTANCE.parse(dataset.getName(), "test_table2", "test_table2_import");

			final QueryTest test = (QueryTest) JsonIntegrationTest.readJson(dataset, testJson);
			final IQuery query = test.parseQuery(conquery);

			// Manually import data, so we can do our own work.
			{
				ValidatorHelper.failOnError(log, conquery.getValidator().validate(test));

				test.importTables(conquery);
				conquery.waitUntilWorkDone();

				test.importConcepts(conquery);
				conquery.waitUntilWorkDone();

				test.importTableContents(conquery, Arrays.asList(test.getContent().getTables()));
				conquery.waitUntilWorkDone();
			}




			final int nImports = namespace.getStorage().getAllImports().size();

			log.info("Checking state before deletion");

			// State before deletion.
			{
				// Must contain the import.
				assertThat(namespace.getStorage().getAllImports())
						.filteredOn(imp -> imp.getId().equals(importId))
						.isNotEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.isNotEmpty();
						assertThat(workerStorage.getAllBuckets())
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.isNotEmpty();
					}
				}

				log.info("Executing query before deletion");

				assertQueryResult(conquery, query, 2L);
			}

			log.info("Issuing deletion of import {}", importId);

			// Delete the import.
			namespace.getStorage().removeImport(importId);

			for (WorkerInformation w : namespace.getWorkers()) {
				w.send(new RemoveImportJob(importId));
			}
			Thread.sleep(100);
			conquery.waitUntilWorkDone();

			log.info("Checking state after deletion");

			{
				// We have deleted an import now there should be two less!
				assertThat(namespace.getStorage().getAllImports().size()).isLessThan(nImports);

				// The deleted import should not be found.
				assertThat(namespace.getStorage().getAllImports())
						.filteredOn(imp -> imp.getId().equals(importId))
						.isEmpty();

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						// No bucket should be found referencing the import.
						assertThat(workerStorage.getAllBuckets())
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.filteredOn(bucket -> bucket.getImp().getId().equals(importId))
								.isEmpty();

						// No CBlock associated with import may exist
						assertThat(workerStorage.getAllCBlocks())
								.describedAs("CBlocks for Worker %s", value.getInfo().getId())
								.filteredOn(cBlock -> cBlock.getBucket().getImp().equals(importId))
								.isEmpty();
					}
				}

				log.info("Executing query after deletion");

				// Issue a query and asseert that it has less content.
				assertQueryResult(conquery, query, 1L);
			}

			conquery.waitUntilWorkDone();

			// Load the same import into the same table, with only the deleted import/table
			{
				// only import the deleted import/table
				test.importTableContents(conquery, Arrays.stream(test.getContent().getTables()).filter(table -> table.getName().equalsIgnoreCase(importId.getTable().getTable())).collect(Collectors.toList()));
				conquery.waitUntilWorkDone();
			}

			log.info("Checking state after re-import");

			{
				assertThat(namespace.getStorage().getAllImports().size()).isEqualTo(nImports);

				for (SlaveCommand slave : conquery.getStandaloneCommand().getSlaves()) {
					for (Worker value : slave.getWorkers().getWorkers().values()) {
						final WorkerStorage workerStorage = value.getStorage();

						assertThat(workerStorage.getAllBuckets().stream().filter(bucket -> bucket.getImp().getId().equals(importId)))
								.describedAs("Buckets for Worker %s", value.getInfo().getId())
								.isNotEmpty();
					}
				}

				log.info("Executing query after re-import");

				// Issue a query and assert that it has the same content as the first time around.
				assertQueryResult(conquery, query, 2L);
			}

		}
		finally {
			if (storage != null) {
				storage.removeMandator(mandatorId);
				storage.removeUser(userId);
			}
		}
	}

	/**
	 * Send a query onto the conquery instance and assert the result's size.
	 */
	private void assertQueryResult(StandaloneSupport conquery, IQuery query, long size) throws JSONException {
		final ManagedQuery managedQuery = conquery.getNamespace().getQueryManager().runQuery(query, DevAuthConfig.USER);

		managedQuery.awaitDone(2, TimeUnit.MINUTES);
		assertThat(managedQuery.getState()).isEqualTo(ExecutionState.DONE);

		assertThat(managedQuery.getLastResultCount()).isEqualTo(size);
	}
}
