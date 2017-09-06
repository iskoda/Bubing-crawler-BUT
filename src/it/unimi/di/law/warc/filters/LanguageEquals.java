package it.unimi.di.law.warc.filters;

/** A filter accepting only URIResponse whose content is in a certain language. */
public class LanguageEquals extends AbstractFilter<URIResponse> {

    private final String language;

    public LanguageEquals( final String language ) {
        this.language = language;
    }

    @Override
    public boolean apply( final URIResponse response ) {
        return response.language().is_reliable && this.language.equals( response.language().language );
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

