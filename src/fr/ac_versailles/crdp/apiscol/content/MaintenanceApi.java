package fr.ac_versailles.crdp.apiscol.content;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.w3c.dom.DOMException;

import fr.ac_versailles.crdp.apiscol.ApiscolApi;
import fr.ac_versailles.crdp.apiscol.ParametersKeys;
import fr.ac_versailles.crdp.apiscol.content.crawler.LinkRefreshingHandler;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder.DBTypes;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.FileSystemAccessException;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.recovery.ContentRecoveryHandler;
import fr.ac_versailles.crdp.apiscol.content.recovery.ContentRecoveryHandler.State;
import fr.ac_versailles.crdp.apiscol.content.representations.AbstractSearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.content.representations.EntitiesRepresentationBuilderFactory;
import fr.ac_versailles.crdp.apiscol.content.representations.IEntitiesRepresentationBuilder;
import fr.ac_versailles.crdp.apiscol.content.representations.UnknownMediaTypeForResponseException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineFactory;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineCommunicationException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineErrorException;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SolrJSearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLock;
import fr.ac_versailles.crdp.apiscol.transactions.KeyLockManager;

@Path("/maintenance")
public class MaintenanceApi extends ApiscolApi {

	private static ISearchEngineQueryHandler searchEngineQueryHandler;
	private static boolean isInitialized = false;
	private static ISearchEngineFactory searchEngineFactory;

	public MaintenanceApi(@Context ServletContext context) {
		super(context);
		if (!isInitialized) {
			initializeResourceDirectoryInterface(context);
			createSearchEngineQueryHandler(context);
			isInitialized = true;
		}
	}

	private void createSearchEngineQueryHandler(ServletContext context) {
		String solrAddress = ResourceApi.getProperty(
				ParametersKeys.solrAddress, context);
		String solrSearchPath = ResourceApi.getProperty(
				ParametersKeys.solrSearchPath, context);
		String solrUpdatePath = ResourceApi.getProperty(
				ParametersKeys.solrUpdatePath, context);
		String solrExtractPath = ResourceApi.getProperty(
				ParametersKeys.solrExtractPath, context);
		String solrSuggestPath = ResourceApi.getProperty(
				ParametersKeys.solrSuggestPath, context);
		try {
			searchEngineFactory = AbstractSearchEngineFactory
					.getSearchEngineFactory(AbstractSearchEngineFactory.SearchEngineType.SOLRJ);
		} catch (Exception e) {
			e.printStackTrace();
		}
		searchEngineQueryHandler = searchEngineFactory.getQueryHandler(
				solrAddress, solrSearchPath, solrUpdatePath, solrExtractPath,
				solrSuggestPath);
	}

	private void initializeResourceDirectoryInterface(ServletContext context) {
		if (!ResourceDirectoryInterface.isInitialized())
			ResourceDirectoryInterface.initialize(ResourceApi.getProperty(
					ParametersKeys.fileRepoPath, context), ResourceApi
					.getProperty(ParametersKeys.temporaryFilesPrefix, context));

	}

