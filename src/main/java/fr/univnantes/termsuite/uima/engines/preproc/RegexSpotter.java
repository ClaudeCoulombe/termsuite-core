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
package fr.univnantes.termsuite.uima.engines.preproc;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import fr.univnantes.lina.uima.tkregex.LabelledAnnotation;
import fr.univnantes.lina.uima.tkregex.RegexOccurrence;
import fr.univnantes.lina.uima.tkregex.ae.TokenRegexAE;
import fr.univnantes.termsuite.resources.OccurrenceFilter;
import fr.univnantes.termsuite.resources.TrueFilter;
import fr.univnantes.termsuite.types.TermOccAnnotation;
import fr.univnantes.termsuite.types.WordAnnotation;
import fr.univnantes.termsuite.utils.JCasUtils;
import fr.univnantes.termsuite.utils.TermHistory;
import fr.univnantes.termsuite.utils.TermSuiteConstants;
import fr.univnantes.termsuite.utils.TermSuiteUtils;
import fr.univnantes.termsuite.utils.TermUtils;
import uima.sandbox.filter.resources.FilterResource;


/**
 * 
 * Adds all Token Regex Occurrences to the Cas and to the Term Index.
 * 
 * @author Damien Cram
 *
 */
public class RegexSpotter extends TokenRegexAE {
	private static final Logger LOGGER = LoggerFactory.getLogger(RegexSpotter.class);
	
	public static final String LOG_OVERLAPPING_RULES = "LogOverlappingRules";
	@ConfigurationParameter(name = LOG_OVERLAPPING_RULES, mandatory = false, defaultValue = "false")
	private boolean logOverlappingRules;

	public static final String CONTEXTUALIZE = "Contextualize";
	@ConfigurationParameter(name = CONTEXTUALIZE, mandatory = false, defaultValue = "false")
	private boolean contextualize;

	public static final String CHARACTER_FOOTPRINT_TERM_FILTER = "CharacterFootprintTermFilter";
	@ExternalResource(key =CHARACTER_FOOTPRINT_TERM_FILTER, mandatory = false)
	private OccurrenceFilter termFilter = TrueFilter.INSTANCE;
	
	public static final String STOP_WORD_FILTER = "StopWordFilter";

	@ExternalResource(key =STOP_WORD_FILTER, mandatory = true)
	private FilterResource stopWordFilter;
	
	private Stopwatch sw;

	private AtomicLong totalTimeInMillis = new AtomicLong(0);
	
	@Override
	protected void beforeRuleProcessing(JCas jCas) {
		this.sw = Stopwatch.createStarted();
		this.occurrenceBuffer = new OccurrenceBuffer();
	}
	
	private OccurrenceBuffer occurrenceBuffer;
	
