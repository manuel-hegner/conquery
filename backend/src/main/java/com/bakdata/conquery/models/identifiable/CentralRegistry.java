package com.bakdata.conquery.models.identifiable;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.MutableInjectableValues;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.datasets.Column;
import com.bakdata.conquery.models.datasets.Dataset;
import com.bakdata.conquery.models.datasets.Import;
import com.bakdata.conquery.models.datasets.Table;
import com.bakdata.conquery.models.error.ConqueryError.ExecutionCreationResolveError;
import com.bakdata.conquery.models.identifiable.ids.IId;
import com.bakdata.conquery.models.identifiable.ids.specific.DatasetId;
import com.bakdata.conquery.models.worker.IdResolveContext;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Validator;


@Slf4j
@SuppressWarnings({"rawtypes", "unchecked"}) @NoArgsConstructor
public class CentralRegistry implements Injectable {
	
	private final IdMap map = new IdMap<>();
	private final ConcurrentMap<IId<?>, Supplier<Identifiable<?>>> cacheables = new ConcurrentHashMap<>();
	
	public synchronized void register(Identifiable<?> ident) {
		map.add(ident);
	}
	
	public synchronized void registerCacheable(IId<?> id, Supplier<Identifiable<?>> supplier) {
		cacheables.put(id, supplier);
	}
	
	public <T extends Identifiable<?>> T resolve(IId<T> name) {
		Object res = map.get(name);
		if(res!=null) {
			return (T)res;
		}
		Supplier<Identifiable<?>> supplier = cacheables.get(name);
		if(supplier == null) {
			throw new ExecutionCreationResolveError(name);
		}
		return (T)supplier.get();
	}

	public <T extends Identifiable<?>> Optional<T> getOptional(IId<T> name) {
		Object res = map.get(name);
		if (res != null) {
			return Optional.of((T) res);
		}
		Supplier<Identifiable<?>> supplier = cacheables.get(name);
		if (supplier == null) {
			return Optional.empty();
		}
		return Optional.of((T) supplier.get());
	}

	public synchronized void remove(IId<?> id) {
		map.remove(id);
		cacheables.remove(id);
	}
	
	public void remove(Identifiable<?> ident) {
		remove(ident.getId());
	}

	@Override
	public MutableInjectableValues inject(MutableInjectableValues values) {
		return values.add(CentralRegistry.class, this);
	}

	public static CentralRegistry get(DeserializationContext ctxt) throws JsonMappingException {
		CentralRegistry result = (CentralRegistry) ctxt.findInjectableValue(CentralRegistry.class.getName(), null, null);
		if(result == null) {
			IdResolveContext alternative = (IdResolveContext)ctxt.findInjectableValue(IdResolveContext.class.getName(), null, null);
			if(alternative == null) {
				return null;
			}
			return alternative.getMetaRegistry();
		}
		return result;
	}

	public static CentralRegistry getForDataset(DeserializationContext ctxt, DatasetId datasetId) throws JsonMappingException {
		IdResolveContext alternative = (IdResolveContext)ctxt.findInjectableValue(IdResolveContext.class.getName(), null, null);

		if(alternative == null)
			return null;

		return alternative.findRegistry(datasetId);
	}

	public void addDatasetHook(Dataset dataset) {
		register(dataset);
	}

	public void removeDatasetHook(Dataset dataset) {
		remove(dataset);
	}

	public void addTableHook(Table table) {
		register(table);
		for (Column c : table.getColumns()) {
			register(c);
		}
	}

	public void removeTableHook(Table table) {
		remove(table);
		for (Column c : table.getColumns()) {
			remove(c);
		}
	}

	@SneakyThrows
	public void addConceptHook(Concept<?> concept, boolean registerImports, Collection<Import> imports, Validator validator) {
		register(concept);
		Dataset ds = resolve(
				concept.getDataset() == null
						? concept.getId().getDataset()
						: concept.getDataset()
		);
		concept.setDataset(ds.getId());

		concept.initElements(validator);

		concept.getSelects().forEach(this::register);
		for (Connector c : concept.getConnectors()) {
			this.register(c);
			c.collectAllFilters().forEach(this::register);
			c.getSelects().forEach(this::register);
		}
		//add imports of table
		if (registerImports) {
			for (Import imp : imports) {
				for (Connector con : concept.getConnectors()) {
					if (con.getTable().getId().equals(imp.getTable())) {
						con.addImport(imp);
					}
				}
			}
		}
	}
}
