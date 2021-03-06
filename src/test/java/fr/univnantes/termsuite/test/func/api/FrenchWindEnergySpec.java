
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
import static fr.univnantes.termsuite.test.func.FunctionalTests.termsByProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.Test;

import fr.univnantes.termsuite.api.TermSuite;
import fr.univnantes.termsuite.api.TerminologyStats;
import fr.univnantes.termsuite.engines.gatherer.VariationType;
import fr.univnantes.termsuite.index.TermIndex;
import fr.univnantes.termsuite.index.TermIndexType;
import fr.univnantes.termsuite.index.TermIndexValueProvider;
import fr.univnantes.termsuite.model.CompoundType;
import fr.univnantes.termsuite.model.Lang;
import fr.univnantes.termsuite.model.RelationType;
import fr.univnantes.termsuite.model.Term;
import fr.univnantes.termsuite.model.TermProperty;
import fr.univnantes.termsuite.model.Word;
import fr.univnantes.termsuite.test.util.TermSuiteExtractors;

public class FrenchWindEnergySpec extends WindEnergySpec {
	
	@Override
	protected Lang getLang() {
		return Lang.FR;
	}
	
	@Override
	protected List<String> getSyntacticMatchingRules() {
		return Lists.newArrayList(
				"S-NA",
				"NA-NprefA",
				"S-Ed-NA-A",
				"S-Ed-NA-AA",
				"S-Ed-NA-PN",
				"S-Ed-NA-PAN",
				"S-Ed-NA-PACAN",
				"S-Ed-NA-PNA",
				"S-Ed-NA-CA",
				"S-Ed-NA-,ACA",
				"S-I-NA-A",
				"S-I-N(N|A)-PN",
				"S-I-NA-R",
				"S-I-NA-V",
				"S-I-NA-AC",
				"S-I-NA-A,AC",
				"S-I-NA-PNA",
				"S-I1-NPN-A",
				"S-Ed-NPN-A",
				"S-Ed-NPN-PN",
				"S-Ed-NPN-PAN",
				"S-Ed-NPN-PNA",
				"S-Ed-NPN-PACAN",
				"S-Ed-NPN-AA",
				"S-Ed-NPN-CPN",
				"S-Ed-NPN-,PNCPN",
				"S-Eg-NA-NP",
				"S-R2-NPN",
				"S-R2I-NPN-P",
				"S-R2D-NPN",
				"S-P-NAPN-A",
				"S-P-NAA-A",
				"M-S-NN",
				"M-PI-EN-P",
				"M-I-NA-CE",
				"S-IEg-NA-A,-CA",
				"S-Eg-NPN-NP",
				
				/*
				 * Found only when term merging is disabled
				 */
				"M-I-NA-EC"
				);
	}
	
	@Override
	protected List<String> getSyntacticNotMatchingRules() {
		return Lists.newArrayList(
				"S-IEg-NPN-PN,-CPN",
				"S-I-NA-RV",
				"S-R2I2-NPN-PNP",
				"S-I2-NPN-PN,PNC",
				"S-PID-NA-P",
				"S-PID-NAA-P",
				"M-I2-NA");
	}
	
	@Override
	protected List<String> getRulesNotTested() {
		return Lists.newArrayList(
				"NA-NsynA",
				"NA-synNA",
				"NPN-NPsynN",
				"NPN-synNPN"
				);
	}
	
