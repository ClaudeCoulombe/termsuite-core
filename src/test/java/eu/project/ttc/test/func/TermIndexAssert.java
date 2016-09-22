
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

package eu.project.ttc.test.func;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.util.Lists;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

import eu.project.ttc.models.Term;
import eu.project.ttc.models.TermIndex;
import eu.project.ttc.models.TermVariation;
import eu.project.ttc.models.VariationType;
import eu.project.ttc.utils.TermIndexUtils;

public class TermIndexAssert extends AbstractAssert<TermIndexAssert, TermIndex> {

	public TermIndexAssert(TermIndex actual) {
		super(actual, TermIndexAssert.class);
	}

	public TermIndexAssert hasSize(int expected) {
		if(actual.getTerms().size() != expected)
			failWithMessage("Expected size was <%s>, but actual size is <%s>.", 
					expected, actual.getTerms().size());
		return this;
	}

	public TermIndexAssert containsTerm(String expectedTerm, int frequency) {
		for(Term t:actual.getTerms()) {
			if(t.getGroupingKey().equals(expectedTerm)) {
				if(t.getFrequency() != frequency)
					failWithMessage("Expected frequency for term %s was <%s>, but actually is: <%s>.",
							expectedTerm,
							frequency,
							t.getFrequency());
				return this;
			}
		}
		failWithMessage("No such term <%s> found in term index.", expectedTerm);
		return this;
	}
	
	public TermIndexAssert containsTerm(String expectedTerm) {
		for(Term t:actual.getTerms()) {
			if(t.getGroupingKey().equals(expectedTerm))
				return this;
		}
		
		failWithMessage("No such term <%s> found in term index.", expectedTerm);
		return this;
	}
	
	public TermIndexAssert containsVariation(String baseGroupingKey, VariationType type, String variantGroupingKey) {
		if(failToFindTerms(baseGroupingKey, variantGroupingKey))
			return this;
		
		
		List<TermVariation> potentialVariations = Lists.newArrayList();
		Set<TermVariation> sameType = Sets.newHashSet();
		
		for(TermVariation tv:getVariations()) {
			if(tv.getBase().getGroupingKey().equals(baseGroupingKey)
					&& tv.getVariant().getGroupingKey().equals(variantGroupingKey)) {
				potentialVariations.add(tv);
				if(tv.getVariationType() == type)
					return this;
			}
			if(type == tv.getVariationType())
				sameType.add(tv);
		}
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(baseGroupingKey).getVariations(type)));
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(variantGroupingKey).getBases(type)));
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(baseGroupingKey).getVariations()));
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(variantGroupingKey).getBases()));
		potentialVariations.addAll(sameType);
		
		failWithMessage("No such variation <%s--%s--%s> found in term index. Closed variations: <%s>", 
				baseGroupingKey, type, variantGroupingKey,
				Joiner.on(", ").join(potentialVariations.subList(0, Ints.min(10, potentialVariations.size())))
				);
		return this;
	}

	private boolean failToFindTerms(String... groupingKeys) {
		boolean failed = false;
		for(String gKey:groupingKeys) {
			if(actual.getTermByGroupingKey(gKey) == null) {
				failed = true;
				failWithMessage("Could not find term <%s> in termIndex", gKey);
			}
		}
		return failed;
	}
	
	public TermIndexAssert containsVariation(String baseGroupingKey, VariationType type, String variantGroupingKey, Object info) {
		if(failToFindTerms(baseGroupingKey, variantGroupingKey))
			return this;

		
		List<TermVariation> potentialVariations = Lists.newArrayList();
		Set<TermVariation> sameType = Sets.newHashSet();
		for(TermVariation tv:getVariations()) {
			if(tv.getBase().getGroupingKey().equals(baseGroupingKey)
					&& tv.getVariant().getGroupingKey().equals(variantGroupingKey)) {
				potentialVariations.add(tv);
				if(tv.getVariationType() == type && Objects.equal(info, tv.getInfo()))
					return this;
			}
			if(type == tv.getVariationType())
				sameType.add(tv);
		}
		
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(baseGroupingKey).getVariations(type)));
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(variantGroupingKey).getBases(type)));
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(baseGroupingKey).getVariations()));
		potentialVariations.addAll(Sets.newHashSet(actual.getTermByGroupingKey(variantGroupingKey).getBases()));
		potentialVariations.addAll(sameType);
		
		failWithMessage("No such variation <%s--%s[%s]--%s> found in term index. Closed variations: <%s>", 
				baseGroupingKey, type, 
				info,
				variantGroupingKey,
				Joiner.on(", ").join(potentialVariations)
				);
		return this;
	}


	private Collection<TermVariation> getVariations() {
		Set<TermVariation> termVariations = Sets.newHashSet();
		for(Term t:actual.getTerms()) {
			for(TermVariation v:t.getVariations())
				termVariations.add(v);
		}
		return termVariations;
		
	}

	public TermIndexAssert hasNVariationsOfType(int expected, VariationType type) {
		int cnt = 0;
		for(TermVariation tv:getVariations()) {
			if(tv.getVariationType() == type)
				cnt++;
		}
	
		if(cnt != expected)
			failWithMessage("Expected <%s> variations of type <%s>. Got: <%s>", expected, type, cnt);
		
		return this;
	}

	public AbstractIterableAssert<?, ? extends Iterable<? extends TermVariation>, TermVariation> asTermVariationsHavingObject(Object object) {
		Set<TermVariation> variations = Sets.newHashSet();
		for(TermVariation v:getVariations())
			if(Objects.equal(v.getInfo(), object))
				variations.add(v);
		return assertThat(variations);
	}

	public AbstractIterableAssert<?, ? extends Iterable<? extends TermVariation>, TermVariation> asTermVariations(VariationType... variations) {
		return assertThat(
				TermIndexUtils.selectTermVariations(actual, variations));
	}

	public AbstractIterableAssert<?, ? extends Iterable<? extends Term>, Term> asCompoundList() {
		return assertThat(
				TermIndexUtils.selectCompounds(actual));
	}

	public AbstractIterableAssert<?, ? extends Iterable<? extends String>, String> asMatchingRules() {
		Set<String> matchingRuleNames = Sets.newHashSet();
		for(TermVariation tv:TermIndexUtils.selectTermVariations(actual, VariationType.SYNTACTICAL, VariationType.MORPHOLOGICAL)) 
			matchingRuleNames.add((String)tv.getInfo());
		return assertThat(matchingRuleNames);
	}


}
