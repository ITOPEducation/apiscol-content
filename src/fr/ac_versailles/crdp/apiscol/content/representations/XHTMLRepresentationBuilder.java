package fr.ac_versailles.crdp.apiscol.content.representations;

import java.awt.Point;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.w3c.dom.Document;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.resources.ResourcesLoader;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineResultHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.HTMLUtils;
import fr.ac_versailles.crdp.apiscol.utils.XMLUtils;

public class XHTMLRepresentationBuilder extends
		AbstractRepresentationBuilder<String> {

	public XHTMLRepresentationBuilder(Map<String, String> dbParams) {
		super(dbParams);
		innerBuilder = new XMLRepresentationBuilder(dbParams);

	}

	private AbstractRepresentationBuilder<Document> innerBuilder;

	@Override
	public String getLinkUpdateProcedureRepresentation(URI baseUri,
			UriInfo uriInfo) {
		return "not yet implemented";
	}

	@Override
	public String getResourceRepresentation(URI baseUri,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		InputStream xslStream = ResourcesLoader
				.loadResource("xsl/resourceXMLToHTMLTransformer.xsl");
		if (xslStream == null) {
			logger.error("Impossible de charger la feuille de transformation xsl");
		}
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				xslStream, innerBuilder.getResourceRepresentation(baseUri,
						apiscolInstanceName, resourceId, editUri)));
	}

	@Override
	public String getCompleteResourceListRepresentation(URI baseUri,
			String apiscolInstanceName, int start, int rows, String editUri)
			throws Exception {
		InputStream xslStream = ResourcesLoader
				.loadResource("xsl/resourcesListXMLToHTMLTransformer.xsl");
		if (xslStream == null) {
			logger.error("Impossible de charger la feuille de transformation xsl");
		}
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				xslStream, (Document) innerBuilder
						.getCompleteResourceListRepresentation(baseUri,
								apiscolInstanceName, start, rows, editUri)));
	}

	@Override
	public String selectResourceFollowingCriterium(URI baseUri,
			String apiscolInstanceName, ISearchEngineResultHandler handler,
			int start, int rows, String editUri) throws DBAccessException {
		InputStream xslStream = ResourcesLoader
				.loadResource("xsl/resourcesListXMLToHTMLTransformer.xsl");
		if (xslStream == null) {
			logger.error("Impossible de charger la feuille de transformation xsl");
		}
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				xslStream, innerBuilder.selectResourceFollowingCriterium(
						baseUri, apiscolInstanceName, handler, start, rows,
						editUri)));
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.TEXT_HTML_TYPE;
	}

	@Override
	public String getResourceStringRepresentation(URI baseUri,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		return "not yet implemented";
	}

	@Override
	public String getFileSuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String fileName) {
		return "not yet implemented";
	}

	@Override
	public String getInexistentFileDestructionAttemptReport(URI baseUri,
			String resourceId, String fileName) {
		return "not yet implemented";
	}

	@Override
	public String getResourceSuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String warnings) {
		return "not yet implemented";
	}

	@Override
	public String getResourceUnsuccessfulDestructionReport(URI baseUri,
			String apiscolInstanceName, String resourceId, String warnings) {
		return "not yet implemented";
	}

	@Override
	public String getSuccessfullOptimizationReport(URI baseUri, UriInfo uriInfo) {
		return "not yet implemented";
	}

	@Override
	public String getSuccessfulGlobalDeletionReport() {
		return "not yet implemented";
	}

	@Override
	public String getThumbListRepresentation(String resourceId,
			Map<String, Point> thumbsUris, URI baseUri,
			String apiscolInstanceName, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		return "not yet implemented";
	}

	@Override
	public String getResourceTechnicalInformations(URI baseUri,
			String apiscolInstanceName, String resourceId)
			throws ResourceDirectoryNotFoundException, DBAccessException,
			InexistentResourceInDatabaseException {
		return "not yet implemented";
	}

	@Override
	public Object getRefreshProcessRepresentation(
			Integer refreshProcessIdentifier, URI baseUri,
			RefreshProcessRegistry refreshProcessRegistry) {
		return "not yet implemented";
	}

	@Override
	public String getSuccessfulRecoveryReport(URI baseUri, UriInfo uriInfo) {
		return "not yet implemented";
	}

	@Override
	public Object getRecoveryProcedureRepresentation(URI externalUri,
			UriInfo uriInfo, Integer nbLines) {
		return "not yet implemented";
	}

}
