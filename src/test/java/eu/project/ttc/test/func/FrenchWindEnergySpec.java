
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

import static eu.project.ttc.test.TermSuiteAssertions.assertThat;
import static eu.project.ttc.test.func.FunctionalTests.termsByProperty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.util.Lists;
import org.junit.Test;

import eu.project.ttc.engines.desc.Lang;
import eu.project.ttc.models.CompoundType;
import eu.project.ttc.models.RelationType;
import eu.project.ttc.models.Term;
import eu.project.ttc.models.TermProperty;
import eu.project.ttc.models.Word;

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
				"S-Eg-NPN-NP",
				"S-R2-NPN",
				"S-R2I-NPN-P",
				"S-R2D-NPN",
				"S-P-NAPN-A",
				"S-P-NAA-A",
				"M-S-NN",
				"M-PI-EN-P",
				"M-R1-NA",
				"M-I-NA-CE",
				"M-I-NA-EC");
	}
	
	@Override
	protected List<String> getSyntacticNotMatchingRules() {
		return Lists.newArrayList(
				"S-IEg-NPN-PN,-CPN",
				"S-IEg-NA-A,-CA",
				"S-I-NA-RV",
				"S-R2I2-NPN-PNP",
				"S-I2-NPN-PN,PNC",
				"S-PID-NA-P",
				"S-PID-NAA-P",
				"M-I2-NA");
	}
	
	@Test
	public void weNeoclassicalCompounds() {
		List<Word> neoclassicals = termIndex.getWords().stream()
			.filter(Word::isCompound)
			.filter(w -> w.getCompoundType() == CompoundType.NEOCLASSICAL).collect(Collectors.toList());
		
		assertThat(neoclassicals)
			.isNotEmpty()
			.extracting("lemma", "neoclassicalAffix.lemma")
			.contains(tuple("hydroélectrique", "eau"))
			.contains(tuple("antinucléaire", "anti"))
			.contains(tuple("aéroélastique", "air"))
			.hasSize(363);
	}

	
	@Test
	public void testTop10ByFreq() {
		assertThat(termsByProperty(termIndex, TermProperty.FREQUENCY, true).subList(0, 10))
			.hasSize(10)
			.extracting("groupingKey")
			.containsExactly(
					"n: puissance", "a: éolien", "n: système", "n: énergie", "n: vitesse", 
					"n: vent", "n: réseau", "n: éolienne", "n: machine", "n: figure");
	}
	
	@Test
	public void testTop10ByWR() {
		assertThat(termsByProperty(termIndex, TermProperty.SPECIFICITY, true).subList(0, 10))
			.hasSize(10)
			.extracting("groupingKey")
			.containsExactly(
					"n: éolienne", "a: électrique", "n: convertisseur", 
					"n: générateur", "n: pale", "n: rotor", "n: optimisation", 
					"npn: vitesse de rotation", "n: réglage", 
					"npn: système de stockage"
					)
			;
	}
	
	@Test
	public void testCheckTerms() {
		assertThat(termIndex.getTerms())
			.extracting("groupingKey")
			.contains(
					"n: tourbillon"
					)
			.doesNotContain("npn: givrage de pale")
			;
	}

	@Test
	public void testTermHydroelectrique() {
		Term term = termIndex.getTermByGroupingKey("a: hydroélectrique");
		
		assertThat(term)
			.isCompound()
			.hasCompoundType(CompoundType.NEOCLASSICAL)
			.hasCompositionSubstrings("hydro", "électrique")
			.hasCompositionLemmas("eau", "électrique")
			;
	}


	@Test
	public void testTermElectromagnetique() {
		Term term = termIndex.getTermByGroupingKey("a: électromagnétique");
		
		assertThat(term)
			.isCompound()
			.hasCompoundType(CompoundType.NEOCLASSICAL)
			.hasCompositionSubstrings("électro", "magnétique");
	}

	@Test
	public void testTermVitesseDeRotation() {
		Term term = termIndex.getTermByGroupingKey("npn: vitesse de rotation");
		assertThat(term)
			.hasFrequency(308);
		
		assertThat(termIndex)
			.hasNBases(term, 2)
			.hasNVariationsOfType(term, 24, RelationType.SYNTACTICAL)
			.getVariations(term)
			.extracting("to.groupingKey", "info", "to.frequency")
			.contains(
					tuple("napn: vitesse angulaire de rotation", "S-I1-NPN-A", 2),
					tuple("napn: vitesse nominal de rotation", "S-I1-NPN-A", 2),
					tuple("npna: vitesse de rotation correspondant", "S-Ed-NPN-A", 3),
					tuple("npnpna: vitesse de rotation du champ magnétique", "S-Ed-NPN-PNA", 2)
				);
	}

	@Test
	public void testTermEolienne() {
		assertThat(termIndex.getTermByGroupingKey("n: éolienne"))
				.hasFrequency(1102)
				.hasGroupingKey("n: éolienne");
	}

	@Test
	public void testTop10ByRank() {
		assertThat(termsByProperty(termIndex, TermProperty.RANK, false).subList(0, 10))
			.hasSize(10)
			.extracting("groupingKey")
			.containsExactly(
					"n: éolienne", "a: électrique", "n: convertisseur", 
					"n: générateur", "n: pale", "n: rotor", "n: optimisation", 
					"npn: vitesse de rotation", "n: réglage", 
					"npn: système de stockage"
				)
			;
	}

	@Test
	public void testMINACEVariations() {
		assertThat(termIndex)
			.asTermVariationsHavingObject("M-I-NA-CE")
			.extracting("from.groupingKey", "to.groupingKey")
			.contains(
				   tuple("na: fonctionnement hypersynchrone", "naca: fonctionnement hyper et hyposynchrone")
			)
			.hasSize(1)
			;

	}

	@Test
	public void testMSNNVariations() {
		assertThat(termIndex)
			.hasNVariationsOfType(43, RelationType.MORPHOLOGICAL)
			.asTermVariationsHavingObject("M-S-NN")
			.hasSize(9)
			.extracting("from.groupingKey", "to.groupingKey")
			.contains(
				   tuple("n: microsystème", "nn: micro système"), 
				   tuple("n: transistor-diode", "nn: transistor diode"), 
				   tuple("n: france-allemagne", "nn: france allemagne"), 
				   tuple("n: schéma-bloc", "nn: schéma bloc")
			)
			;
	}

		   
	
	@Test
	public void testSyntacticalVariations() {
		assertThat(termIndex)
			.containsVariation("npn: phase du stator", RelationType.SYNTACTICAL, "na: phase statorique", "S-R2D-NPN")
			.containsVariation("na: machine asynchrone", RelationType.SYNTACTICAL, "naa: machine asynchrone auto-excitée", "S-Ed-NA-A")
			.containsVariation("na: machine asynchrone", RelationType.SYNTACTICAL, "napn: machine asynchrone à cage", "S-Ed-NA-PN")
			.containsVariation("na: machine asynchrone", RelationType.SYNTACTICAL, "napna: machine asynchrone à cage autonome", "S-Ed-NA-PNA")
			.containsVariation("na: machine asynchrone", RelationType.SYNTACTICAL, "napan: machine asynchrone à double alimentation", "S-Ed-NA-PAN")
			.containsVariation("na: machine asynchrone", RelationType.SYNTACTICAL, "naca: machine synchrone ou asynchrone", "S-I-NA-AC")
			;
	}

	@Test
	public void testSyntacticalVariationsWithPrefixes() {
		assertThat(termIndex)
		.asTermVariationsHavingObject("NA-NprefA")
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
		.hasSize(27)
		;
		
	}

	@Test
	public void testSyntacticalVariationsWithDerivatesSR2DNPN() {
		assertThat(termIndex)
			.asTermVariationsHavingObject("S-R2D-NPN")
			.hasSize(77)
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
		assertThat(termIndex)
			.asTermVariationsHavingObject("S-PID-NA-P")
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
		assertThat(termIndex)
			.containsVariation("a: multipolaire", RelationType.IS_PREFIX_OF, "a: polaire")
			.containsVariation("n: cofinancement", RelationType.IS_PREFIX_OF, "n: financement")
			.containsVariation("a: tripale", RelationType.IS_PREFIX_OF, "n: pale")
			.containsVariation("a: bipale", RelationType.IS_PREFIX_OF, "n: pale")
			.containsVariation("a: asynchrone", RelationType.IS_PREFIX_OF, "a: synchrone")
			.containsVariation("n: déréglementation", RelationType.IS_PREFIX_OF, "n: réglementation")
			;
	}
	
	@Test
	public void testDerivations() {
		assertThat(termIndex)
			.containsVariation("n: hydroélectricité", RelationType.DERIVES_INTO, "a: hydroélectrique")
			.containsVariation("n: stator", RelationType.DERIVES_INTO, "a: statorique")
			.containsVariation("n: usage", RelationType.DERIVES_INTO, "n: usager")
			.containsVariation("n: support", RelationType.DERIVES_INTO, "n: supportage")
			.containsVariation("n: commerce", RelationType.DERIVES_INTO, "a: commercial")
			;
	}

}
