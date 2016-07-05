package eu.project.ttc.test.func;

import static eu.project.ttc.test.TermSuiteAssertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.assertj.core.api.iterable.Extractor;
import org.assertj.core.groups.Tuple;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import eu.project.ttc.engines.cleaner.TermProperty;
import eu.project.ttc.engines.desc.Lang;
import eu.project.ttc.engines.desc.TermSuiteCollection;
import eu.project.ttc.models.Term;
import eu.project.ttc.models.TermIndex;
import eu.project.ttc.models.VariationType;
import eu.project.ttc.resources.MemoryTermIndexManager;
import eu.project.ttc.tools.TermSuitePipeline;
import eu.project.ttc.tools.utils.ControlFilesGenerator;

public abstract class WindEnergySpec {

	protected TermIndex termIndex = null;
	protected Lang lang;
	protected List<String> syntacticMatchingRules = Lists.newArrayList();
	protected List<String> syntacticNotMatchingRules = Lists.newArrayList();

	
	public WindEnergySpec() {
		super();
		this.lang = getLang();
		this.syntacticMatchingRules = getSyntacticMatchingRules();
		this.syntacticNotMatchingRules = getSyntacticNotMatchingRules();
	}
	
	protected abstract Lang getLang();
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
	
	private static final LoadingCache<Lang, TermIndex> TERM_INDEX_CACHE = CacheBuilder.newBuilder()
				.maximumSize(1)
				.build(new CacheLoader<Lang, TermIndex>() {
					@Override
					public TermIndex load(Lang lang) throws Exception {
						return runPipeline(lang);
					}
				});
	
	@Before
	public void setup() {
		this.termIndex = TERM_INDEX_CACHE.getUnchecked(lang);
	}

	protected static TermIndex runPipeline(Lang lang) {
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
			
		return pipeline.getTermIndex();
	}

	@Test
	public void weControlSyntacticMatchingRules() {
		assertThat(termIndex)
			.asMatchingRules()
			.containsOnlyElementsOf(syntacticMatchingRules)
			.doesNotContainAnyElementsOf(syntacticNotMatchingRules);
		
	}

//	@Test
//	public void weControlSyntacticVariations() {
//		for(String ruleName:syntacticMatchingRules) {
//			assertThat(termIndex)
//			.asTermVariationsHavingObject(ruleName)
//			.extracting("base.groupingKey", "variant.groupingKey", "variationType")
//			.containsAll(
//					ControlFiles.syntacticVariationTuples(lang, "we", ruleName)
//			);
//		}
//	}

	@Test
	public void weControlPrefixes() {
		assertThat(termIndex)
			.asTermVariations(VariationType.IS_PREFIX_OF)
			.extracting("base.groupingKey", "variant.groupingKey")
			.containsOnly(
					ControlFiles.prefixVariationTuples(lang, "we")
			);
	}

	@Test
	public void weControlDerivates() {
		assertThat(termIndex)
		.asTermVariations(VariationType.DERIVES_INTO)
		.extracting("info", "base.groupingKey", "variant.groupingKey")
		.containsOnly(
				ControlFiles.derivateVariationTuples(lang, "we")
		);

	}

	@Test
	public void weCompounds() {
		assertThat(termIndex)
			.asCompoundList()
			.extracting(new Extractor<Term, Tuple>() {
				@Override
				public Tuple extract(Term compoundTerm) {
					return tuple(
							compoundTerm.getWords().get(0).getWord().getCompoundType().getShortName(),
							compoundTerm.getGroupingKey(),
							ControlFilesGenerator.toCompoundString(compoundTerm)
							);
				}
			})
			.containsOnly(
					ControlFiles.compoundTuples(lang, "we")
			);

	}
}