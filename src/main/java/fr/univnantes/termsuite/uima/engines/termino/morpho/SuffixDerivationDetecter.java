
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

package fr.univnantes.termsuite.uima.engines.termino.morpho;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.univnantes.termsuite.model.RelationProperty;
import fr.univnantes.termsuite.model.RelationType;
import fr.univnantes.termsuite.model.Term;
import fr.univnantes.termsuite.model.TermIndex;
import fr.univnantes.termsuite.model.TermRelation;
import fr.univnantes.termsuite.model.TermWord;
import fr.univnantes.termsuite.model.termino.CustomTermIndex;
import fr.univnantes.termsuite.model.termino.TermValueProviders;
import fr.univnantes.termsuite.resources.SuffixDerivation;
import fr.univnantes.termsuite.uima.resources.TermHistoryResource;
import fr.univnantes.termsuite.uima.resources.termino.SuffixDerivationList;
import fr.univnantes.termsuite.uima.resources.termino.TermIndexResource;
import fr.univnantes.termsuite.utils.TermHistory;

public class SuffixDerivationDetecter extends JCasAnnotator_ImplBase {
	private static final Logger LOGGER = LoggerFactory.getLogger(SuffixDerivationDetecter.class);

	private static final String LEMMA_INDEX = "LemmaIndex";
	public static final String TASK_NAME = "Morphosyntactic analysis (suffix derivation)";

	@ExternalResource(key=TermIndexResource.TERM_INDEX, mandatory=true)
	private TermIndexResource termIndexResource;
	
	@ExternalResource(key=SuffixDerivationList.SUFFIX_DERIVATIONS, mandatory=true)
	private SuffixDerivationList suffixDerivationList;

	@ExternalResource(key =TermHistoryResource.TERM_HISTORY, mandatory = true)
	private TermHistoryResource historyResource;

	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		// do nothing
	}
	
	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		LOGGER.info("Starting {} for TermIndex {}", TASK_NAME, this.termIndexResource.getTermIndex().getName());
		TermIndex termIndex = termIndexResource.getTermIndex();
		CustomTermIndex lemmaIndex = termIndex.createCustomIndex(
				LEMMA_INDEX, 
				TermValueProviders.TERM_LEMMA_LOWER_CASE_PROVIDER);
		
		int nbDerivations = 0, nbSwt = 0;
		TermWord candidateDerivateTermWord, baseTermWord;
		for(Term swt:termIndex.getTerms()) {
			if(!swt.isSingleWord())
				continue;
			nbSwt++;
			candidateDerivateTermWord = swt.getWords().get(0);
			List<SuffixDerivation> derivations = suffixDerivationList.getDerivationsFromDerivateForm(candidateDerivateTermWord);
			for(SuffixDerivation suffixDerivation:derivations) {
				if(suffixDerivation.isKnownDerivate(candidateDerivateTermWord)) {
					baseTermWord = suffixDerivation.getBaseForm(candidateDerivateTermWord);
					List<Term> baseTerms = lemmaIndex.getTerms(baseTermWord.getWord().getLemma());
					for(Term baseTerm:baseTerms) {
						if(baseTerm.getWords().get(0).equals(baseTermWord)) {
							nbDerivations++;
							if(LOGGER.isTraceEnabled())
								LOGGER.trace("Found derivation base: {} for derivate word {}", baseTerm, swt);
							TermRelation relation = new TermRelation(RelationType.DERIVES_INTO, baseTerm, swt);
							relation.setProperty(RelationProperty.DERIVATION_TYPE, suffixDerivation.getType());
							termIndex.addRelation(relation);
							watch(swt, baseTerm);
						}
					}
				}
			}
		}
		
		termIndex.dropCustomIndex(LEMMA_INDEX);
		
		LOGGER.debug("Number of derivations found: {} out of {} SWTs", 
				nbDerivations, 
				nbSwt);
	}

	private void watch(Term swt, Term baseTerm) {
		TermHistory history = historyResource.getHistory();
		if(history.isWatched(swt.getGroupingKey())) 
			history.saveEvent(
				swt.getGroupingKey(), 
				this.getClass(), 
				"Term is a derivate of term " + baseTerm);
		
		if(history.isWatched(baseTerm.getGroupingKey())) 
			history.saveEvent(
				baseTerm.getGroupingKey(), 
				this.getClass(), 
				"Term has a new found derivate: " + swt);

	}
}