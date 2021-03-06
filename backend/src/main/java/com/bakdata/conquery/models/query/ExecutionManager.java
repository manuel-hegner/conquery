package com.bakdata.conquery.models.query;

import java.util.Objects;
import java.util.UUID;

import com.bakdata.conquery.apiv1.QueryDescription;
import com.bakdata.conquery.io.storage.MetaStorage;
import com.bakdata.conquery.metrics.ExecutionMetrics;
import com.bakdata.conquery.models.auth.AuthorizationHelper;
import com.bakdata.conquery.models.auth.entities.Group;
import com.bakdata.conquery.models.auth.entities.User;
import com.bakdata.conquery.models.config.ConqueryConfig;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.execution.ExecutionState;
import com.bakdata.conquery.models.execution.ManagedExecution;
import com.bakdata.conquery.models.identifiable.ids.specific.ManagedExecutionId;
import com.bakdata.conquery.models.messages.namespaces.specific.ExecuteQuery;
import com.bakdata.conquery.models.query.results.ShardResult;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class ExecutionManager {

	@NonNull
	private final Namespace namespace;

	public static ManagedExecution<?> runQuery(DatasetRegistry datasets, QueryDescription query, User user, Dataset submittedDataset, ConqueryConfig config) {
		final ManagedExecution<?> execution = createExecution(datasets, query, user, submittedDataset);
		execute(datasets, execution, config);

		return execution;
	}

	public static void execute(DatasetRegistry datasets, ManagedExecution<?> execution, ConqueryConfig config) {
		// Initialize the query / create subqueries
		execution.initExecutable(datasets, config);

		log.info("Executing Query[{}] in Datasets[{}]", execution.getQueryId(), execution.getRequiredDatasets());


		execution.start();

		final MetaStorage storage = datasets.getMetaStorage();
		final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(execution.getOwner(), storage).map(Group::getName).orElse("none");
		ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).inc();

		for (Namespace namespace : execution.getRequiredDatasets()) {
			namespace.getQueryManager().executeQueryInNamespace(execution);
		}
	}

	public static ManagedExecution<?> createExecution(DatasetRegistry datasets, QueryDescription query, User user, Dataset submittedDataset) {
		return createQuery(datasets, query, UUID.randomUUID(), user, submittedDataset);
	}

	/**
	 * Send message for query execution to all workers.
	 */
	private ManagedExecution<?> executeQueryInNamespace(ManagedExecution<?> query) {
		namespace.sendToAll(new ExecuteQuery(query));
		return query;
	}

	public static ManagedExecution<?> createQuery(DatasetRegistry datasets, QueryDescription query, UUID queryId, User user, Dataset submittedDataset) {
		// Transform the submitted query into an initialized execution
		ManagedExecution<?> managed = query.toManagedExecution(user, submittedDataset);

		managed.setQueryId(queryId);

		// Store the execution
		datasets.getMetaStorage().addExecution(managed);

		return managed;
	}


	/**
	 * Receive part of query result and store into query.
	 *
	 * @param result
	 */
	public <R extends ShardResult, E extends ManagedExecution<R>> void addQueryResult(R result) {
		final MetaStorage storage = namespace.getNamespaces().getMetaStorage();

		final E query = (E) getQuery(result.getQueryId());

		if(!query.getState().equals(ExecutionState.RUNNING)){
			return;
		}

		query.addResult(storage, result);

		if (query.getState() == ExecutionState.DONE || query.getState() == ExecutionState.FAILED) {
			final String primaryGroupName = AuthorizationHelper.getPrimaryGroup(query.getOwner(), storage).map(Group::getName).orElse("none");

			ExecutionMetrics.getRunningQueriesCounter(primaryGroupName).dec();
			ExecutionMetrics.getQueryStateCounter(query.getState(), primaryGroupName).inc();
			ExecutionMetrics.getQueriesTimeHistogram(primaryGroupName).update(query.getExecutionTime().toMillis());
		}
	}

	public ManagedExecution<?> getQuery(@NonNull ManagedExecutionId id) {
		return Objects.requireNonNull(namespace.getStorage().getMetaStorage().getExecution(id), "Unable to find query " + id.toString());
	}

}