	/**
	 * Creates a void resource
	 * 
	 * @return resource representation
	 * @throws SearchEngineCommunicationException
	 * @throws SearchEngineErrorException
	 * @throws UnknownMediaTypeForResponseException
	 * @throws DBAccessException
	 * @throws InexistentResourceInDatabaseException
	 * @throws DOMException
	 * @throws FileSystemAccessException
	 * @throws ResourceDirectoryNotFoundException
	 */
	@POST
	@Path("/optimization")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response omptimizeSearchEngineIndex(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException,
			UnknownMediaTypeForResponseException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context,
						getDbConnexionParameters());
		searchEngineQueryHandler.processOptimizationQuery();
		return Response.ok(
				rb.getSuccessfullOptimizationReport(getExternalUri(), uriInfo),
				rb.getMediaType()).build();
	}

	@POST
	@Path("/link_update_process")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response startLinkUpdateProcedure(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			UnknownMediaTypeForResponseException {
		KeyLock keyLock = null;
		IEntitiesRepresentationBuilder<?> rb = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {
				logger.info("Entering critical section with mutual exclusion for all the content service");
				String requestedFormat = guessRequestedFormat(request, format);
				rb = EntitiesRepresentationBuilderFactory
						.getRepresentationBuilder(requestedFormat, context,
								getDbConnexionParameters());
				IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
						.setDbType(DBTypes.mongoDB)
						.setParameters(getDbConnexionParameters()).build();
				LinkRefreshingHandler.State state = LinkRefreshingHandler
						.getInstance().getCurrentState();
				if (state == LinkRefreshingHandler.State.INACTIVE) {
					LinkRefreshingHandler.getInstance().startUdateProcess(
							searchEngineQueryHandler, resourceDataHandler);
				}
			} finally {
				keyLock.unlock();
			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for all the content service"));
		}
		return Response
				.ok()
				.entity(rb.getLinkUpdateProcedureRepresentation(
						getExternalUri(), uriInfo)).build();
	}

	@GET
	@Path("/link_update_process")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response getLinkUpdateProcedureState(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			UnknownMediaTypeForResponseException {
		String requestedFormat = guessRequestedFormat(request, format);
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(requestedFormat, context,
						getDbConnexionParameters());
		return Response
				.ok()
				.entity(rb.getLinkUpdateProcedureRepresentation(
						getExternalUri(), uriInfo)).build();
	}

	@POST
	@Path("/deletion")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response deleteAllContents(
			@QueryParam(value = "format") final String format,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			UnknownMediaTypeForResponseException {
		KeyLock keyLock = null;
		IEntitiesRepresentationBuilder<?> rb = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {
				ResourceDirectoryInterface.deleteAllFiles();
				searchEngineQueryHandler.deleteIndex();
				IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
						.setDbType(DBTypes.mongoDB)
						.setParameters(getDbConnexionParameters()).build();
				resourceDataHandler.deleteAllDocuments();
				rb = EntitiesRepresentationBuilderFactory
						.getRepresentationBuilder(
								MediaType.APPLICATION_ATOM_XML, context,
								getDbConnexionParameters());
			} finally {
				keyLock.unlock();

			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for all the content service"));
		}

		return Response.ok().entity(rb.getSuccessfulGlobalDeletionReport())
				.build();
	}

	@POST
	@Path("/recovery")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response startRecovery(@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws Exception {
		KeyLock keyLock = null;
		IEntitiesRepresentationBuilder<?> rb = null;
		try {
			keyLock = keyLockManager.getLock(KeyLockManager.GLOBAL_LOCK_KEY);
			keyLock.lock();
			try {

				logger.info("Entering critical section with mutual exclusion for all the content service");
				rb = EntitiesRepresentationBuilderFactory
						.getRepresentationBuilder(
								MediaType.APPLICATION_ATOM_XML, context,
								getDbConnexionParameters());
				IResourceDataHandler resourceDataHandler = new DBAccessBuilder()
						.setDbType(DBTypes.mongoDB)
						.setParameters(getDbConnexionParameters()).build();
				State state = ContentRecoveryHandler.getInstance()
						.getCurrentState();
				if (state == State.INACTIVE) {
					ContentRecoveryHandler.getInstance().startRecoveryProcess(
							searchEngineQueryHandler, resourceDataHandler);
				}
			} finally {
				keyLock.unlock();

			}
		} finally {
			if (keyLock != null) {
				keyLock.release();
			}
			logger.info(String
					.format("Leaving critical section with mutual exclusion for all the content service"));
		}

		return Response
				.ok()

				.entity(rb.getRecoveryProcedureRepresentation(getExternalUri(),
						uriInfo, 0)).type(MediaType.APPLICATION_XML_TYPE)
				.build();
	}

	@GET
	@Path("/recovery")
	@Produces({ MediaType.APPLICATION_XML, MediaType.APPLICATION_ATOM_XML })
	public Response getRecoveryProcedureState(
			@DefaultValue("10") @QueryParam(value = "nblines") final Integer nblines,
			@Context HttpServletRequest request,
			@Context ServletContext context, @Context UriInfo uriInfo)
			throws SearchEngineErrorException,
			SearchEngineCommunicationException, DBAccessException,
			UnknownMediaTypeForResponseException {
		IEntitiesRepresentationBuilder<?> rb = EntitiesRepresentationBuilderFactory
				.getRepresentationBuilder(MediaType.APPLICATION_ATOM_XML,
						context, getDbConnexionParameters());
		return Response
				.ok()
				.entity(rb.getRecoveryProcedureRepresentation(getExternalUri(),
						uriInfo, nblines)).type(MediaType.APPLICATION_XML_TYPE)
				.build();
	}

}
