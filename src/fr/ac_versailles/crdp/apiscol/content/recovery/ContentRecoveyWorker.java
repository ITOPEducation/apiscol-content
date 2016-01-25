package fr.ac_versailles.crdp.apiscol.content.recovery;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.recovery.ContentRecoveryHandler.MessageTypes;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.SolrJSearchEngineQueryHandler;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public class ContentRecoveyWorker implements Runnable {

	private final ISearchEngineQueryHandler searchEngineQueryHandler;
	private final ContentRecoveryHandler caller;
	private final IResourceDataHandler resourceDataHandler;
	private Logger logger;

	public enum Terminations {
		SUCCESSFULL, ERRORS, ABORTED, NONE
	}

	public ContentRecoveyWorker(
			ISearchEngineQueryHandler searchEngineQueryHandler,
			IResourceDataHandler resourceDataHandler,
			ContentRecoveryHandler caller) {
		this.searchEngineQueryHandler = searchEngineQueryHandler;
		this.resourceDataHandler = resourceDataHandler;
		this.caller = caller;
		createLogger();
	}

	@Override
	public void run() {
		int nbErrors = 0;
		String message = "";
		Terminations termination = Terminations.SUCCESSFULL;
		try {
			searchEngineQueryHandler.deleteIndex();
		} catch (Exception e) {
			termination = Terminations.ABORTED;
			message = "Error encoutered while trying to delete search engine index "
					+ e.getMessage();
			logger.error(message);
			caller.addMessage(message, MessageTypes.errorType);
			caller.notifyRefreshingProcessTermination(termination, nbErrors);
			return;
		}
		try {
			resourceDataHandler.deleteAllDocuments();
		} catch (Exception e) {
			termination = Terminations.ABORTED;
			message = "Error encoutered while trying to clean database content "
					+ e.getMessage();
			logger.error(message);
			caller.addMessage(message, MessageTypes.errorType);
			caller.notifyRefreshingProcessTermination(termination, nbErrors);
			return;

		}
		ArrayList<String> resourceList = null;
		try {
			resourceList = ResourceDirectoryInterface.getResourcesList();
		} catch (Exception e) {
			termination = Terminations.ABORTED;
			message = "Error encoutered while trying get the list of resources from file system "
					+ e.getMessage();
			logger.error(message);
			caller.addMessage(message, MessageTypes.errorType);
			caller.notifyRefreshingProcessTermination(termination, nbErrors);
			return;
		}
		Iterator<String> it = resourceList.iterator();
		caller.setTotalNumberOfDocuments(resourceList.size());
		int nbOfDocumentProcessed = 0;
		while (it.hasNext()) {
			message = "================================================================";
			logger.info(message);
			caller.addMessage(message, MessageTypes.infoType);
			String resourceId = it.next();
			caller.setCurrentlyProcessedResource(resourceId);
			message = String.format("Resource %s (n. %d)", resourceId,
					nbOfDocumentProcessed + 1);
			logger.info(message);
			caller.addMessage(message, MessageTypes.infoType);
			String serializedData = null;
			try {
				serializedData = ResourceDirectoryInterface
						.getSerializedData(resourceId);
			} catch (Exception e) {
				nbErrors++;
				termination = Terminations.ERRORS;
				message = "Error encoutered while trying to get serialized data for resource "
						+ resourceId + " with messsage " + e.getMessage();
				logger.warn(message);
				caller.addMessage(message, MessageTypes.warningType);
				continue;
			}
			message = "Restoring serialized data for resource " + resourceId;
			logger.info(message);
			caller.addMessage(message, MessageTypes.infoType);
			try {
				resourceDataHandler.deserializeAndSaveToDataBase(resourceId,
						serializedData);
			} catch (Exception e) {
				nbErrors++;
				termination = Terminations.ERRORS;
				message = "Error encoutered while trying to deserialize and save data for resource "
						+ resourceId + " with messsage " + e.getMessage();
				logger.warn(message);
				caller.addMessage(message, MessageTypes.errorType);
				continue;
			}
			ArrayList<File> files = null;
			try {
				files = ResourceDirectoryInterface
						.getFileList(resourceId, true);
			} catch (ResourceDirectoryNotFoundException e) {
				nbErrors++;
				termination = Terminations.ERRORS;
				message = "Error encoutered while trying to get list of files frome file system for resource "
						+ resourceId + " with messsage " + e.getMessage();
				logger.warn(message);
				caller.addMessage(message, MessageTypes.warningType);
				continue;
			}
			Iterator<File> it2 = files.iterator();
			while (it2.hasNext()) {
				String fileName = it2.next().getName();
				try {
					searchEngineQueryHandler
							.processAddQueryForFile(
									SolrJSearchEngineQueryHandler
											.getDocumentIdentifier(resourceId,
													fileName),
									ResourceDirectoryInterface.getFilePath(
											resourceId, fileName));
				} catch (Exception e) {
					nbErrors++;
					termination = Terminations.ERRORS;
					message = "Error encoutered while trying to add file "
							+ fileName
							+ " to search engine index for resource "
							+ resourceId + " with messsage " + e.getMessage();
					logger.warn(message);
					caller.addMessage(message, MessageTypes.warningType);
					continue;
				}
				message = "Indexing file " + fileName + "for resource "
						+ resourceId;
				logger.info(message);
				caller.addMessage(message, MessageTypes.infoType);
				try {
					searchEngineQueryHandler.processCommitQuery();
				} catch (Exception e) {
					nbErrors++;
					termination = Terminations.ERRORS;
					message = "Error encoutered while trying to commit file "
							+ fileName
							+ " to search engine index for resource "
							+ resourceId + " with messsage " + e.getMessage();
					logger.warn(message);
					caller.addMessage(message, MessageTypes.warningType);
					continue;
				}

			}
			nbOfDocumentProcessed++;
			caller.setNumberOfDocumentsProcessed(nbOfDocumentProcessed);
		}
		message = "End of recovery process.";
		logger.info(message);
		caller.addMessage(message, MessageTypes.infoType);
		caller.notifyRefreshingProcessTermination(termination, nbErrors);
	}

	private void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());
	}

}
