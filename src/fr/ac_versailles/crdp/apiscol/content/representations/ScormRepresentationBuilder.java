package fr.ac_versailles.crdp.apiscol.content.representations;

import java.awt.Point;
import java.net.URI;
import java.util.Iterator;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;

public class ScormRepresentationBuilder extends
		AbstractRepresentationBuilder<Document> {
	public ScormRepresentationBuilder(Map<String, String> dbParams) {
		super(dbParams);
	}

	private static final String XMLNS = "http://www.w3.org/2000/xmlns/";
	private static final String IMS = "http://www.imsglobal.org/xsd/imscp_v1p1";
	private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
	private static final String ADLCP = "http://www.adlnet.org/xsd/adlcp_v1p3";
	private static final String ADLSEQ = "http://www.adlnet.org/xsd/adlseq_v1p3";
	private static final String ADLNAV = "http://www.adlnet.org/xsd/adlnav_v1p3";
	private static final String IMSS = "http://www.imsglobal.org/xsd/imsss";
	private static final String SCHEMA_LOCATION = "http://www.imsglobal.org/xsd/imscp_v1p1 imscp_v1p1.xsd"
			+ "http://www.adlnet.org/xsd/adlcp_v1p3 adlcp_v1p3.xsd"
			+ "http://www.adlnet.org/xsd/adlseq_v1p3 adlseq_v1p3.xsd"
			+ "http://www.adlnet.org/xsd/adlnav_v1p3 adlnav_v1p3.xsd"
			+ "http://www.imsglobal.org/xsd/imsss imsss_v1p0.xsd";

	@Override
	public Document getResourceRepresentation(URI baseUri,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		Document scormRepresentation = createScormXMLDocument();
		scormRepresentation.getDocumentElement().setAttribute("id",
				getResourceUrn(resourceId, apiscolInstanceName));
		addXMLSubTreeForResource(scormRepresentation, baseUri,
				apiscolInstanceName, resourceId);
		return scormRepresentation;
	}

	private void addXMLSubTreeForResource(Document XMLDocument, URI baseUri,
			String apiscolInstanceName, String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		Element resourcesElement = XMLDocument.createElement("resources");
		Element resourceElement = XMLDocument.createElement("resource");
		XMLDocument.getDocumentElement().appendChild(resourcesElement);
		resourcesElement.appendChild(resourceElement);
		resourceElement.setAttribute("identifier",
				getResourceUrn(resourceId, apiscolInstanceName));
		// TODO implement scorm
		resourceElement.setAttributeNS(ADLCP, "adlcp:scormType",
				getResourceType(resourceId).toString());
		// TODO implement main
		String mainFile = getMainFileForResource(resourceId);
		resourceElement.setAttribute("main", mainFile == null ? "" : mainFile);
		String metadataLocation = getMetadataUri(resourceId);
		if (StringUtils.isNotBlank(metadataLocation)) {
			Element metadataElement = XMLDocument.createElement("metadata");
			Element metadataLocationElement = XMLDocument.createElementNS(
					ADLCP, "adlcp:location");
			metadataLocationElement.setTextContent(metadataLocation);
			resourceElement.appendChild(metadataElement);
			metadataElement.appendChild(metadataLocationElement);
		}
		Iterator<String> it = null;

		try {
			it = getFileList(resourceId).iterator();
			while (it.hasNext()) {
				Element fileElement = XMLDocument.createElement("file");
				fileElement.setAttribute("href", it.next());
				resourceElement.appendChild(fileElement);

			}
		} catch (ResourceDirectoryNotFoundException e) {
			// TODO log the problem
			return;
		}

	}

	private static Document createScormXMLDocument() {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		docFactory.setNamespaceAware(true);
		DocumentBuilder docBuilder = null;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document scormManifest = docBuilder.newDocument();
		Element rootElement = scormManifest.createElement("manifest");
		scormManifest.appendChild(rootElement);

		scormManifest.getDocumentElement().setAttributeNS(XMLNS, "xmlns", IMS);
		scormManifest.getDocumentElement().setAttributeNS(XMLNS, "xmlns:xsi",
				XSI);
		scormManifest.getDocumentElement().setAttributeNS(XMLNS, "xmlns:adlcp",
				ADLCP);
		scormManifest.getDocumentElement().setAttributeNS(XMLNS,
				"xmlns:adlseq", ADLSEQ);
		scormManifest.getDocumentElement().setAttributeNS(XMLNS,
				"xmlns:adlnav", ADLNAV);
		scormManifest.getDocumentElement().setAttributeNS(XMLNS, "xmlns:imsss",
				IMSS);
		scormManifest.getDocumentElement().setAttributeNS(XSI,
				"xsi:schemaLocation", SCHEMA_LOCATION);
		Element metadataElement = scormManifest.createElement("metadata");
		Element schemaElement = scormManifest.createElement("schema");
		schemaElement.setTextContent("ADL SCORM");
		Element schemaVersionElement = scormManifest
				.createElement("schemaversion");
		schemaVersionElement.setTextContent("2004 4th Edition");
		rootElement.appendChild(metadataElement);
		metadataElement.appendChild(schemaElement);
		metadataElement.appendChild(schemaVersionElement);
		Element organizationsElement = scormManifest
				.createElement("organizations");
		rootElement.appendChild(organizationsElement);
		return scormManifest;
	}

	@Override
	public Document getFileSuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String fileName) {
		return null;
	}

	@Override
	public Document getInexistentFileDestructionAttemptReport(URI baseUri,
			String resourceId, String fileName) {
		return null;
	}

	@Override
	public Document getCompleteResourceListRepresentation(URI baseUri,
			String apiscolInstanceName, int start, int rows, String editUri) {
		return null;
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.APPLICATION_XML_TYPE;
	}

	@Override
	public String getResourceStringRepresentation(URI baseUri,
			String apiscolInstanceName, String string, String editUri) {
		return null;
	}

	@Override
	public Document selectResourceFollowingCriterium(URI baseUri,
			String apiscolInstanceName, ISearchEngineResultHandler handler,
			int start, int rows, String editUri) {
		return null;
	}

	@Override
	public Document getResourceSuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String warnings) {
		return null;
	}

	@Override
	public Document getResourceUnsuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String warnings) {
		return null;
	}

	@Override
	public Document getSuccessfullOptimizationReport(URI baseUri,
			UriInfo uriInfo) {
		return null;
	}

	@Override
	public Document getSuccessfulGlobalDeletionReport() {
		return null;
	}

	@Override
	public Document getResourceTechnicalInformations(URI baseUri,
			String apiscolInstanceName, String resourceId) {
		return null;
	}

	@Override
	public Document getThumbListRepresentation(String resourceId,
			Map<String, Point> thumbsUris, URI baseUri,
			String apiscolInstanceName, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		return null;
	}

	@Override
	public Object getRefreshProcessRepresentation(
			Integer refreshProcessIdentifier, URI baseUri,
			RefreshProcessRegistry refreshProcessRegistry) {
		return null;
	}

	@Override
	public Document getLinkUpdateProcedureRepresentation(URI baseUri,
			UriInfo uriInfo) {
		return null;
	}

	@Override
	public Document getSuccessfulRecoveryReport(URI baseUri, UriInfo uriInfo) {
		return null;
	}

	@Override
	public Object getRecoveryProcedureRepresentation(URI externalUri,
			UriInfo uriInfo, Integer nbLines)

	{
		return null;
	}

}
