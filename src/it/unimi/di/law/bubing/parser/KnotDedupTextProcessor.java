package it.unimi.di.law.bubing.parser;

/*		 
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

import it.unimi.di.law.bubing.parser.Parser.TextProcessor;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.ArrayList;

/** */
public final class KnotDedupTextProcessor implements TextProcessor<List<String>> {
	public final static class Paragraphs extends ArrayList<String> {
	};
                
	KnotDedupTextProcessor.Paragraphs paragraphs;
    
	public KnotDedupTextProcessor() { }	
	
	@Override
	public Appendable append( CharSequence csq ) throws IOException {
		paragraphs.add( csq.toString() );
		return this;
	}

	@Override
	public Appendable append( CharSequence csq, int start, int end ) throws IOException {
		paragraphs.add( csq.subSequence( start, end ).toString() );
		return this;
	}

	@Override
	public Appendable append( char c ) throws IOException {
		return this;
	}

	@Override
	public void init( URI responseUrl ) {
		paragraphs = new Paragraphs();
	}

	@Override
	public List<String> result() {
		return paragraphs;
	}

	@Override
	public TextProcessor<List<String>> copy() {
		return new KnotDedupTextProcessor( );
	}
}
