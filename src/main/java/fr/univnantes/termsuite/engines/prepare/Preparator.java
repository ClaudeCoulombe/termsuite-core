package fr.univnantes.termsuite.engines.prepare;

import fr.univnantes.termsuite.framework.AggregateEngine;

public class Preparator extends AggregateEngine {

	@Override
	public void configure() {
		pipe(StopWordCleaner.class);
		pipe(SWTFlagSetter.class);
		pipe(SWTSizeSetter.class);
		pipe(CorpusWidePropertiesSetter.class);
		pipe(TermSpecificityComputer.class);
		pipe(ExtensionDetecter.class);
		pipe("Set IS_EXTENSION after Preparator", IsExtensionPropertySetter.class);
	}
}