package it.unimi.di.law.warc.filters;

import it.unimi.di.law.bubing.parser.HTMLParser;
import it.unimi.di.law.bubing.parser.LanguageTextProcessor;
import it.unimi.di.law.bubing.parser.Parser;

import java.io.IOException;

import wrapper.NNetLanguageIdentifierWrapper;

/** A filter accepting only URIResponse whose content is in a certain language. */
public class LanguageEquals extends AbstractFilter<URIResponse> {

    private static final HTMLParser<NNetLanguageIdentifierWrapper.Result> HTML_PARSER = new HTMLParser<>( null, new LanguageTextProcessor(), false );
    private final String language;

    public LanguageEquals( final String language ) {
        this.language = language;
    }

    @Override
    public boolean apply( final URIResponse response ) {
        try {
            if( response.response().getStatusLine().getStatusCode() != 200 ) {
                return false;
            }
            HTML_PARSER.parse( response.uri(), response.response(), Parser.NULL_LINK_RECEIVER);
            return this.language.equals( HTML_PARSER.result().language );
        } catch (IOException ex) {
            return false;
        }
    }

    public static LanguageEquals valueOf( final String spec ) {
        return new LanguageEquals( spec );
    }

    @Override
    public String toString() {
        return toString( language );
    }

    @Override
    public Filter<URIResponse> copy() {
        return this;
    }
}
