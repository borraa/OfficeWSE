package analyzer;

import java.nio.file.Paths;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LetterTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class CustomStandardAnalyzer extends Analyzer {

	
	
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		
		final Tokenizer source = new StandardTokenizer();
		TokenStream filter = new LowerCaseFilter(source);                
		return new TokenStreamComponents(source, filter);
	}

}
