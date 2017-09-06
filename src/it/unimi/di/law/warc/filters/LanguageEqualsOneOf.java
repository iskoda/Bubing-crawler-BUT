package it.unimi.di.law.warc.filters;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import cz.vutbr.fit.knot.NNetLanguageIdentifierWrapper;

/** A filter accepting only URIResponse whose content is in one of a given set of languages. */
public class LanguageEqualsOneOf extends AbstractFilter<URIResponse> {

    private static final Splitter SPLITTER = Splitter.on( ',' ).trimResults().omitEmptyStrings();
    
    private final String[] languages;

    public LanguageEqualsOneOf( final String[] languages ) {
        this.languages = languages;
    }

    @Override
    public boolean apply( final URIResponse response ) {
        NNetLanguageIdentifierWrapper.Result lan = response.language();
        if( lan.is_reliable == false ) return false;
        for ( String language: languages ) if ( lan.language.equals( language ) ) return true;
        return false;
    }

    public static LanguageEqualsOneOf valueOf( final String spec ) {
        return new LanguageEqualsOneOf(  Iterables.toArray( SPLITTER.split( spec ), String.class ) );
    }

    @Override
    public String toString() {
        return toString((Object[]) this.languages);
    }

    @Override
    public Filter<URIResponse> copy() {
        return this;
    }
}
