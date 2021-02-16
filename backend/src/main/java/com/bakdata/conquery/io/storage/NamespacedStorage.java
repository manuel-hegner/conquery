package com.bakdata.conquery.io.storage;

import com.bakdata.conquery.ConqueryConstants;
import com.bakdata.conquery.io.storage.xodus.stores.SingletonStore;
import com.bakdata.conquery.models.concepts.Concept;
import com.bakdata.conquery.models.concepts.Connector;
import com.bakdata.conquery.models.concepts.filters.Filter;
import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.datasets.*;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.dictionary.EncodedDictionary;
import com.bakdata.conquery.models.events.Bucket;
import com.bakdata.conquery.models.events.CBlock;
import com.bakdata.conquery.models.events.stores.specific.string.StringTypeEncoded;
import com.bakdata.conquery.models.identifiable.CentralRegistry;
import com.bakdata.conquery.models.identifiable.ids.specific.*;
import com.bakdata.conquery.models.worker.WorkerInformation;
import lombok.Getter;
import lombok.SneakyThrows;

import javax.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Overlapping storage structure for {@link WorkerStorage} and {@link NamespaceStorage}.
 * The reason for the overlap ist primarily that all this stored members are necessary in the
 * SerDes communication between the manager and the shards/worker for the resolving of ids included in
 * messages (see also {@link com.bakdata.conquery.io.jackson.serializer.NsIdRef}).
 */
public abstract class NamespacedStorage implements ConqueryStorage {

    @Getter
    protected final CentralRegistry centralRegistry = new CentralRegistry();
    @Getter
    private final Validator validator;

    protected SingletonStore<Dataset> dataset;
    protected IdentifiableStore<SecondaryIdDescription> secondaryIds;
    protected IdentifiableStore<Table> tables;
    protected IdentifiableStore<Dictionary> dictionaries;
    protected IdentifiableStore<Import> imports;
    protected IdentifiableStore<Concept<?>> concepts;

    public NamespacedStorage(Validator validator, StoreFactory storageFactory, List<String> pathName) {
        this.validator = validator;

        dataset = storageFactory.createDatasetStore(pathName);
        secondaryIds = storageFactory.createSecondaryIdDescriptionStore(centralRegistry, pathName);
        tables = storageFactory.createTableStore(centralRegistry, pathName);
        dictionaries = storageFactory.createDictionaryStore(centralRegistry, pathName);
        imports = storageFactory.createImportStore(centralRegistry, pathName);
        concepts = storageFactory.createConceptStore(centralRegistry, pathName);

        decorateDatasetStore(dataset);
        decorateSecondaryIdDescriptionStore(secondaryIds);
        decorateDictionaryStore(dictionaries);
        decorateTableStore(tables);
        decorateImportStore(imports);
        decorateConceptStore(concepts);

    }

    @Override
    public void loadData() {
        dataset.loadData();
        secondaryIds.loadData();
        tables.loadData();
        dictionaries.loadData();
        imports.loadData();
        concepts.loadData();
    }

    @Override
    public void clear() {
        dataset.clear();
        secondaryIds.clear();
        tables.clear();
        dictionaries.clear();
        imports.clear();
        concepts.clear();
    }

    @Override
    public void removeStorage() {
        dataset.removeStore();
        secondaryIds.removeStore();
        tables.removeStore();
        dictionaries.removeStore();
        imports.removeStore();
        concepts.removeStore();

    }

    protected abstract boolean isRegisterImports();

    private void decorateDatasetStore(SingletonStore<Dataset> store) {
        store
                .onAdd(getCentralRegistry()::register)
                .onRemove(getCentralRegistry()::remove);
    }

    private void decorateSecondaryIdDescriptionStore(IdentifiableStore<SecondaryIdDescription> store) {
        // Nothing to decorate
    }

    private void decorateDictionaryStore(IdentifiableStore<Dictionary> store) {
        // Nothing to decorate
    }

    private void decorateTableStore(IdentifiableStore<Table> store) {
        store
                .onAdd(table -> {
                    for (Column c : table.getColumns()) {
                        getCentralRegistry().register(c);
                    }
                })
                .onRemove(table -> {
                    for (Column c : table.getColumns()) {
                        getCentralRegistry().remove(c);
                    }
                });
    }

