package it.unimi.di.law.bubing.parser;

import it.unimi.di.law.bubing.parser.Parser.TextProcessor;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.input.CharSequenceReader;

import cz.vutbr.fit.knot.NNetLanguageIdentifierWrapper;

/** An implementation of a {@link Parser.TextProcessor} that identifier language of text. */
public final class LanguageTextProcessor implements TextProcessor<NNetLanguageIdentifierWrapper.Result> {
	
	private String text;
        private final NNetLanguageIdentifierWrapper identifier;
	
	public LanguageTextProcessor() {
		this.identifier = new NNetLanguageIdentifierWrapper();
	}
	
	public LanguageTextProcessor( int min_num, int max_num ) {
		this.identifier = new NNetLanguageIdentifierWrapper( min_num, max_num );
	}
	
	@Override
	public Appendable append( CharSequence csq ) throws IOException {
                text += " ";
		text += new CharSequenceReader( csq );
		return this;
	}

	@Override
	public Appendable append( CharSequence csq, int start, int end ) throws IOException {
                text += " ";
		text += new CharSequenceReader( csq.subSequence( start, end ) );
		return this;
	}

	@Override
	public Appendable append( char c ) throws IOException {
                text += c;
		return this;
	}

	@Override
	public void init( URI responseUrl ) {
		text = "";
	}

	@Override
	public NNetLanguageIdentifierWrapper.Result result() {
		return identifier.findLanguage( String.join( " ", text.split("\\s+") ).trim() );
	}

	@Override
	public TextProcessor<NNetLanguageIdentifierWrapper.Result> copy() {
		return this;
	}
}