	@Test
	public void weNeoclassicalCompounds() {
		List<Word> neoclassicals = termino.getWords().values().stream()
			.filter(Word::isCompound)
			.filter(w -> w.getCompoundType() == CompoundType.NEOCLASSICAL).collect(Collectors.toList());
		
		assertThat(neoclassicals)
			.isNotEmpty()
			.extracting("lemma", "neoclassicalAffix.lemma")
			.contains(tuple("hydroélectrique", "eau"))
			.contains(tuple("antinucléaire", "anti"))
			.contains(tuple("aéroélastique", "air"))
			.hasSize(362);
	}

	
	@Test
	public void testTop10ByFreq() {
		assertThat(termsByProperty(termino, TermProperty.FREQUENCY, true).subList(0, 10))
			.hasSize(10)
			.extracting("groupingKey")
			.containsExactly(
					"n: puissance", "a: éolien", "n: système", "n: énergie", "n: vitesse", 
					"n: vent", "n: réseau", "n: éolienne", "n: machine", "n: figure");
	}
	
	@Test
	public void testCheckTerms() {
		assertThat(termino.getTerms().values())
			.extracting("groupingKey")
			.contains(
					"n: tourbillon"
					)
			.doesNotContain("npn: givrage de pale")
			;
	}

	@Test
	public void testTermHydroelectrique() {
		Term term = termino.getTerms().get("a: hydroélectrique");
		
		assertThat(term)
			.isCompound()
			.hasCompoundType(CompoundType.NEOCLASSICAL)
			.hasCompositionSubstrings("hydro", "électrique")
			.hasCompositionLemmas("eau", "électrique")
			;
	}


	@Test
	public void testTermElectromagnetique() {
		Term term = termino.getTerms().get("a: électromagnétique");
		
		assertThat(term)
			.isCompound()
			.hasCompoundType(CompoundType.NEOCLASSICAL)
			.hasCompositionSubstrings("électro", "magnétique");
	}

	@Test
	public void testTermVitesseDeRotation() {
		Term term = termino.getTerms().get("npn: vitesse de rotation");
		assertThat(term)
			.hasFrequency(308);
		
		assertThat(termino)
			.hasNVariationsOfType(term, 21, VariationType.SYNTAGMATIC)
			.getVariations(term)
			.extracting(TermSuiteExtractors.RELATION_TOGKEY_RULE_TOFREQ)
			.contains(
					tuple("napn: vitesse angulaire de rotation", "S-I1-NPN-A", 2),
					tuple("napn: vitesse nominal de rotation", "S-I1-NPN-A", 2),
					tuple("npna: vitesse de rotation correspondant", "S-Ed-NPN-A", 3),
					tuple("npnpna: vitesse de rotation du champ magnétique", "S-Ed-NPN-PNA", 2)
				);
	}

	@Test
	public void testTermEolienne() {
		assertThat(termino.getTerms().get("n: éolienne"))
				.hasFrequency(1102)
				.hasGroupingKey("n: éolienne");
	}
	
	
	@Test
	public void testTop10ByWR() {
		assertThat(termsByProperty(termino, TermProperty.SPECIFICITY, true).subList(0, 10))
			.hasSize(10)
			.extracting("groupingKey")
			.containsExactly(
					"n: éolienne", 
					"a: électrique", 
					"n: convertisseur",
					"n: générateur",
					"n: pale", 
					"n: rotor", 
					"n: optimisation", 
					"npn: vitesse de rotation", 
					"n: réglage", 
					"npn: système de stockage"
				)
			;
	}


	@Test
	public void testTop10ByRank() {
		assertThat(termsByProperty(termino, TermProperty.RANK, false).subList(0, 10))
			.hasSize(10)
			.extracting("groupingKey")
			.containsExactly(
					"n: éolienne", 
					"a: électrique", 
					"n: convertisseur",
					"n: générateur",
					"n: pale", 
					"n: rotor", 
					"n: optimisation", 
					"npn: vitesse de rotation", 
					"n: réglage", 
					"npn: système de stockage"
				)
			;
	}

	@Test
	public void testMINACEVariations() {
		assertThat(termino)
			.asTermVariationsHavingRule("M-I-NA-CE")
			.extracting("from.groupingKey", "to.groupingKey")
			.contains(
				   tuple("na: fonctionnement hypersynchrone", "naca: fonctionnement hyper et hyposynchrone")
			)
			.hasSize(1)
			;

	}