    private void decorateConceptStore(IdentifiableStore<Concept<?>> store) {
        store
                .onAdd(concept -> {
                    Dataset ds = centralRegistry.resolve(
                            concept.getDataset() == null
                                    ? concept.getId().getDataset()
                                    : concept.getDataset()
                    );
                    concept.setDataset(ds.getId());

                    concept.initElements(getValidator());

                    concept.getSelects().forEach(centralRegistry::register);
                    for (Connector c : concept.getConnectors()) {
                        centralRegistry.register(c);
                        c.collectAllFilters().forEach(centralRegistry::register);
                        c.getSelects().forEach(centralRegistry::register);
                    }
                    //add imports of table
                    if (isRegisterImports()) {
                        for (Import imp : getAllImports()) {
                            for (Connector con : concept.getConnectors()) {
                                if (con.getTable().getId().equals(imp.getTable())) {
                                    con.addImport(imp);
                                }
                            }
                        }
                    }
                })
                .onRemove(concept -> {
                    concept.getSelects().forEach(centralRegistry::remove);
                    //see #146  remove from Dataset.concepts
                    for (Connector c : concept.getConnectors()) {
                        c.getSelects().forEach(centralRegistry::remove);
                        c.collectAllFilters().stream().map(Filter::getId).forEach(centralRegistry::remove);
                        centralRegistry.remove(c.getId());
                    }
                });
    }

    private void decorateImportStore(IdentifiableStore<Import> store) {
        store
                .onAdd(imp -> {
                    imp.loadExternalInfos(this);

                    if (isRegisterImports()) {
                        for (Concept<?> c : getAllConcepts()) {
                            for (Connector con : c.getConnectors()) {
                                if (con.getTable().getId().equals(imp.getTable())) {
                                    con.addImport(imp);
                                }
                            }
                        }
                    }

                    getCentralRegistry().register(imp);

                })
                .onRemove(imp -> {
                    getCentralRegistry().remove(imp);

                });
    }

    public void addDictionary(Dictionary dict) {
        dictionaries.add(dict);
    }

    public Dictionary getDictionary(DictionaryId id) {
        return dictionaries.get(id);
    }

    public void updateDictionary(Dictionary dict) {
        dictionaries.update(dict);
    }

    public void removeDictionary(DictionaryId id) {
        dictionaries.remove(id);
    }

    public EncodedDictionary getPrimaryDictionary() {
        return new EncodedDictionary(dictionaries.get(ConqueryConstants.getPrimaryDictionary(getDataset())), StringTypeEncoded.Encoding.UTF8);
    }

    public void addImport(Import imp) {
        imports.add(imp);
    }

    public Import getImport(ImportId id) {
        return imports.get(id);
    }

    public Collection<Import> getAllImports() {
        return imports.getAll();
    }

    public void updateImport(Import imp) {
        imports.update(imp);
    }

    public void removeImport(ImportId id) {
        imports.remove(id);
    }

    public Dataset getDataset() {
        return dataset.get();
    }

    public void updateDataset(Dataset dataset) {
        this.dataset.update(dataset);
    }

    public List<Table> getTables() {
        return new ArrayList<>(tables.getAll());
    }

    public Table getTable(TableId tableId) {
        return tables.get(tableId);
    }

    public void addTable(Table table) {
        tables.add(table);
    }

    public void removeTable(TableId table) {
        tables.remove(table);
    }

    public List<SecondaryIdDescription> getSecondaryIds() {
        return new ArrayList<>(secondaryIds.getAll());
    }

    public SecondaryIdDescription getSecondaryId(SecondaryIdDescriptionId descriptionId) {
        return secondaryIds.get(descriptionId);
    }

    public void addSecondaryId(SecondaryIdDescription secondaryIdDescription) {
        secondaryIds.add(secondaryIdDescription);
    }

    public void removeSecondaryId(SecondaryIdDescriptionId secondaryIdDescriptionId) {
        secondaryIds.remove(secondaryIdDescriptionId);
    }

    public Concept<?> getConcept(ConceptId id) {
        return concepts.get(id);
    }

    public boolean hasConcept(ConceptId id) {
        return concepts.get(id) != null;
    }

    @SneakyThrows
    public void updateConcept(Concept<?> concept) {
        concepts.update(concept);
    }

    public void removeConcept(ConceptId id) {
        concepts.remove(id);
    }

    public Collection<? extends Concept<?>> getAllConcepts() {
        return concepts.getAll();
    }

    public void close() throws IOException {
        dataset.close();
        secondaryIds.close();
        tables.close();
        dictionaries.close();
        imports.close();
        concepts.close();
    }
}