	private Optional<TermHistory> history = Optional.empty();
	
	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
	}
	
	@Override
	public void ruleMatched(JCas jCas, RegexOccurrence occurrence) {
		String groupingKey = TermSuiteUtils.toGroupingKey(occurrence);
		
		/*
		 * Do not keep the term if it has too many bad characters
		 */
		if(!termFilter.accept(occurrence)) {
			watchCharacterFiltered(jCas, occurrence, groupingKey);
			return;
		}

		/*
		 * Do not keep the term if it is a stop word
		 */
		WordAnnotation wa = (WordAnnotation)occurrence.getLabelledAnnotations().get(0).getAnnotation();
		if((occurrence.size() == 1 && stopWordFilter.getFilters().contains(wa.getCoveredText().toLowerCase()))
				|| (occurrence.size() == 1 && wa.getLemma() != null && stopWordFilter.getFilters().contains(wa.getLemma().toLowerCase()))) {
			watchStopWordFiltered(jCas, occurrence, groupingKey);
			return;
		}
		
		if(history.isPresent())
			watchOccurrence(jCas, occurrence, groupingKey);
			
		/*
		 * Add the occurrence the buffer. Will be added to jCas if it is not filtered by any post processing strategy
		 */
		this.occurrenceBuffer.bufferize(occurrence);
	}

	public void watchOccurrence(JCas jCas, RegexOccurrence occurrence, String groupingKey) {
		if(history.get().isStringWatched(groupingKey))
			history.get().saveEvent(
					groupingKey, 
					RegexSpotter.class, String.format("Term spotted at [%d,%d] in file %s",
							occurrence.getBegin(), occurrence.getEnd(),
							JCasUtils.getSourceDocumentAnnotation(jCas).get().getUri()));
	}

	public void watchStopWordFiltered(JCas jCas, RegexOccurrence occurrence, String groupingKey) {
		if(history.isPresent())
			if(history.get().isStringWatched(groupingKey))
				history.get().saveEvent(
						groupingKey, 
						RegexSpotter.class, String.format(
								"[!] Term spotted at [%d,%d] in file %s but filtered out by stop word filter",
								occurrence.getBegin(), occurrence.getEnd(),
								JCasUtils.getSourceDocumentAnnotation(jCas).get().getUri(),
								termFilter.getClass()));
	}

	public void watchCharacterFiltered(JCas jCas, RegexOccurrence occurrence, String groupingKey) {
		if(history.isPresent())
			if(history.get().isStringWatched(groupingKey))
				history.get().saveEvent(
						groupingKey, 
						RegexSpotter.class, String.format(
								"[!] Term spotted at [%d,%d] in file %s but filtered out by %s",
								occurrence.getBegin(), occurrence.getEnd(),
								JCasUtils.getSourceDocumentAnnotation(jCas).get().getUri(),
								termFilter.getClass()));
	}

	@Override
	protected void allRulesFailed(JCas jCas) {
		flushOccurrenceBuffer(jCas);
	}
	
	/*
	 * 
	 */
	private void flushOccurrenceBuffer(JCas jCas) {
		
		/*
		 * Log a warning if the occurrence was found for another rule
		 */
		if(logOverlappingRules) {
			for(Collection<RegexOccurrence> doublons:this.occurrenceBuffer.findDuplicates()) {
				Iterator<RegexOccurrence> it = doublons.iterator();
				RegexOccurrence base = it.next();
				while(it.hasNext()) {
					RegexOccurrence occ = it.next();
					LOGGER.warn("Rules {} and {} overlap on occurrence [{},{}] \"{}\"", 
							base.getRule().getName(),
							occ.getRule().getName(),
							occ.getBegin(),
							occ.getEnd(),
							TermUtils.collapseText(jCas.getDocumentText().substring(occ.getBegin(), occ.getEnd()))
						);
				}
			}
		}
		
		for(RegexOccurrence occ:this.occurrenceBuffer) 
			addOccurrenceToCas(jCas, occ);
		this.occurrenceBuffer.clear();
	}
	
	private void addOccurrenceToCas(JCas jCas, RegexOccurrence occurrence) {
		TermOccAnnotation annotation = (TermOccAnnotation) jCas
				.getCas().createAnnotation(
						jCas.getCasType(TermOccAnnotation.type),
						occurrence.getBegin(),
						occurrence.getEnd());
		
		StringArray patternFeature = new StringArray(jCas, occurrence.size());
		FSArray innerWords = new FSArray(jCas, occurrence.size());
		StringBuilder termLemma = new StringBuilder();
		int i = 0;
		for (LabelledAnnotation la:occurrence.getLabelledAnnotations()) {
			patternFeature.set(i, la.getLabel());
			WordAnnotation word = (WordAnnotation) la.getAnnotation();
			termLemma.append(word.getLemma());
			if(i<occurrence.size()-1)
				termLemma.append(TermSuiteConstants.WHITESPACE);
			WordAnnotation wordAnno = (WordAnnotation) la.getAnnotation();
			if(wordAnno.getRegexLabel() != null) {
				if(!wordAnno.getRegexLabel().equals(la.getLabel())) {
					LOGGER.warn("Another label has already been set for WordAnnotation "+wordAnno.getCoveredText()+":"+wordAnno.getRegexLabel()+" ["+wordAnno.getBegin()+","+wordAnno.getEnd()+"]. Ignoring the new label "+la.getLabel()+" (rule: "+occurrence.getRule().getName()+")");
				}
			} else
				wordAnno.setRegexLabel(la.getLabel());
			innerWords.set(i, wordAnno);
			i++;
		}
		
		annotation.setWords(innerWords);
		annotation.setPattern(patternFeature);
		annotation.setSpottingRuleName(occurrence.getRule().getName());
		annotation.setTermKey(TermSuiteUtils.getGroupingKey(annotation));
		annotation.addToIndexes();
	}
	
	@Override
	protected void afterRuleProcessing(JCas jCas) {
		this.sw.stop();
		totalTimeInMillis.addAndGet(this.sw.elapsed(TimeUnit.MILLISECONDS));
		LOGGER.debug("Processed MWT spotting on doc {} in {}ms [Cumulated: {}ms]", 
				JCasUtils.getSourceDocumentAnnotation(jCas).get().getUri(), 
				sw.elapsed(TimeUnit.MILLISECONDS), totalTimeInMillis.get());
		flushOccurrenceBuffer(jCas);
	}
	
}