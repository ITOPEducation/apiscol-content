package fr.ac_versailles.crdp.apiscol.content.searchEngine;

import fr.ac_versailles.crdp.apiscol.auth.oauth.OauthServersProxy;

public interface ISearchEngineFactory {

	ISearchEngineQueryHandler getQueryHandler(String solrAddress,
			String solrSearchPath, String solrUpdatePath,
			String solrExtractPath, String solrSuggestPath,
			OauthServersProxy oauthServersProxy);

	ISearchEngineResultHandler getResultHandler();

}
