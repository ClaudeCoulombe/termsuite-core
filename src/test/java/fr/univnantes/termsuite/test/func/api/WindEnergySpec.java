
/*******************************************************************************
 * Copyright 2015-2016 - CNRS (Centre National de Recherche Scientifique)
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *******************************************************************************/

package fr.univnantes.termsuite.test.func.api;

import static fr.univnantes.termsuite.test.asserts.TermSuiteAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import fr.univnantes.termsuite.api.ExtractorOptions;
import fr.univnantes.termsuite.api.TermSuite;
import fr.univnantes.termsuite.framework.PipelineStats;
import fr.univnantes.termsuite.index.Terminology;
import fr.univnantes.termsuite.model.IndexedCorpus;
import fr.univnantes.termsuite.model.Lang;
import fr.univnantes.termsuite.model.RelationType;
import fr.univnantes.termsuite.test.func.FunctionalTests;
import fr.univnantes.termsuite.test.unit.UnitTests;
import fr.univnantes.termsuite.test.util.TermSuiteExtractors;
import fr.univnantes.termsuite.tools.ControlFiles;
import fr.univnantes.termsuite.tools.ControlFilesGenerator;
import fr.univnantes.termsuite.uima.ResourceType;

public abstract class WindEnergySpec {

	private static final Logger LOGGER = LoggerFactory.getLogger(WindEnergySpec.class);
	
	protected IndexedCorpus corpus = null;
	protected Terminology termino = null;
	protected Lang lang;
	protected List<String> notTestedRules = Lists.newArrayList();
	protected List<String> syntacticMatchingRules = Lists.newArrayList();
	protected List<String> syntacticNotMatchingRules = Lists.newArrayList();

	public WindEnergySpec() {
		super();
		this.lang = getLang();
		this.notTestedRules = getRulesNotTested();
		this.syntacticMatchingRules = getSyntacticMatchingRules();
		this.syntacticNotMatchingRules = getSyntacticNotMatchingRules();
	}
	
	protected abstract Lang getLang();
	protected abstract List<String> getRulesNotTested();
	protected abstract List<String> getSyntacticMatchingRules();
	protected abstract List<String> getSyntacticNotMatchingRules();


	protected void expectNotMatchingRules(String... rules) {
		for(String rule:rules)
			syntacticNotMatchingRules.add(rule);
	}


	protected void expectMatchingRules(String... rules) {
		for(String rule:rules)
			syntacticMatchingRules.add(rule);		
	}
	
	private static final LoadingCache<Lang, IndexedCorpus> TERMINOLOGY_CACHE = CacheBuilder.newBuilder()
				.maximumSize(1)
				.build(new CacheLoader<Lang, IndexedCorpus>() {
					@Override
					public IndexedCorpus load(Lang lang) throws Exception {
						IndexedCorpus corpus = runPipeline(lang);
						File controlDir = FunctionalTests.getFunctionalTestsControlDir().resolve("we-" + lang.getCode()).toFile();
						controlDir.mkdirs();
						new ControlFilesGenerator(UnitTests.getTerminologyService(corpus)).generate(controlDir);
						return corpus;
					}
				});
	
	
	@Before
	public void setup() {
		this.corpus = TERMINOLOGY_CACHE.getUnchecked(lang);
		this.termino = this.corpus.getTerminology();
	}
	
	protected static IndexedCorpus runPipeline(Lang lang) throws IOException {
//		ClearTempFiles.main(new String[0]);
		
		IndexedCorpus corpus = TermSuite.preprocessor()
			.setPreprocessedCorpusCache(FunctionalTests.getCachedWindEnergyPreprocessedCorpusFile(lang))
			.setTaggerPath(FunctionalTests.getTaggerPath())
			.toIndexedCorpus(FunctionalTests.getCorpusWE(lang), 5000000);
		
		ExtractorOptions extractorOptions = TermSuite.getDefaultExtractorConfig(lang);
		extractorOptions.getPostProcessorConfig().setEnabled(false);
		extractorOptions.getGathererConfig().setMergerEnabled(false);
		PipelineStats stats = TermSuite.terminoExtractor()
					.setOptions(extractorOptions)
					.execute(corpus);
		LOGGER.info("Pipeline finished in {} ms", stats.getTotalTime());
		LOGGER.info("Total indexing time: {} ms", stats.getIndexingTime());
		LOGGER.info("Engine Stats: \n{}", stats.getEngineStats());
		return corpus;
	}

	@Test
	public void weControlSyntacticMatchingRules() {
		try(InputStream openStream = ResourceType.VARIANTS.fromClasspath(lang).openStream()) {
			@SuppressWarnings("unchecked")
			Set<String> ruleNames = (Set<String>) ((Map<?, ?>)new Yaml().load(openStream)).keySet();
			
			assertThat(ruleNames)
				.containsAll(syntacticMatchingRules);
			
			syntacticMatchingRules.stream().forEach(rule-> {
				assertTrue(String.format("Bad rule name. Rule <%s> does not exist", rule),
						ruleNames.contains(rule));
			});
	
			syntacticNotMatchingRules.stream().forEach(rule-> {
				assertTrue(String.format("Bad rule name. Rule <%s> does not exist", rule),
						ruleNames.contains(rule));
			});
	
			ruleNames.removeAll(syntacticMatchingRules);
			ruleNames.removeAll(syntacticNotMatchingRules);
			ruleNames.removeAll(notTestedRules);

			assertTrue(String.format("Bad rule list. Some rule are not under test: <%s>", ruleNames),
					ruleNames.isEmpty());
		
			assertThat(termino)
				.asMatchingRules()
				.containsAll(syntacticMatchingRules)
				.doesNotContainAnyElementsOf(syntacticNotMatchingRules);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		
	}

	@Test
	public void weControlPrefixes() {
		assertThat(termino)
			.asTermVariations(RelationType.IS_PREFIX_OF)
			.extracting("from.groupingKey", "to.groupingKey")
			.containsOnly(
					ControlFiles.prefixVariationTuples(lang, "we")
			);
	}

	@Test
	public void weControlDerivates() {
		assertThat(termino)
		.asTermVariations(RelationType.DERIVES_INTO)
		.extracting(TermSuiteExtractors.RELATION_DERIVTYPE_FROMGKEY_TOGKEY)
		.containsOnly(
				ControlFiles.derivateVariationTuples(lang, "we")
		);
	}

	@Test
	public void weCompounds() throws FileNotFoundException {
		assertThat(termino)
			.hasExpectedCompounds(
					FunctionalTests.getTestsOutputFile(String.format("compounds-we-%s.txt", lang.getCode())),
					ControlFiles.compoundTuples(lang, "we"));
	}
}
