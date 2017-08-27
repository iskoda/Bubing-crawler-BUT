package it.unimi.di.law.warc.filters;

import it.unimi.di.law.bubing.parser.HTMLParser;
import it.unimi.di.law.bubing.parser.LanguageTextProcessor;
import it.unimi.di.law.bubing.parser.Parser;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import wrapper.NNetLanguageIdentifierWrapper;

/** A filter accepting only URIResponse whose content is in one of a given set of languages. */
public class LanguageEqualsOneOf extends AbstractFilter<URIResponse> {

    private static final HTMLParser<NNetLanguageIdentifierWrapper.Result> HTML_PARSER = new HTMLParser<>( null, new LanguageTextProcessor(), false );
    private static final Splitter SPLITTER = Splitter.on( ',' ).trimResults().omitEmptyStrings();
    
    private final Set<String> languages;

    public LanguageEqualsOneOf( final String[] languages ) {
        this.languages = new HashSet<>();
        this.languages.addAll( Arrays.asList( languages ) );
    }

    @Override
    public boolean apply( final URIResponse response ) {
        try {
            if( response.response().getStatusLine().getStatusCode() != 200 ) {
                return false;
            }
            HTML_PARSER.parse( response.uri(), response.response(), Parser.NULL_LINK_RECEIVER);
            return this.languages.contains( HTML_PARSER.result().language );
        } catch( IOException ex ) {
            return false;
        }
    }

    public static LanguageEqualsOneOf valueOf( final String spec ) {
        return new LanguageEqualsOneOf(  Iterables.toArray( SPLITTER.split( spec ), String.class ) );
    }

    @Override
    public String toString() {
        return toString( this.languages.toArray() );
    }

    @Override
    public Filter<URIResponse> copy() {
        return this;
    }
}
