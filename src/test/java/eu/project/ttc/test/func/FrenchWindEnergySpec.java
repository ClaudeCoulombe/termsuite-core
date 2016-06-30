package eu.project.ttc.test.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static eu.project.ttc.test.func.FunctionalTests.termsByProperty;
import static eu.project.ttc.test.func.FunctionalTests.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

import eu.project.ttc.engines.cleaner.TermProperty;
import eu.project.ttc.engines.desc.Lang;
import eu.project.ttc.engines.desc.TermSuiteCollection;
import eu.project.ttc.models.TermIndex;
import eu.project.ttc.models.VariationType;
import eu.project.ttc.resources.MemoryTermIndexManager;
import eu.project.ttc.tools.TermSuitePipeline;

public class FrenchWindEnergySpec {
	
	
	static TermIndex termIndex;
	static Lang lang = Lang.FR;
	
	@BeforeClass
	public static void setup() {
		MemoryTermIndexManager.getInstance().clear();
		TermSuitePipeline pipeline = TermSuitePipeline.create(lang.getCode(), "file:")
			.setResourcePath(FunctionalTests.getResourcePath())
			.setCollection(TermSuiteCollection.TXT, FunctionalTests.getCorpusWEPath(lang), "UTF-8")
			.aeWordTokenizer()
			.setTreeTaggerHome(FunctionalTests.getTaggerPath())
			.aeTreeTagger()
			.aeUrlFilter()
			.aeStemmer()
			.aeRegexSpotter()
			.aeStopWordsFilter()
			.aeSpecificityComputer()
			.aeCompostSplitter()
			.aePrefixSplitter()
			.aeSuffixDerivationDetector()
			.aeSyntacticVariantGatherer()
			.aeGraphicalVariantGatherer()
			.aeExtensionDetector()
			.aeRanker(TermProperty.WR, true)
			.run();
			
		termIndex = pipeline.getTermIndex();
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
		assertThat(termsByProperty(termIndex, TermProperty.WR, true).subList(0, 10))
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
	public void testTermVitesseDeRotation() {
		assertThat(termIndex.getTermByGroupingKey("npn: vitesse de rotation"))
			.hasFrequency(308)
			.hasNBases(0)
			.hasNVariationsOfType(24, VariationType.SYNTACTICAL)
			.getVariations()
			.extracting("variant.groupingKey", "info", "variant.frequency")
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
	public void testMSNNVariations() {
		assertThat(termIndex)
			.hasNVariationsOfType(28, VariationType.MORPHOLOGICAL)
			.getVariationsHavingObject("M-S-NN")
			.hasSize(9)
			.extracting("base.groupingKey", "variant.groupingKey")
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
			.containsVariation("na: machine asynchrone", VariationType.SYNTACTICAL, "naa: machine asynchrone auto-excitée", "S-Ed-NA-A")
			.containsVariation("na: machine asynchrone", VariationType.SYNTACTICAL, "napn: machine asynchrone à cage", "S-Ed-NA-PN")
			.containsVariation("na: machine asynchrone", VariationType.SYNTACTICAL, "napna: machine asynchrone à cage autonome", "S-Ed-NA-PNA")
			.containsVariation("na: machine asynchrone", VariationType.SYNTACTICAL, "napan: machine asynchrone à double alimentation", "S-Ed-NA-PAN")
			.containsVariation("na: machine asynchrone", VariationType.SYNTACTICAL, "naca: machine synchrone ou asynchrone", "S-I-NA-AC")
			;
	}

	@Test
	public void testSyntacticalVariationsWithPrefixes() {
		assertThat(termIndex)
		.getVariationsHavingObject("NA-NprefA")
		.extracting("base.groupingKey", "variant.groupingKey")
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
		.hasSize(26)
		;
		
	}

	@Test
	public void testSyntacticalVariationsWithDerivatesSPIDNAP() {
		assertThat(termIndex)
			.getVariationsHavingObject("S-PID-NA-P")
			.hasSize(0)
			;
	}

	@Test
	public void testSyntacticalVariationsWithDerivatesSR2DNPN() {
		assertThat(termIndex)
			.getVariationsHavingObject("S-R2D-NPN")
			.hasSize(77)
			.extracting("base.groupingKey", "variant.groupingKey")
			.contains(
					tuple("npn: production de électricité", "na: production électrique"),
					tuple("npn: étude de environnement", "na: étude environnemental"),
					tuple("npn: génération de électricité", "na: génération électrique")
			)
			;
	}

	@Test
	public void testPrefixes() {
		assertThat(termIndex)
			.containsVariation("a: multipolaire", VariationType.IS_PREFIX_OF, "a: polaire")
			.containsVariation("n: cofinancement", VariationType.IS_PREFIX_OF, "n: financement")
			.containsVariation("a: tripale", VariationType.IS_PREFIX_OF, "n: pale")
			.containsVariation("a: bipale", VariationType.IS_PREFIX_OF, "n: pale")
			.containsVariation("a: asynchrone", VariationType.IS_PREFIX_OF, "a: synchrone")
			.containsVariation("n: déréglementation", VariationType.IS_PREFIX_OF, "n: réglementation")
			;
	}
	
	@Test
	public void testDerivations() {
		assertThat(termIndex)
			.containsVariation("n: hydroélectricité", VariationType.DERIVES_INTO, "a: hydroélectrique")
			.containsVariation("n: stator", VariationType.DERIVES_INTO, "a: statorique")
			.containsVariation("n: usage", VariationType.DERIVES_INTO, "n: usager")
			.containsVariation("n: support", VariationType.DERIVES_INTO, "n: supportage")
			.containsVariation("n: commerce", VariationType.DERIVES_INTO, "a: commercial")
			;
	}

}