	@Test
	public void testMicroSysteme() throws InstantiationException, IllegalAccessException {
		Term term1 = termino.getTerms().get("n: microsystème");
		assertNotNull(term1);
		Term term2 = termino.getTerms().get("nn: micro système");
		assertNotNull(term2);
		TermIndexValueProvider provider = TermIndexType.ALLCOMP_PAIRS.getProviderClass().newInstance();
		assertThat(provider.getClasses(term2))
			.containsOnly("micro+système");
		assertThat(provider.getClasses(term1))
			.containsOnly("micro+système");
		TermIndex termIndex = new TermIndex(provider);
		termIndex.addToIndex(term1);
		termIndex.addToIndex(term2);
		assertThat(termIndex.getClasses().keySet())
			.containsOnly("micro+système");
		assertThat(termIndex.getTerms("micro+système"))
			.contains(term1)
			.contains(term2)
			;
		
	}

	@Test
	public void testMSNNVariations() {
		assertThat(termino)
			.asTermVariationsHavingRule("M-S-NN")
			.extracting("from.groupingKey", "to.groupingKey")
			.contains(
				   tuple("n: microsystème", "nn: micro système"), 
//				   tuple("n: transistor-diode", "nn: transistor diode"), // terms have been merged
//				   tuple("n: france-allemagne", "nn: france allemagne"), // terms have been merged
				   tuple("n: schéma-bloc", "nn: schéma bloc"),
				   tuple("n: micro-turbines", "nn: micro turbine")
			)
			.hasSize(13)
			;
		
	}
	
	@Test
	public void testNbMorphologicalVariations() {
		assertThat(termino)
			.hasNVariationsOfType(42, VariationType.MORPHOLOGICAL);
	}



	
	@Test
	public void testSyntacticalVariations() {
		assertThat(termino)
			.containsVariationWithRuleName("npn: phase du stator", VariationType.DERIVATION, "na: phase statorique", "S-R2D-NPN")
			.containsVariationWithRuleName("na: machine asynchrone", VariationType.SYNTAGMATIC, "naa: machine asynchrone auto-excitée", "S-Ed-NA-A")
			.containsVariationWithRuleName("na: machine asynchrone", VariationType.SYNTAGMATIC, "napn: machine asynchrone à cage", "S-Ed-NA-PN")
			.containsVariationWithRuleName("na: machine asynchrone", VariationType.SYNTAGMATIC, "napna: machine asynchrone à cage autonome", "S-Ed-NA-PNA")
			.containsVariationWithRuleName("na: machine asynchrone", VariationType.SYNTAGMATIC, "napan: machine asynchrone à double alimentation", "S-Ed-NA-PAN")
			.containsVariationWithRuleName("na: machine asynchrone", VariationType.SYNTAGMATIC, "naca: machine synchrone ou asynchrone", "S-I-NA-AC")
			;
	}

	@Test
	public void testInferedVariations() {
		assertThat(termino)
			.containsVariation("npnpn: résistance du bobinage de stator", VariationType.INFERENCE, "npna: résistance du bobinage statorique")
			.containsVariation("naa: machine synchrone classique", VariationType.INFERENCE, "naa: machine asynchrone classique");
	}

	@Test
	public void testSyntacticalVariationsWithPrefixes() {
		assertThat(termino)
		.asTermVariationsHavingRule("NA-NprefA")
		.extracting("from.groupingKey", "to.groupingKey")
		.contains(
			tuple("na: générateur synchrone", "na: générateur asynchrone"),
			tuple("na: machine synchrone", "na: machine asynchrone"),
			tuple("na: contrôle direct", "na: contrôle indirect"),
			tuple("na: mode direct", "na: mode indirect"),
			tuple("na: aspect esthétique", "na: aspect inesthétique"),
			tuple("na: option nucléaire", "na: option antinucléaire"),
			tuple("na: génératrice synchrone", "na: génératrice asynchrone"),
			tuple("na: mesure précis", "na: mesure imprécis"),
			tuple("na: circulation stationnaire", "na: circulation instationnaire")
		)
		.hasSize(36)
		;
		
	}

