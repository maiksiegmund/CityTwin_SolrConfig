package de.citytwin;

import opennlp.tools.namefind.DefaultNameContextGenerator;
import opennlp.tools.namefind.NameContextGenerator;
import opennlp.tools.namefind.TokenNameFinderFactory;
import opennlp.tools.util.featuregen.AdaptiveFeatureGenerator;
import opennlp.tools.util.featuregen.BigramNameFeatureGenerator;
import opennlp.tools.util.featuregen.CachedFeatureGenerator;
import opennlp.tools.util.featuregen.OutcomePriorFeatureGenerator;
import opennlp.tools.util.featuregen.PreviousMapFeatureGenerator;
import opennlp.tools.util.featuregen.SentenceFeatureGenerator;
import opennlp.tools.util.featuregen.TokenClassFeatureGenerator;
import opennlp.tools.util.featuregen.TokenFeatureGenerator;
import opennlp.tools.util.featuregen.WindowFeatureGenerator;

public class TokenNameFinderFactoryCustom extends TokenNameFinderFactory {
	
	public TokenNameFinderFactoryCustom() {
		super();
	}

	@Override
	public NameContextGenerator createContextGenerator() {

	    AdaptiveFeatureGenerator featureGenerator = createFeatureGenerators();

	    if (featureGenerator == null) {
	      featureGenerator = new CachedFeatureGenerator(
	          new WindowFeatureGenerator(new TokenFeatureGenerator(), 2, 2),
//	          new WindowFeatureGenerator(new TokenClassFeatureGenerator(true), 2, 2),
//	          new OutcomePriorFeatureGenerator(),
//	          new PreviousMapFeatureGenerator(),
//	          new BigramNameFeatureGenerator(),
	          new SentenceFeatureGenerator(true, false));
	    }

	    return new DefaultNameContextGenerator(featureGenerator);
	  }
	
}
