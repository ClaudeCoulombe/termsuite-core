
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

package fr.univnantes.termsuite.test.unit.engines.gatherer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.junit.Before;
import org.junit.Test;

import fr.univnantes.termsuite.api.TermSuite;
import fr.univnantes.termsuite.engines.SimpleEngine;
import fr.univnantes.termsuite.engines.gatherer.GathererOptions;
import fr.univnantes.termsuite.engines.gatherer.GraphicalGatherer;
import fr.univnantes.termsuite.framework.TermSuiteFactory;
import fr.univnantes.termsuite.index.Terminology;
import fr.univnantes.termsuite.model.IndexedCorpus;
import fr.univnantes.termsuite.model.Lang;
import fr.univnantes.termsuite.model.Term;
import fr.univnantes.termsuite.model.TermProperty;
import fr.univnantes.termsuite.test.mock.TermFactory;
import fr.univnantes.termsuite.test.unit.UnitTests;
import fr.univnantes.termsuite.test.util.TermSuiteExtractors;

public class GraphicalVariantGathererSpec {
	
	
	private GathererOptions options;
	private IndexedCorpus corpus;
	private Terminology termino;
	private Term tetetete;
	private Term tetetetx;
	private Term teteteteAccent;
	private Term abcdefghijkl;
	private Term abcdefghijkx;
	private Term abcdefghijklCapped;
	
	@Before
	public void setup() {
		options = TermSuite.getDefaultExtractorConfig(Lang.EN).getGathererConfig();
		corpus = TermSuiteFactory.createIndexedCorpus(Lang.FR, "");
		termino = corpus.getTerminology();
		TermFactory termFactory = new TermFactory(termino);
		tetetete = termFactory.create("N:tetetete|tetetete");
		tetetete.setProperty(TermProperty.SPECIFICITY, 1d);
		tetetetx = termFactory.create("N:tetetetx|tetetetx");
		tetetetx.setProperty(TermProperty.SPECIFICITY, 2d);
		teteteteAccent = termFactory.create("N:tétetete|tétetete");
		teteteteAccent.setProperty(TermProperty.SPECIFICITY, 3d);
		abcdefghijklCapped = termFactory.create("N:Abcdefghijkl|Abcdefghijkl");
		abcdefghijklCapped.setProperty(TermProperty.SPECIFICITY, 4d);
		abcdefghijkl = termFactory.create("N:abcdefghijkl|abcdefghijkl");
		abcdefghijkl.setProperty(TermProperty.SPECIFICITY, 5d);
		abcdefghijkx = termFactory.create("N:abcdefghijkx|abcdefghijkx");
		abcdefghijkx.setProperty(TermProperty.SPECIFICITY, 6d);
	}


	private SimpleEngine makeAE(double similarityThreashhold) throws Exception {
		options.setGraphicalSimilarityThreshold(similarityThreashhold);
		return UnitTests.createSimpleEngine(corpus, GraphicalGatherer.class, options);
	}

	@Test
	public void testCaseInsensitive() throws  Exception {
		makeAE( 1.0d).execute();

		assertThat(UnitTests.outRels(termino, this.abcdefghijkl)).hasSize(1);
		assertThat(UnitTests.outRels(termino, this.abcdefghijklCapped)).hasSize(0);
		
		assertThat(UnitTests.outRels(termino, this.abcdefghijkl))
			.hasSize(1)
			.extracting("to")
			.contains(this.abcdefghijklCapped);
	}


	@Test
	public void testWithDiacritics() throws AnalysisEngineProcessException, Exception {
		makeAE( 1.0d).execute();
		assertThat(UnitTests.outRels(termino, this.tetetete))
			.hasSize(0);

		assertThat(UnitTests.outRels(termino, this.teteteteAccent))
			.hasSize(1)
			.extracting(TermSuiteExtractors.VARIATION_TYPE_TO)
			.contains(tuple("g", this.tetetete));
	}
	
	

	@Test
	public void testWith0_9() throws AnalysisEngineProcessException, Exception {
		makeAE( 0.9d).execute();
		assertThat(UnitTests.outRels(termino, this.abcdefghijkx))
			.hasSize(2)
			.extracting("to")
			.contains(this.abcdefghijkl, this.abcdefghijklCapped);
		
		assertThat(UnitTests.outRels(termino, this.teteteteAccent))
			.hasSize(1)
			.extracting(TermSuiteExtractors.VARIATION_TYPE_TO)
			.contains(
					tuple("g", this.tetetete)
					);
	}

	
	@Test
	public void testWith0_8() throws AnalysisEngineProcessException, Exception {
		makeAE(0.8d).execute();
		assertThat(UnitTests.outRels(termino, this.abcdefghijklCapped))
			.hasSize(0);
		
		assertThat(UnitTests.outRels(termino, this.abcdefghijkl))
			.hasSize(1)
			.extracting("to")
			.contains(this.abcdefghijklCapped);

		assertThat(UnitTests.outRels(termino, this.abcdefghijkx))
			.hasSize(2)
			.extracting("to")
			.contains(this.abcdefghijklCapped, this.abcdefghijkl);
	}
}