	@Test
	public void testSyntacticalVariationsWithDerivatesSR2DNPN() {
		assertThat(termino)
			.asTermVariationsHavingRule("S-R2D-NPN")
			.hasSize(82)
			.extracting("from.groupingKey", "to.groupingKey")
			.contains(
					tuple("npn: production de électricité", "na: production électrique"),
					tuple("npn: étude de environnement", "na: étude environnemental"),
					tuple("npn: génération de électricité", "na: génération électrique")
			)
			;
	}
	@Test
	public void testSyntacticalVariationsWithDerivatesSPIDNAP() {
		assertThat(termino)
			.asTermVariationsHavingRule("S-PID-NA-P")
			.hasSize(0)
			.extracting("from.groupingKey", "to.groupingKey")
			.contains(
//					tuple("npn: givrage de pale", "na: pale givrer"),
//					tuple("npn: profondeur de eau", "na: eau profond")
			)
			;
	}
	

	@Test
	public void testPrefixes() {
		assertThat(termino)
			.containsRelation("a: multipolaire", RelationType.IS_PREFIX_OF, "a: polaire")
			.containsRelation("n: cofinancement", RelationType.IS_PREFIX_OF, "n: financement")
			.containsRelation("a: tripale", RelationType.IS_PREFIX_OF, "n: pale")
			.containsRelation("a: bipale", RelationType.IS_PREFIX_OF, "n: pale")
			.containsRelation("a: asynchrone", RelationType.IS_PREFIX_OF, "a: synchrone")
			.containsRelation("n: déréglementation", RelationType.IS_PREFIX_OF, "n: réglementation")
			;
	}

	@Test
	public void testStats() {
		TerminologyStats stats = TermSuite.getTerminologyService(termino).computeStats();
		assertThat(stats.getNbTerms()).isEqualTo(35431);
		assertThat(stats.getNbVariations()).isEqualTo(12220);
		assertThat(stats.getNbWords()).isEqualTo(9349);
		assertThat(stats.getNbSingleWords()).isEqualTo(9100);
		assertThat(stats.getNbSize2Words()).isEqualTo(19369);
		assertThat(stats.getNbSize3Words()).isEqualTo(6167);
		assertThat(stats.getNbSize4Words()).isEqualTo(789);
		assertThat(stats.getNbSize5Words()).isEqualTo(6);
		assertThat(stats.getNbSize6Words()).isEqualTo(0);
		assertThat(stats.getNbExtensions()).isEqualTo(0);
		assertThat(stats.getNbGraphical()).isEqualTo(2209);
		assertThat(stats.getNbDerivations()).isEqualTo(82);
		assertThat(stats.getNbMorphological()).isEqualTo(42);
		assertThat(stats.getNbSemantic()).isEqualTo(0);
		assertThat(stats.getNbInfered()).isEqualTo(18);
		assertThat(stats.getNbCompounds()).isEqualTo(535);
		assertThat(stats.getNbSemanticDistribOnly()).isEqualTo(0);
		assertThat(stats.getNbSemanticWithDico()).isEqualTo(0);
	}

	@Test
	public void testDerivations() {
		assertThat(termino)
			.containsRelation("n: hydroélectricité", RelationType.DERIVES_INTO, "a: hydroélectrique")
			.containsRelation("n: stator", RelationType.DERIVES_INTO, "a: statorique")
			.containsRelation("n: usage", RelationType.DERIVES_INTO, "n: usager")
			.containsRelation("n: support", RelationType.DERIVES_INTO, "n: supportage")
			.containsRelation("n: commerce", RelationType.DERIVES_INTO, "a: commercial")
			;
	}

}
