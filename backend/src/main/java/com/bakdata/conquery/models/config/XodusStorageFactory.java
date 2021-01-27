package com.bakdata.conquery.models.config;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.validation.Valid;
import javax.validation.Validator;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.bakdata.conquery.commands.ManagerNode;
import com.bakdata.conquery.commands.ShardNode;
import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.*;
import com.bakdata.conquery.io.xodus.stores.SerializingStore;
import com.bakdata.conquery.models.worker.DatasetRegistry;
import com.bakdata.conquery.models.worker.Namespace;
import com.bakdata.conquery.models.worker.Workers;
import com.bakdata.conquery.util.io.ConqueryMDC;
import io.dropwizard.util.Duration;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter @Setter @ToString
@CPSType(id = "XODUS", base = StorageFactory.class)
public class XodusStorageFactory implements StorageFactory {

	private Path directory = Path.of("storage");

	private boolean validateOnWrite = false;
	@NotNull @Valid
	private XodusConfig xodus = new XodusConfig();

	private boolean useWeakDictionaryCaching = true;
	@NotNull
	private Duration weakCacheDuration = Duration.hours(48);

	@Min(1)
	private int nThreads = Runtime.getRuntime().availableProcessors();
	
	/**
	 * Flag for the {@link SerializingStore} whether to delete values from the underlying store, that cannot be mapped to an object anymore.
	 */
	private boolean removeUnreadablesFromStore = false;
	
	/**
	 * When set, all values that could not be deserialized from the persistent store, are dump into individual files.
	 */
	private Optional<File> unreadbleDataDumpDirectory = Optional.empty();

	@Override
	public MetaStorage createMetaStorage(Validator validator, DatasetRegistry datasets) {
		return new MetaStorageImpl(datasets, validator, this);
	}

	@Override
	public NamespaceStorage createNamespaceStorage(Validator validator, String directory, boolean returnNullOnExisting) {
		File storageDir = getDirectory().resolve(directory).toFile();
		if (returnNullOnExisting && storageDir.exists()) {
			return null;
		}
		return new NamespaceStorageImpl(validator, storageDir, this);
	}

	@Override
	public WorkerStorage createWorkerStorage(Validator validator, String directory, boolean returnNullOnExisting) {
		File storageDir = getDirectory().resolve(directory).toFile();
		if (returnNullOnExisting && storageDir.exists()) {
			return null;
		}
		return new WorkerStorageImpl(validator, storageDir, this);
	}

	@Override
	@SneakyThrows
	public void loadNamespaceStorages(ManagerNode managerNode) {

		if(getDirectory().toFile().mkdir()){
			log.warn("Had to create Storage Dir at `{}`", getDirectory());
		}

		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : getDirectory().toFile().listFiles((file, name) -> name.startsWith("dataset_"))) {
			loaders.submit(() -> {
				NamespaceStorage datasetStorage = NamespaceStorageImpl.tryLoad(managerNode.getValidator(), this, directory);

				if (datasetStorage == null) {
					log.warn("Unable to load a dataset at `{}`", directory);
					return;
				}

				Namespace ns = new Namespace(datasetStorage);
				ns.initMaintenance(managerNode.getMaintenanceService());
				managerNode.getDatasetRegistry().add(ns);
			});
		}


		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)){
			log.debug("Still waiting for Datasets to load. {} already finished.", managerNode.getDatasetRegistry().getDatasets());
		}

		log.info("All stores loaded: {}",  managerNode.getDatasetRegistry().getDatasets());
	}

	@Override
	@SneakyThrows
	public void loadWorkerStorages(ShardNode shardNode) {

		if (getDirectory().toFile().mkdir()) {
			log.warn("Had to create Storage Dir at `{}`", getDirectory());
		}

		Workers workers = new Workers(
				shardNode.getConfig().getQueries().getExecutionPool(),
				getNThreads(),
				shardNode.getConfig().getCluster().getEntityBucketSize());

		ExecutorService loaders = Executors.newFixedThreadPool(getNThreads());


		for (File directory : getDirectory().toFile().listFiles((file, name) -> name.startsWith("worker_"))) {

			loaders.submit(() -> {
				ConqueryMDC.setLocation(directory.toString());

				WorkerStorage workerStorage = WorkerStorageImpl.tryLoad(shardNode.getValidator(), this, directory);
				if (workerStorage == null) {
					log.warn("No valid WorkerStorage found.");
					return;
				}

				workers.createWorker(
						workerStorage
				);

				ConqueryMDC.clearLocation();
			});
		}

		loaders.shutdown();
		while (!loaders.awaitTermination(1, TimeUnit.MINUTES)) {
			log.debug("Waiting for Workers to load. {} are already finished.", workers.getWorkers().size());
		}
		shardNode.setWorkers(workers);
	}
}
