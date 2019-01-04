package com.bakdata.conquery.io.xodus.stores;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import javax.validation.Validator;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.Jackson;
import com.bakdata.conquery.io.jackson.JacksonUtil;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.models.exceptions.ValidatorHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MPStore <KEY, VALUE> implements Store<KEY, VALUE> {

	private final ObjectWriter valueWriter;
	private final ObjectWriter keyWriter;
	private ObjectReader	valueReader;
	private final ObjectReader	keyReader;
	private final Validator validator;
	private final XodusStore store;
	private final Class<VALUE> valueType;
	private final IStoreInfo storeInfo;
	
	@SuppressWarnings("unchecked")
	public MPStore(Validator validator, Environment env, IStoreInfo storeInfo) {
		this.storeInfo = storeInfo;
		store = new XodusStore(env, storeInfo);
		this.validator = validator;
		this.valueType = (Class<VALUE>)storeInfo.getValueType();
		this.valueWriter = Jackson.BINARY_MAPPER.writerFor(valueType);
		this.valueReader = Jackson.BINARY_MAPPER.readerFor(valueType);
		this.keyWriter = Jackson.BINARY_MAPPER.writerFor(storeInfo.getKeyType());
		this.keyReader = Jackson.BINARY_MAPPER.readerFor(storeInfo.getKeyType());
	}

	@Override
	public void close() throws IOException {
		store.close();
	}
	
	@Override
	public void add(KEY key, VALUE value) throws JSONException {
		if(!valueType.isInstance(value)) {
			throw new IllegalStateException("The element "+value+" is not of the required type "+valueType);
		}
		ValidatorHelper.failOnError(log, validator.validate(value), "encoding "+value.getClass().getSimpleName());
		
		store.add(writeKey(key), writeValue(value));
	}

	@Override
	public VALUE get(KEY key) {
		return readValue(store.get(writeKey(key)));
	}

	@Override
	public void forEach(Consumer<StoreEntry<KEY, VALUE>> consumer) {
		final StoreEntry<KEY, VALUE> entry = new StoreEntry<>();
		store.forEach((k,v) -> {
			entry.setKey(readKey(k));
			entry.setByteSize(v.getLength());
			entry.setValue(readValue(v));
			consumer.accept(entry);
		});
	}

	@Override
	public void update(KEY key, VALUE value) throws JSONException {
		if(!valueType.isInstance(value)) {
			throw new IllegalStateException("The element "+value+" is not of the required type "+valueType);
		}
		ValidatorHelper.failOnError(log, validator.validate(value), "encoding "+value.getClass().getSimpleName());
		
		store.update(writeKey(key), writeValue(value));
	}
	
	@Override
	public void remove(KEY key) {
		store.remove(writeKey(key));
	}

	private ByteIterable writeValue(VALUE value) {
		return write(valueWriter, value);
	}

	private ByteIterable writeKey(KEY key) {
		return write(keyWriter, key);
	}
	
	private VALUE readValue(ByteIterable value) {
		return read(valueReader, value);
	}
	
	private KEY readKey(ByteIterable key) {
		return read(keyReader, key);
	}
	
	private ByteIterable write(ObjectWriter writer, Object obj) {
		try {
			byte[] bytes = writer.writeValueAsBytes(obj);
			if(log.isTraceEnabled()) {
				String json = JacksonUtil.toJsonDebug(bytes);
				log.trace("Written Messagepack ({}): {}", valueType.getName(), json);
			}
			return new ArrayByteIterable(bytes);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to write "+obj, e);
		}
	}
	
	private <T> T read(ObjectReader reader, ByteIterable obj) {
		if(obj==null) {
			return null;
		}
		try {
			return reader.readValue(obj.getBytesUnsafe(), 0, obj.getLength());
		} catch (IOException e) {
			throw new RuntimeException("Failed to read "+JacksonUtil.toJsonDebug(obj.getBytesUnsafe()), e);
		}
	}
	
	@Override
	public void fillCache() {}

	@Override
	public int count() {
		return store.count();
	}

	@Override
	public Collection<VALUE> getAll() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void inject(Injectable injectable) {
		valueReader = injectable.injectInto(valueReader);
	}
	
	@Override
	public String toString() {
		return storeInfo.getXodusName()+"("+storeInfo.getValueType().getSimpleName()+")";
	}
}
