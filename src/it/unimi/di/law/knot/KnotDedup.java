package it.unimi.di.law.knot;

import java.io.*;
import java.net.*;
import java.util.*;
import java.math.BigInteger;

import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

/** Deduplication worker.
 * @author karel
 */
public class KnotDedup {

	static final int LENGTH_LOW  = 70;
	static final int LENGTH_HIGH = 200;

	Set<String> servers;
	int port;

	Map<Integer,String> distibution;
	BigInteger blockCount;
	KnotDedupClient[] blockToServer;

	List<String> paragraphs;

	public KnotDedup(String hashMap) throws FileNotFoundException  {
		this( hashMap, 1234 );
	}

	public KnotDedup(String hashMap, int port) throws FileNotFoundException {
		this( loadHashMap( hashMap ), port );
	}

	protected KnotDedup(Map<Integer,String> distibution, int port) {
		this.blockCount = BigInteger.ZERO;
		this.distibution = distibution;
		this.servers = new HashSet<>();
		this.port = port;

		for(Map.Entry<Integer, String> entry : this.distibution.entrySet()) {
			String value = entry.getValue();
			this.servers.add( value );
			this.blockCount = this.blockCount.add( BigInteger.ONE );
		}
	}

	/** Copy of worker.
	 */
	public KnotDedup copy() {
		return new KnotDedup( this.distibution, this.port );
	}

	/** Add hash to one of the servers
	 * 
	 * @param hash Hash to add to server,
	 * @return Returns False if hash is duplicite.
	 * @throws IOException 
	 */
	public boolean addHash(byte[] hash) throws IOException {
		Integer blockId = new BigInteger( 1, hash ).mod( blockCount ).intValue();

		return blockToServer[blockId].addHash( hash );
	}

	/** Set a list of strings to detect duplicates.
	 * 
	 * @param paragraphs List of strings.
	 */
	public void setParagraphs( List<String> paragraphs ) {
		this.paragraphs = paragraphs;
	}
        
	public List<String> getParagraphs() {
		return paragraphs;
	}
        
	/** Determining the rate of duplication.
	 * 
	 * @return Returns interval <0, 1>. If isn't duplicite return 0.
	 * @throws IOException 
	 */
	public float duplicityRate() throws IOException {
		float sum = 0.f;
		float total = 0.f;
		for( String paragraph : paragraphs) {
			if( paragraph.length() > LENGTH_LOW ) {
				final Hasher hasher = Hashing.sipHash24().newHasher();
				hasher.putBytes( paragraph.getBytes() );
				final byte[] hash = hasher.hash().asBytes();

				float w = paragraph.length() > LENGTH_HIGH ? 1.f : 0.5f;
				sum += w * ( addHash( hash ) ? 0.f : 1.f );
				total += w;
			}
		}
		return sum / total;
	}

	/** Connection to servers.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException 
	 */
	public void connect() throws UnknownHostException, IOException {
		Map<String, KnotDedupClient> tmpClient = new HashMap<>();

		for (String hostname : servers) {
			tmpClient.put( hostname, new KnotDedupClient( hostname, port ) );
		}

		blockToServer = new KnotDedupClient[blockCount.intValue()];

		for(Map.Entry<Integer, String> entry : distibution.entrySet()) {
			blockToServer[entry.getKey()] = tmpClient.get( entry.getValue() );
		}
	}

	/** Close connection.
	 * 
	 * @throws IOException 
	 */
	public void close() throws IOException {
		for (KnotDedupClient server : blockToServer) {
			server.close();
		}
	}

	/** Load hash map from file.
         * 
         * @param hashMap File name of hash map
         * @return Mapping hash block to server
         * @throws FileNotFoundException 
         */
	private static Map<Integer,String> loadHashMap(String hashMap) throws FileNotFoundException {
		Map<Integer,String> output = new HashMap<>();
		File file = new File(hashMap);
		try (Scanner input = new Scanner(file)) {
			for( int i = 0; i < 9; i++ ) {
				input.next();
			}

			while(input.hasNext()) {
				int key = input.nextInt();
				input.nextInt();
				String value = input.next();
				output.put( key, value);
			}
		}
		return output;
	}

	/** Client to connect to one server.
	 */
	static class KnotDedupClient {

		Socket clientSocket;
		OutputStream outToServer;
		InputStream inFromServer;
		String hostname;
		int port;

		public KnotDedupClient(String server, int port) throws UnknownHostException, IOException {
			hostname = server;
			this.port = port;
			clientSocket = new Socket(server, port);
			outToServer = clientSocket.getOutputStream();
			inFromServer = clientSocket.getInputStream();
		}

		/** Add hash to database on server.
		 * 
		 * @param hash
		 * @return If hash is duplicate return false
		 * @throws IOException 
		 */
		public boolean addHash(byte[] hash) throws IOException{
			int count;
			try {
				outToServer.write(hash);
			} catch (IOException e) {
				reconnect();
				return addHash( hash );
			}

			clientSocket.setSoTimeout( 10 * 1000 );
			byte[] bytes = new byte[17];
			try {
				count = inFromServer.read(bytes);
			} catch ( SocketTimeoutException ignored ) {
				reconnect();
				return addHash( hash );
			}

			if( count == -1 ) {
				reconnect();
				return addHash(hash);
			}

			if( bytes[0] == '1') {
				return false;
			} else if ( bytes[0] == '0' ) {
				return true;
			}
			return true;
		}
                
		/** Re-connect to server.
		 * 
		 * @throws IOException 
		 */
		protected void reconnect() throws UnknownHostException, IOException {
			close();
			connect();
		}
                
		/** Create conection to server
                 * .
		 * @throws IOException 
		 */
		public void connect() throws UnknownHostException, IOException {
			clientSocket = new Socket(hostname, port);
			outToServer = clientSocket.getOutputStream();
			inFromServer = clientSocket.getInputStream();

			clientSocket.setSoTimeout( 10 * 1000 );
		}
                
		/** Close conection.
                 * 
		 * @throws IOException 
		 */
		public void close() throws IOException {
			outToServer.close();
			inFromServer.close();
			clientSocket.close();
		}
	}
}
