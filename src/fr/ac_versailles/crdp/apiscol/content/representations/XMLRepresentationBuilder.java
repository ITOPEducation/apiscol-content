package fr.ac_versailles.crdp.apiscol.content.representations;

import java.awt.Point;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.Pair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import fr.ac_versailles.crdp.apiscol.ResourcesKeySyntax;
import fr.ac_versailles.crdp.apiscol.UsedNamespaces;
import fr.ac_versailles.crdp.apiscol.content.ContentType;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry;
import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.crawler.LinkRefreshingHandler;
import fr.ac_versailles.crdp.apiscol.content.crawler.LinkRefreshingHandler.State;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder.DBTypes;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.recovery.ContentRecoveryHandler;
import fr.ac_versailles.crdp.apiscol.content.recovery.ContentRecoveryHandler.MessageTypes;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.TimeUtils;
import fr.ac_versailles.crdp.apiscol.utils.XMLUtils;

public class XMLRepresentationBuilder extends
		AbstractRepresentationBuilder<Document> {

	public XMLRepresentationBuilder(Map<String, String> dbParams) {
		super(dbParams);
	}

	@Override
	public Document getResourceRepresentation(URI baseUri,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		Document XMLRepresentation = XMLUtils.createXMLDocument();

		addXMLSubTreeForResource(XMLRepresentation, XMLRepresentation, baseUri,
				apiscolInstanceName, resourceId, editUri);
		XMLUtils.addNameSpaces(XMLRepresentation, UsedNamespaces.ATOM);
		return XMLRepresentation;
	}

	private void addXMLSubTreeForResult(Document XMLDocument,
			Node insertionElement, String resourceId, String fileName,
			String score, String type, List<String> snippets,
			String apiscolInstanceName) {
		Element rootElement = XMLDocument.createElement("apiscol:hit");
		insertionElement.appendChild(rootElement);
		rootElement.setAttribute("resourceId",
				getResourceUrn(resourceId, apiscolInstanceName));
		Element fileElement = XMLDocument.createElement("apiscol:file");
		rootElement.appendChild(fileElement);
		fileElement.setTextContent(fileName);
		Element scoreElement = XMLDocument.createElement("apiscol:score");
		rootElement.appendChild(scoreElement);
		scoreElement.setTextContent(score);
		Element typeElement = XMLDocument.createElement("apiscol:type");
		rootElement.appendChild(typeElement);
		typeElement.setTextContent(type);
		Iterator<String> it = snippets.iterator();
		Element matchesElement = XMLDocument.createElement("apiscol:matches");
		rootElement.appendChild(matchesElement);
		while (it.hasNext()) {
			Element matchElement = XMLDocument.createElement("apiscol:match");
			matchesElement.appendChild(matchElement);
			matchElement.setTextContent(it.next());
		}

	}

	private void addXMLSubTreeForSpellcheck(Document XMLDocument,
			Element insertionElement,
			Map<String, List<String>> suggestionsforTerms,
			List<String> suggestionsforQuery) {
		Iterator<String> it = suggestionsforTerms.keySet().iterator();
		String term;
		while (it.hasNext()) {
			term = it.next();
			Element queryTermElement = XMLDocument
					.createElement("apiscol:query_term");
			insertionElement.appendChild(queryTermElement);
			queryTermElement.setAttribute("requested", term);
			Iterator<String> it2 = suggestionsforTerms.get(term).iterator();
			while (it2.hasNext()) {
				Element wordElement = XMLDocument.createElement("apiscol:word");
				queryTermElement.appendChild(wordElement);
				wordElement.setTextContent(it2.next());

			}
		}
		Element queriesElement = XMLDocument.createElement("apiscol:queries");
		insertionElement.appendChild(queriesElement);
		Iterator<String> it3 = suggestionsforQuery.iterator();
		while (it3.hasNext()) {
			Element queryElement = XMLDocument.createElement("apiscol:query");
			queriesElement.appendChild(queryElement);
			queryElement.setTextContent(it3.next());

		}

	}

	private long addXMLSubTreeForResource(Document XMLDocument,
			Node insertionElement, URI baseUri, String apiscolInstanceName,
			String resourceId, String editUri) throws DBAccessException,
			InexistentResourceInDatabaseException {
		ContentType type = getResourceType(resourceId);
		Element rootElement = XMLDocument.createElement("entry");
		Element updatedElement = XMLDocument.createElement("updated");
		long updateTime;
		String etagForResource = getEtagForResource(resourceId).replace(
				"data-version-", "");
		try {
			updateTime = Long.parseLong(etagForResource);
		} catch (NumberFormatException e1) {
			logger.warn(String.format(
					"Etag for ressourse %s is not a valid long : %s",
					resourceId, etagForResource));
			updateTime = 0L;
		}
		updatedElement.setTextContent(TimeUtils.toRFC3339(updateTime));
		rootElement.appendChild(updatedElement);
		Element idElement = XMLDocument.createElement("id");
		idElement
				.setTextContent(getResourceUrn(resourceId, apiscolInstanceName));
		rootElement.appendChild(idElement);
		Element authorElement = XMLDocument.createElement("author");
		Element titleElement = XMLDocument.createElement("title");
		Element nameElement = XMLDocument.createElement("name");
		rootElement.appendChild(authorElement);
		authorElement.appendChild(nameElement);
		rootElement.appendChild(titleElement);
		Element metadataElement = XMLDocument.createElement("link");
		metadataElement.setAttribute("href", getMetadataUri(resourceId));
		metadataElement.setAttribute("rel", "describedby");
		metadataElement.setAttribute("type", "application/atom+xml");
		rootElement.appendChild(metadataElement);
		if (StringUtils.isNotEmpty(editUri)) {
			Element editElement = XMLDocument.createElement("link");
			editElement.setAttribute("href",
					getResourceEditUri(editUri, resourceId));
			editElement.setAttribute("rel", "edit");
			editElement.setAttribute("type", "application/atom+xml");
			rootElement.appendChild(editElement);
			Element editMediaElement = XMLDocument.createElement("link");
			editMediaElement.setAttribute("href",
					getResourceEditMediaUri(editUri, resourceId, type));
			editMediaElement.setAttribute("rel", "edit-media");
			editMediaElement.setAttribute("type", "application/atom+xml");
			rootElement.appendChild(editMediaElement);
		}
		Element typeElement = XMLDocument.createElement("category");

		typeElement.setAttribute("term", type.toString());
		rootElement.appendChild(typeElement);
		Element selfHTMLLinkElement = XMLDocument.createElement("link");
		selfHTMLLinkElement.setAttribute("rel", "self");
		selfHTMLLinkElement.setAttribute("type", "text/html");
		selfHTMLLinkElement.setAttribute("href",
				getResourceHTMLUri(baseUri, resourceId));
		rootElement.appendChild(selfHTMLLinkElement);
		Element selfAtomXMLLinkElement = XMLDocument.createElement("link");
		selfAtomXMLLinkElement.setAttribute("rel", "self");
		selfAtomXMLLinkElement.setAttribute("type", "application/atom+xml");
		selfAtomXMLLinkElement.setAttribute("href",
				getResourceAtomXMLUri(baseUri, resourceId));
		rootElement.appendChild(selfAtomXMLLinkElement);
		Element selfThumbLinkElement = XMLDocument.createElement("link");
		selfThumbLinkElement.setAttribute("rel", "icon");
		selfThumbLinkElement.setAttribute("type", "application/atom+xml");
		selfThumbLinkElement.setAttribute("href",
				getResourceThumbsUri(baseUri, resourceId));
		rootElement.appendChild(selfThumbLinkElement);
		Element selfPreviewLinkElement = XMLDocument.createElement("link");
		selfPreviewLinkElement.setAttribute("rel", "preview");
		selfPreviewLinkElement.setAttribute("type", "text/html");
		selfPreviewLinkElement.setAttribute("href",
				getResourcePreviewUri(baseUri, resourceId));
		rootElement.appendChild(selfPreviewLinkElement);
		if (type.equals(ContentType.url)) {
			Element urlElement = XMLDocument.createElement("content");
			Element htmlLinkElement = XMLDocument.createElementNS(
					UsedNamespaces.XHTML.getUri(), "a");
			htmlLinkElement.setAttribute("href", getResourceUrl(resourceId));
			urlElement.appendChild(htmlLinkElement);
			rootElement.appendChild(urlElement);
			urlElement.setAttribute("type", "text/html");
		} else {

			Element xmlContentElement = XMLDocument.createElement("content");
			xmlContentElement.setAttribute("type", "application/xml");
			Element filesElement = XMLDocument.createElement("apiscol:files");
			xmlContentElement.appendChild(filesElement);
			rootElement.appendChild(xmlContentElement);
			String mainFile = getMainFileForResource(resourceId);
			if (StringUtils.isNotEmpty(mainFile))
				filesElement.setAttribute("main", mainFile);
			Iterator<String> it = null;

			try {
				it = getFileList(resourceId).iterator();
				while (it.hasNext()) {
					addXMLSubTreeForFile(XMLDocument, filesElement, baseUri,
							apiscolInstanceName, resourceId, it.next());
				}
			} catch (ResourceDirectoryNotFoundException e) {
				logger.warn(String.format(
						"Directory not found for resource %s with message %s",
						resourceId, e.getMessage()));
				System.out.println(String.format(
						"Directory not found for resource %s with message %s",
						resourceId, e.getMessage()));
				Element element = XMLDocument.createElement("status");
				element.setTextContent("Directory not yet created");
				rootElement.appendChild(element);
				insertionElement.appendChild(rootElement);
				return 0L;
			}
			if (ResourceDirectoryInterface.existResourceArchive(resourceId)) {
				Element downloadLinkElement = XMLDocument
						.createElement("apiscol:archive");
				downloadLinkElement.setAttribute("type", "application/zip");
				downloadLinkElement.setAttribute("src",
						getResourceArchiveDownloadUri(baseUri, resourceId));
				xmlContentElement.appendChild(downloadLinkElement);
			}

		}
		insertionElement.appendChild(rootElement);
		return updateTime;
	}

	private void addXMLSubTreeForFile(Document document, Node insertionNode,
			URI baseUri, String apiscolInstanceName, String resourceId,
			String fileName) {
		Element rootElement = document.createElement("apiscol:file");
		insertionNode.appendChild(rootElement);
		Element nameElement = document.createElement("title");

		Element downloadLinkElement = document.createElement("link");
		downloadLinkElement.setAttribute("rel", "self");
		downloadLinkElement.setAttribute("href",
				getFileDownloadUrl(baseUri, resourceId, fileName));
		downloadLinkElement.setAttribute("type", "application/octet-stream");
		nameElement.setTextContent(fileName);
		rootElement.appendChild(nameElement);
		rootElement.appendChild(downloadLinkElement);
	}

	@Override
	public Document getFileSuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String fileName) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("done");
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href",
				getFileDownloadUrl(baseUri, resourceId, fileName));
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("File deleted");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		return report;
	}

	@Override
	public Document getResourceSuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String warnings) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		Element idElement = report.createElement("id");
		idElement
				.setTextContent(getResourceUrn(resourceId, apiscolInstanceName));
		stateElement.setTextContent("done");
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href",
				getResourceAtomXMLUri(baseUri, resourceId));
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("Resource deleted " + warnings);
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(idElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		return report;
	}

	@Override
	public Document getResourceUnsuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String warnings) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("failed");
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href",
				getResourceArchiveDownloadUri(baseUri, resourceId));
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("Resource not deleted " + warnings);
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		return report;
	}

	@Override
	public Document getInexistentFileDestructionAttemptReport(URI baseUri,
			String resourceId, String fileName) {
		return null;
	}

	@Override
	public Document getCompleteResourceListRepresentation(URI baseUri,
			String apiscolInstanceName, int start, int rows, String editUri)
			throws Exception {
		ArrayList<String> resourcesList = getResourcesList();
		Document response = XMLUtils.createXMLDocument();
		Element rootElement = response.createElement("feed");
		Element titleElement = response.createElement("title");
		Element idElement = response.createElement("id");
		rootElement.appendChild(titleElement);
		rootElement.appendChild(idElement);
		Element updatedElement = response.createElement("updated");
		rootElement.appendChild(updatedElement);
		idElement.setTextContent(baseUri.toString());
		Element totalElement = response.createElement(UsedNamespaces.OPENSEARCH
				.getShortHand() + ":totalResults");
		totalElement.setTextContent(Integer.toString(resourcesList.size()));
		rootElement.appendChild(totalElement);
		Element startElement = response.createElement(UsedNamespaces.OPENSEARCH
				.getShortHand() + ":startIndex");
		startElement.setTextContent(Integer.toString(start));
		rootElement.appendChild(startElement);
		Element rowsElement = response.createElement(UsedNamespaces.OPENSEARCH
				.getShortHand() + ":itemsPerPage");

		rootElement.appendChild(rowsElement);
		Iterator<String> it = resourcesList.iterator();
		int counter = -1;
		int numResultsDisplayed = 0;
		long maxTime = 0;
		while (it.hasNext()) {
			String resourceId = it.next();
			counter++;
			if (counter < start)
				continue;
			if (counter >= (start + rows))
				break;
			numResultsDisplayed++;
			try {

				maxTime = (int) Math.max(
						maxTime,
						addXMLSubTreeForResource(response, rootElement,
								baseUri, apiscolInstanceName, resourceId,
								editUri));
			} catch (InexistentResourceInDatabaseException e) {
				logger.error(String
						.format("The resource %s was not found while trying to build xml representation",
								resourceId));
				e.printStackTrace();
			}

		}
		rowsElement.setTextContent(Integer.toString(numResultsDisplayed));
		updatedElement.setTextContent(TimeUtils.toRFC3339(maxTime));
		response.appendChild(rootElement);
		XMLUtils.addNameSpaces(response, UsedNamespaces.ATOM);
		return response;
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.APPLICATION_XML_TYPE;
	}

	@Override
	public String getResourceStringRepresentation(URI baseUri,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		return formatXML((Document) getResourceRepresentation(baseUri,
				apiscolInstanceName, resourceId, editUri));
	}

	private String formatXML(Document document) {
		DOMSource domSource = new DOMSource(document);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = tf.newTransformer();
		} catch (TransformerConfigurationException e1) {
			e1.printStackTrace();
		}
		try {
			transformer.transform(domSource, result);
		} catch (TransformerException e) {
			e.printStackTrace();
		}
		return writer.toString();
	}

	@Override
	public Document selectResourceFollowingCriterium(URI baseUri,
			String apiscolInstanceName, ISearchEngineResultHandler handler,
			int start, int rows, String editUri) throws DBAccessException {
		Document response = XMLUtils.createXMLDocument();
		Element resourcesElement = response.createElement("feed");
		Element titleElement = response.createElement("title");
		Element idElement = response.createElement("id");
		resourcesElement.appendChild(titleElement);
		resourcesElement.appendChild(idElement);
		idElement.setTextContent(baseUri.toString());
		Element updatedElement = response.createElement("updated");
		resourcesElement.appendChild(updatedElement);
		Element hitsElement = response.createElement("apiscol:hits");
		Element spellcheckElement = response
				.createElement("apiscol:spellcheck");

		Set<String> resultsIds = handler.getResultsIds();
		Iterator<String> it = resultsIds.iterator();
		Element totalElement = response.createElement(UsedNamespaces.OPENSEARCH
				.getShortHand() + ":totalResults");
		totalElement.setTextContent(Integer.toString(resultsIds.size()));
		resourcesElement.appendChild(totalElement);
		String resultId, resourceId, fileName, score, type;
		List<String> snippets;
		Set<String> yetSignaledResource = new HashSet<String>();
		int counter = -1;
		long maxTime = 0;
		while (it.hasNext()) {
			resultId = it.next();
			counter++;
			if (counter < start)
				continue;
			if (counter >= (start + rows))
				break;
			score = handler.getResultScoresById().get(resultId);
			type = handler.getResultTypesById().get(resultId);
			snippets = handler.getResultSnippetsById().get(resultId);
			List<String> matchFound = ResourcesKeySyntax
					.analyseSolrResultId(resultId);

			if (matchFound.isEmpty())
				continue;
			resourceId = matchFound.get(0);
			fileName = matchFound.get(1);
			if (!yetSignaledResource.contains(resourceId))
				try {
					maxTime = Math.max(
							maxTime,
							addXMLSubTreeForResource(response,
									resourcesElement, baseUri,
									apiscolInstanceName, resourceId, editUri));
					addXMLSubTreeForResult(response, hitsElement, resourceId,
							fileName, score, type, snippets,
							apiscolInstanceName);
					yetSignaledResource.add(resourceId);
				} catch (InexistentResourceInDatabaseException e) {
					logger.error(String
							.format("The resource %s was not found while trying to build xml representation",
									resourceId));
				}
		}
		updatedElement.setTextContent(TimeUtils.toRFC3339(maxTime));
		List<String> suggestionsforQuery = handler.getQuerySuggestions();
		Map<String, List<String>> suggestionsforTerms = handler
				.getWordSuggestionsByQueryTerms();
		addXMLSubTreeForSpellcheck(response, spellcheckElement,
				suggestionsforTerms, suggestionsforQuery);
		response.appendChild(resourcesElement);
		resourcesElement.appendChild(hitsElement);
		resourcesElement.appendChild(spellcheckElement);
		XMLUtils.addNameSpaces(response, UsedNamespaces.ATOM);
		return response;
	}

	@Override
	public Document getSuccessfullOptimizationReport(URI baseUri,
			UriInfo uriInfo) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("done");
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href", baseUri + uriInfo.getPath());
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("Search engine index has been optimized");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		return report;
	}

	@Override
	public Document getSuccessfulRecoveryReport(URI baseUri, UriInfo uriInfo) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("done");
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href", baseUri + uriInfo.getPath());
		Element messageElement = report.createElement("message");
		messageElement.setTextContent("Resource repository has been restored");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		return report;
	}

	@Override
	public Document getLinkUpdateProcedureRepresentation(URI baseUri,
			UriInfo uriInfo) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		String state = "";
		switch (LinkRefreshingHandler.getInstance().getCurrentState()) {
		case INACTIVE:
			state = "inactive";
			break;
		case RUNNING:
			state = "running";
			break;
		}

		stateElement.setTextContent(state);
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement
				.setAttribute("href", baseUri.toString() + uriInfo.getPath());
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		if (LinkRefreshingHandler.getInstance().getCurrentState() == State.RUNNING) {
			Element currentUrlElement = report.createElement("current_url");
			currentUrlElement.setTextContent(LinkRefreshingHandler
					.getInstance().getCurrentlyParsedUrl());

			rootElement.appendChild(currentUrlElement);
		} else {
			Element terminationElement = report.createElement("termination");
			String termination = "";
			switch (LinkRefreshingHandler.getInstance()
					.getLastProcessTermination()) {
			case SUCCESSFULL:
				termination = "successful";
				break;
			case ABORTED:
				termination = "aborted";
				break;
			case ERRORS:
				termination = "errors";
				break;
			case NONE:
				termination = "none";
				break;
			}
			terminationElement.setTextContent(termination);
			Element errorsElement = report.createElement("errors");
			errorsElement.setTextContent(String.valueOf(LinkRefreshingHandler
					.getInstance().getLastProcessNumberOfErrors()));

			rootElement.appendChild(terminationElement);
			rootElement.appendChild(errorsElement);
		}
		report.appendChild(rootElement);
		return report;
	}

	@Override
	public Document getRecoveryProcedureRepresentation(URI baseUri,
			UriInfo uriInfo, Integer nbLines) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("apiscol:status");
		Element stateElement = report.createElement("apiscol:state");
		String state = "";
		ContentRecoveryHandler instance = ContentRecoveryHandler.getInstance();
		switch (instance.getCurrentState()) {
		case INACTIVE:
			state = "inactive";
			break;
		case RECOVERY_RUNNING:
			state = "recovery_running";
			break;

		}

		stateElement.setTextContent(state);
		Element linkElement = report.createElementNS(
				UsedNamespaces.ATOM.getUri(), "link");
		linkElement.setAttribute("href",
				baseUri.toString() + "/" + uriInfo.getPath());
		linkElement.setAttribute("rel", "self");
		linkElement.setAttribute("type", "application/atom+xml");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		LinkedList<Pair<String, MessageTypes>> originalMessages = instance
				.getMessages();
		if (originalMessages != null) {
			@SuppressWarnings("unchecked")
			LinkedList<Pair<String, MessageTypes>> messages = (LinkedList<Pair<String, MessageTypes>>) originalMessages
					.clone();
			Iterator<Pair<String, MessageTypes>> it = messages.iterator();
			rootElement.appendChild(stateElement);
			rootElement.appendChild(linkElement);
			int counter = 0;
			int start = 0;
			if (nbLines > 0)
				start = Math.max(0, messages.size() - nbLines);
			while (it.hasNext()) {
				counter++;
				Pair<String, MessageTypes> message = it.next();
				if (counter - 1 < start) {
					continue;
				}

				Element messageElement = report
						.createElement("apiscol:message");
				messageElement.setAttribute("type", message.getValue()
						.toString());
				messageElement.setTextContent(message.getKey());
				rootElement.appendChild(messageElement);

			}
		}

		if (instance.getCurrentState() == ContentRecoveryHandler.State.RECOVERY_RUNNING) {
			Element currentResourceElement = report
					.createElement("apiscol:current_resource");
			currentResourceElement.setTextContent(instance
					.getCurrentlyProcessedResource());

			rootElement.appendChild(currentResourceElement);
		} else {
			Element terminationElement = report
					.createElement("apiscol:termination");
			String termination = "";
			switch (instance.getLastProcessTermination()) {
			case SUCCESSFULL:
				termination = "successful";
				break;
			case ABORTED:
				termination = "aborted";
				break;
			case ERRORS:
				termination = "errors";
				break;
			case NONE:
				termination = "none";
				break;
			}
			terminationElement.setTextContent(termination);
			Element errorsElement = report.createElement("apiscol:errors");
			errorsElement.setTextContent(String.valueOf(instance
					.getLastProcessNumberOfErrors()));

			rootElement.appendChild(terminationElement);
			rootElement.appendChild(errorsElement);
		}
		Element processedElement = report.createElement("apiscol:processed");
		processedElement.setTextContent(String.valueOf(instance
				.getPercentageOfDocumentProcessed()));
		rootElement.appendChild(processedElement);
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.ATOM);
		return report;
	}

	@Override
	public Document getSuccessfulGlobalDeletionReport() {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("status");
		Element stateElement = report.createElement("state");
		stateElement.setTextContent("done");
		Element messageElement = report.createElement("message");
		messageElement
				.setTextContent("All resource have been deleted in content repository.");
		rootElement.appendChild(stateElement);
		rootElement.appendChild(messageElement);
		report.appendChild(rootElement);
		return report;
	}

	@Override
	public Document getThumbListRepresentation(String resourceId,
			Map<String, Point> thumbsUris, URI baseUri,
			String apiscolInstanceName, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		Document list = XMLUtils.createXMLDocument();
		Element rootElement = list.createElement("apiscol:thumbs");
		addXMLSubTreeForResource(list, rootElement, baseUri,
				apiscolInstanceName, resourceId, editUri);
		Iterator<String> it = thumbsUris.keySet().iterator();
		while (it.hasNext()) {
			String url = (String) it.next();
			Element thumbElement = list.createElement("apiscol:thumb");
			String imageWidth = "0";
			String imageHeight = "0";
			if (thumbsUris.get(url) != null) {
				imageWidth = String.valueOf(thumbsUris.get(url).getX());
				imageHeight = String.valueOf(thumbsUris.get(url).getY());
			}
			thumbElement.setAttribute("width", imageWidth);
			thumbElement.setAttribute("height", imageHeight);
			Element uriElement = list.createElement("apiscol:link");
			uriElement.setAttribute("href", url);
			thumbElement.appendChild(uriElement);
			rootElement.appendChild(thumbElement);
		}
		list.appendChild(rootElement);
		XMLUtils.addNameSpaces(list, UsedNamespaces.ATOM);
		return list;
	}

	@Override
	public Document getResourceTechnicalInformations(URI baseUri,
			String apiscolInstanceName, String resourceId)
			throws ResourceDirectoryNotFoundException, DBAccessException,
			InexistentResourceInDatabaseException {
		Document infos = XMLUtils.createXMLDocument();
		Element rootElement = infos.createElement("infos");
		Element sizeElement = infos.createElement("size");
		Element languageElement = infos.createElement("language");
		Element locationElement = infos.createElement("location");
		Element technicalLocationElement = infos
				.createElement("technical-location");
		Element apiscolInstanceElement = infos
				.createElement("apiscol-instance");
		Element formatElement = infos.createElement("format");
		Element metadataElement = infos.createElement("metadata");
		metadataElement.setTextContent(getMetadataUri(resourceId));
		Element previewElement = infos.createElement("preview");
		previewElement
				.setTextContent(getResourcePreviewUri(baseUri, resourceId));
		rootElement.appendChild(metadataElement);
		rootElement.appendChild(sizeElement);
		rootElement.appendChild(languageElement);
		rootElement.appendChild(locationElement);
		rootElement.appendChild(technicalLocationElement);
		rootElement.appendChild(apiscolInstanceElement);
		rootElement.appendChild(formatElement);
		rootElement.appendChild(previewElement);
		IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
				.setDbType(DBTypes.mongoDB)
				.setParameters(dbConnexionParameters).build();

		sizeElement.setTextContent(getResourceSize(resourceId,
				resourceDataHandler));
		languageElement.setTextContent(getResourceLanguage(resourceId,
				resourceDataHandler));
		formatElement.setTextContent(getMimeType(resourceId,
				resourceDataHandler));
		locationElement.setTextContent(getResourceAtomXMLUri(baseUri,
				resourceId));

		technicalLocationElement.setTextContent(getTechnicalLocation(
				resourceId, resourceDataHandler, baseUri));
		apiscolInstanceElement.setTextContent(apiscolInstanceName);
		infos.appendChild(rootElement);
		XMLUtils.addNameSpaces(infos, UsedNamespaces.APISCOL);
		return infos;
	}

	@Override
	public Document getRefreshProcessRepresentation(
			Integer refreshProcessIdentifier, URI baseUri,
			RefreshProcessRegistry refreshProcessRegistry) {
		Document report = XMLUtils.createXMLDocument();
		Element rootElement = report.createElement("apiscol:status");
		Element stateElement = report.createElement("apiscol:state");
		String resourceId = refreshProcessRegistry
				.getResourceIdForTransfer(refreshProcessIdentifier);
		States transferState = refreshProcessRegistry
				.getTransferState(refreshProcessIdentifier);
		stateElement.setTextContent(transferState.toString());
		Element linkElement = report.createElement("link");
		linkElement.setAttribute(
				"href",
				getUrlForRefreshProcess(UriBuilder.fromUri(baseUri),
						resourceId, refreshProcessIdentifier).toString());
		linkElement.setAttribute("rel", "self");
		linkElement.setAttribute("type", "application/atom+xml");
		Element messageElement = report.createElement("apiscol:message");
		messageElement.setTextContent(refreshProcessRegistry
				.getMessage(refreshProcessIdentifier));
		rootElement.appendChild(stateElement);
		rootElement.appendChild(linkElement);
		rootElement.appendChild(messageElement);
		Element resourceLinkElement = report.createElement("link");

		resourceLinkElement.setAttribute("href",
				getResourceHTMLUri(baseUri, resourceId));
		resourceLinkElement.setAttribute("rel", "item");
		resourceLinkElement.setAttribute("type", "application/atom+xml");
		rootElement.appendChild(resourceLinkElement);
		report.appendChild(rootElement);
		XMLUtils.addNameSpaces(report, UsedNamespaces.ATOM);
		return report;
	}

}
