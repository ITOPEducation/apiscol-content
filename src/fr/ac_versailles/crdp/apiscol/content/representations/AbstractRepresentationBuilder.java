package fr.ac_versailles.crdp.apiscol.content.representations;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.content.ContentType;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder.DBTypes;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.languageDetection.TikaLanguageDetector;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public abstract class AbstractRepresentationBuilder<T> implements
		IEntitiesRepresentationBuilder<T> {
	protected static Logger logger;
	protected Map<String, String> dbConnexionParameters;

	public AbstractRepresentationBuilder(Map<String, String> dbParams) {
		createLogger();
		this.dbConnexionParameters = dbParams;
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());

	}

	protected final ArrayList<String> getResourcesList() throws Exception {
		return ResourceDirectoryInterface.getResourcesList();
	}

	protected final ArrayList<String> getFileList(String resourceId)
			throws ResourceDirectoryNotFoundException {

		return ResourceDirectoryInterface.getFileNamesList(resourceId);
	}

	protected final String getFileDownloadUrl(URI baseUri, String resourceId,
			String fileName) {
		String encodedFileName = null;
		try {
			encodedFileName = URLEncoder.encode(fileName, "UTF-8").replace("+",
					"%20");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return String.format("%s/resources%s/%s", baseUri.toString(),
				FileUtils.getFilePathHierarchy("", resourceId.toString()),
				encodedFileName);
	}

	protected final String getResourceUrn(String resourceId,
			String apiscolInstanceName) {
		return String.format("urn:apiscol:%s:content:resource:%s",
				apiscolInstanceName, resourceId);
	}

	protected String getResourceScormUri(URI baseUri, String resourceId) {
		return String.format("%s?format=scorm",
				getResourceUri(baseUri, resourceId));
	}

	protected final String getResourceAtomXMLUri(URI baseuri, String resourceId) {
		return String.format("%s?format=xml",
				getResourceUri(baseuri, resourceId));
	}

	protected final String getResourceThumbsUri(URI baseUri, String resourceId) {
		return String.format("%s/thumbs", getResourceUri(baseUri, resourceId));
	}

	protected final String getResourcePreviewUri(URI baseUri, String resourceId) {
		return String.format("%s/index.html",
				getResourcePreviewDirectoryUri(baseUri, resourceId));
	}

	@Override
	public String getResourcePreviewDirectoryUri(URI baseUri, String resourceId) {
		return String.format("%s/previews%s", baseUri.toString(),
				FileUtils.getFilePathHierarchy("", resourceId));
	}

	protected final String getResourceHTMLUri(URI baseUri, String resourceId) {
		return getResourceUri(baseUri, resourceId);
	}

	protected final String getResourceEditUri(String editUri, String resourceId) {
		return String.format("%sresource/%s", editUri, resourceId);
	}

	protected final String getResourceEditMediaUri(String editUri,
			String resourceId, ContentType type) {
		if (type.equals(ContentType.url))
			return String.format("%surl_parsing", editUri, resourceId);
		return String.format("%stransfer", editUri, resourceId);
	}

	private final String getResourceUri(URI baseUri, String resourceId) {
		return String.format("%s/resource/%s", baseUri.toString(), resourceId);
	}

	protected final String getResourceArchiveDownloadUri(URI baseUri,
			String resourceId) {
		return String.format("%s/resources%s.zip", baseUri.toString(),
				FileUtils.getFilePathHierarchy("", resourceId.toString()));
	}

	protected String getMainFileForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
				.setDbType(DBTypes.mongoDB)
				.setParameters(dbConnexionParameters).build();
		return resourceDataHandler.getMainFileForResource(resourceId);
	}

	protected String getEtagForResource(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
				.setDbType(DBTypes.mongoDB)
				.setParameters(dbConnexionParameters).build();
		return resourceDataHandler.getEtagForResource(resourceId);
	}

	protected String getMetadataUri(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
				.setDbType(DBTypes.mongoDB)
				.setParameters(dbConnexionParameters).build();
		return resourceDataHandler.getMetadataForResource(resourceId);
	}

	protected ContentType getResourceType(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
				.setDbType(DBTypes.mongoDB)
				.setParameters(dbConnexionParameters).build();
		return ContentType.convertStringToType(resourceDataHandler
				.getScormTypeForResource(resourceId));
	}

	protected String getResourceUrl(String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
				.setDbType(DBTypes.mongoDB)
				.setParameters(dbConnexionParameters).build();
		return resourceDataHandler.getUrlForResource(resourceId);
	}

	protected String getTechnicalLocation(String resourceId,
			IResourceDataHandler resourceDataHandler, URI baseUri)
			throws DBAccessException, InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		String location = "";
		if (ContentType.isLink(resourceDataHandler
				.getScormTypeForResource(resourceId))) {
			location = resourceDataHandler.getUrlForResource(resourceId);
		} else {
			int nbFiles = ResourceDirectoryInterface.getFileList(resourceId,
					true).size();
			if (nbFiles == 0)
				return "";
			else if (nbFiles == 1) {

				String fileName = resourceDataHandler
						.getMainFileForResource(resourceId);
				location = getFileDownloadUrl(baseUri, resourceId, fileName);
			} else
				location = getResourceArchiveDownloadUri(baseUri, resourceId);
		}

		return location;
	}

	protected String getMimeType(String resourceId,
			IResourceDataHandler resourceDataHandler) throws DBAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		String mimeType = "";
		if (ContentType.isLink(resourceDataHandler
				.getScormTypeForResource(resourceId)))
			mimeType = MediaType.TEXT_HTML;
		else
			mimeType = ResourceDirectoryInterface.getMimeType(resourceId,
					resourceDataHandler.getMainFileForResource(resourceId));
		return mimeType;
	}

	protected String getResourceSize(String resourceId,
			IResourceDataHandler resourceDataHandler) throws DBAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		String resourceType = resourceDataHandler
				.getScormTypeForResource(resourceId);
		long size = 0;
		if (ContentType.isFile(resourceType))
			size = ResourceDirectoryInterface.calculateResourceSize(resourceId);
		return Long.toString(size / 1024);
	}

	protected String getResourceLanguage(String resourceId,
			IResourceDataHandler resourceDataHandler) throws DBAccessException,
			InexistentResourceInDatabaseException,
			ResourceDirectoryNotFoundException {
		String resourceType = resourceDataHandler
				.getScormTypeForResource(resourceId);
		String language = "";
		if (ContentType.isFile(resourceType))
			language = new TikaLanguageDetector()
					.detectLanguage(ResourceDirectoryInterface.getTextContent(
							resourceId, resourceDataHandler
									.getMainFileForResource(resourceId)));
		else {
			String urlForResource = resourceDataHandler
					.getUrlForResource(resourceId);
			try {

				language = new TikaLanguageDetector()
						.detectLanguage(ResourceDirectoryInterface
								.getTextContent(resourceId, new URL(
										urlForResource)));
			} catch (MalformedURLException e) {
				logger.error("Impossible to detect language for url "
						+ urlForResource);
			}
		}
		return language;
	}

	protected Object getUrlForRefreshProcess(UriBuilder baseUriBuilder,
			String resourceId, Integer refreshProcessIdentifier) {
		return baseUriBuilder.path("resource").path(resourceId).path("refresh")
				.path(refreshProcessIdentifier.toString()).build();
	}
}
