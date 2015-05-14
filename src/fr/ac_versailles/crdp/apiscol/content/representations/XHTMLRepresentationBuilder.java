package fr.ac_versailles.crdp.apiscol.content.representations;

import java.awt.Point;
import java.io.InputStream;
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

	private AbstractRepresentationBuilder<Document> innerBuilder;

	public XHTMLRepresentationBuilder() {
		innerBuilder = new XMLRepresentationBuilder();
	}

	@Override
	public String getLinkUpdateProcedureRepresentation(UriInfo uriInfo) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceRepresentation(UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		InputStream xslStream = ResourcesLoader
				.loadResource("xsl/resourceXMLToHTMLTransformer.xsl");
		if (xslStream == null) {
			logger.error("Impossible de charger la feuille de transformation xsl");
		}
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				xslStream, innerBuilder.getResourceRepresentation(uriInfo,
						apiscolInstanceName, resourceId, editUri)));
	}

	@Override
	public String getCompleteResourceListRepresentation(UriInfo uriInfo,
			String apiscolInstanceName, int start, int rows, String editUri)
			throws Exception {
		InputStream xslStream = ResourcesLoader
				.loadResource("xsl/resourcesListXMLToHTMLTransformer.xsl");
		if (xslStream == null) {
			logger.error("Impossible de charger la feuille de transformation xsl");
		}
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				xslStream, (Document) innerBuilder
						.getCompleteResourceListRepresentation(uriInfo,
								apiscolInstanceName, start, rows, editUri)));
	}

	@Override
	public String selectResourceFollowingCriterium(UriInfo uriInfo,
			String apiscolInstanceName, ISearchEngineResultHandler handler,
			int start, int rows, String editUri) throws DBAccessException {
		InputStream xslStream = ResourcesLoader
				.loadResource("xsl/resourcesListXMLToHTMLTransformer.xsl");
		if (xslStream == null) {
			logger.error("Impossible de charger la feuille de transformation xsl");
		}
		return HTMLUtils.WrapInHTML5Headers((Document) XMLUtils.xsltTransform(
				xslStream, innerBuilder.selectResourceFollowingCriterium(
						uriInfo, apiscolInstanceName, handler, start, rows,
						editUri)));
	}

	@Override
	public MediaType getMediaType() {
		return MediaType.TEXT_HTML_TYPE;
	}

	@Override
	public String getResourceStringRepresentation(UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getFileSuccessfulDestructionReport(UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String fileName) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getInexistentFileDestructionAttemptReport(UriInfo uriInfo,
			String resourceId, String fileName) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceSuccessfulDestructionReport(UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String warnings) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceUnsuccessfulDestructionReport(UriInfo uriInfo,
			String apiscolInstanceName, String resourceId, String warnings) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getSuccessfullOptimizationReport(UriInfo uriInfo) {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getSuccessfulGlobalDeletionReport() {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getThumbListRepresentation(String resourceId,
			Map<String, Point> thumbsUris, UriInfo uriInfo,
			String apiscolInstanceName, String editUri)
			throws DBAccessException, InexistentResourceInDatabaseException {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public String getResourceTechnicalInformations(UriInfo uriInfo,
			String apiscolInstanceName, String resourceId)
			throws ResourceDirectoryNotFoundException, DBAccessException,
			InexistentResourceInDatabaseException {
		// TODO Auto-generated method stub
		return "not yet implemented";
	}

	@Override
	public Object getRefreshProcessRepresentation(
			Integer refreshProcessIdentifier, UriInfo uriInfo,
			RefreshProcessRegistry refreshProcessRegistry) {
		// TODO Auto-generated method stub
		return null;
	}

}
