package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import fr.ac_versailles.crdp.apiscol.auth.oauth.OauthServersProxy;

public class JerseySearchEngineFactory implements ISearchEngineFactory {

	@Override
	public ISearchEngineQueryHandler getQueryHandler(String solrAddress,
			String solrSearchPath, String solrUpdatePath,
			String solrExtractPath, String solrSuggestPath,
			OauthServersProxy oauthServersProxy) {
		return new JerseySearchEngineQueryHandler(solrAddress, solrSearchPath,
				solrUpdatePath, solrExtractPath, oauthServersProxy);
	}

	@Override
	public ISearchEngineResultHandler getResultHandler() {
		return new JerseySearchEngineResultHandler();
	}

}
