
package com.bakdata.conquery.util.dict;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.powerlibraries.io.In;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SuccinctTrieBenchmark {

	public static List<Arguments> data() throws IOException {
		List<byte[]> list = In.resource(SuccinctTrieBenchmark.class, "SuccinctTrieTest.data").streamLines().map(v->v.getBytes(StandardCharsets.UTF_8)).collect(Collectors.toList());
		int size = list.size();
		for(int i=0;i<60;i++) {
			for(int j=0;j<size;j++) {
				list.add(ArrayUtils.add(list.get(j), (byte)i));
			}
		}
		
		return Arrays.<Arguments>asList(
			
			Arguments.of(
				"succinct",
				new SuccinctTrie(),
				list
			)/*,
			Arguments.of(
				"ternarytree",
				new SuccinctTrie2(),
				list
			)*/
		);
	}

	//@ParameterizedTest(name = "{0}") @MethodSource("data")
	public void test(String name, SuccinctTrie trie, List<byte[]> data) throws IOException {
		data.forEach(trie::add);
		trie.compress();
	}
}