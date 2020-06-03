package com.bakdata.conquery.io.xodus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.validation.Validator;

import com.bakdata.conquery.io.xodus.stores.KeyIncludingStore;
import com.bakdata.conquery.metrics.JobMetrics;
import com.bakdata.conquery.models.config.StorageConfig;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.util.functions.Collector;
import com.codahale.metrics.Timer;
import com.google.common.base.Stopwatch;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Environments;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter @Slf4j
public abstract class ConqueryStorageImpl implements ConqueryStorage {

	protected final File directory;
	protected final Validator validator;
	protected final Environment environment;
	@Getter
	protected final CentralRegistry centralRegistry = new CentralRegistry();
	private final List<KeyIncludingStore<?,?>> stores = new ArrayList<>();
	private final int nThreads;

	public ConqueryStorageImpl(Validator validator, StorageConfig config, File directory) {
		this.directory = directory;
		this.validator = validator;
		this.environment = Environments.newInstance(directory, config.getXodus().createConfig());
		this.nThreads = config.getThreads();
	}

	protected void createStores(Collector<KeyIncludingStore<?,?>> collector) {
	}

	/**
	 * Load all stores from disk.
	 */
	@Override
	public void loadData() {
		createStores(stores::add);
		log.info("Loading storage {} from {}", this.getClass().getSimpleName(), directory);

		try (final Timer.Context timer = JobMetrics.getStoreLoadingTimer()) {
			final ExecutorService loaders = Executors.newFixedThreadPool(nThreads);

			Stopwatch all = Stopwatch.createStarted();
			for (KeyIncludingStore<?, ?> store : stores) {
				loaders.submit(store::loadData);
			}

			loaders.shutdown();
			loaders.awaitTermination(1, TimeUnit.DAYS);

			log.info("Loaded complete {} storage within {}", this.getClass().getSimpleName(), all.stop());
		}
		catch (InterruptedException e) {
			throw new IllegalStateException("Failed while loading stores", e);
		}

	}

	@Override
	public void close() throws IOException {
		for(KeyIncludingStore<?, ?> store : stores) {
			store.close();
		}
		environment.close();
	}

	/**
	 * Clears the environment then closes it.
	 */
	public void remove() throws IOException {
		environment.clear();
		close();
	}
}
