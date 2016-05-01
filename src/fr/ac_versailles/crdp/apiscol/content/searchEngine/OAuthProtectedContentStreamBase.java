package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.apache.solr.common.util.ContentStreamBase;

public class OAuthProtectedContentStreamBase extends ContentStreamBase {

	@Override
	public InputStream getStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Construct a <code>ContentStream</code> from a <code>URL</code>
	 * 
	 * This uses a <code>URLConnection</code> to get the content stream
	 * 
	 * @see URLConnection
	 */
	public static class OAuthURLStream extends ContentStreamBase {
		private final URL url;
		private String accessToken;

		public OAuthURLStream(URL url, String accessToken) {
			this.url = url;
			this.accessToken = accessToken;
			sourceInfo = "url";
		}

		@Override
		public InputStream getStream() throws IOException {

			HttpURLConnection conn = (HttpURLConnection) this.url
					.openConnection();
			conn.setRequestProperty("Authorization", "Bearer " + accessToken);
			System.out.println("Bearer " + accessToken);
			conn.setUseCaches(false);
			contentType = conn.getContentType();
			name = url.toExternalForm();
			size = new Long(conn.getContentLength());
			return conn.getInputStream();
		}
	}

}
