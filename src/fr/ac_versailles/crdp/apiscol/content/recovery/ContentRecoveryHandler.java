package fr.ac_versailles.crdp.apiscol.content.recovery;

import java.util.LinkedList;

import org.apache.solr.common.util.Pair;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.recovery.ContentRecoveyWorker.Terminations;
import fr.ac_versailles.crdp.apiscol.content.searchEngine.ISearchEngineQueryHandler;

public class ContentRecoveryHandler {
	private int totalNumberOfDocuments = 0;
	private int numberOfDocumentsProcessed = 0;

	public enum State {
		INACTIVE, RECOVERY_RUNNING
	}

	public enum MessageTypes {
		errorType("error_type"), warningType("warning_type"), infoType(
				"info_type");
		private String value;

		private MessageTypes(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}

	}

	private static LinkedList<Pair<String, MessageTypes>> messages;

	private ContentRecoveryHandler() {

	}

	private State currentState = State.INACTIVE;
	private ContentRecoveyWorker worker;

	private static ContentRecoveryHandler instance;
	private Terminations lastProcessTermination = Terminations.NONE;
	private String currentlyProcessedResource = "";
	private int lastProcessNumberOfErrors;

	public State getCurrentState() {
		return currentState;
	}

	public void startRecoveryProcess(
			ISearchEngineQueryHandler searchEngineQueryHandler,
			IResourceDataHandler resourceDataHandler) {
		messages = new LinkedList<Pair<String, MessageTypes>>();
		currentState = State.RECOVERY_RUNNING;
		worker = new ContentRecoveyWorker(searchEngineQueryHandler,
				resourceDataHandler, this);
		Thread refreshProcess = new Thread(worker);
		refreshProcess.start();
	}

	public static ContentRecoveryHandler getInstance() {
		if (instance == null)
			instance = new ContentRecoveryHandler();
		return instance;
	}

	public void notifyRefreshingProcessTermination(Terminations termination,
			int nbErrors) {
		this.lastProcessTermination = termination;
		this.lastProcessNumberOfErrors = nbErrors;
		currentState = State.INACTIVE;

	}

	public Terminations getLastProcessTermination() {
		return lastProcessTermination;
	}

	public String getCurrentlyProcessedResource() {
		return currentlyProcessedResource;
	}

	public void setCurrentlyProcessedResource(String currentlyProcessedResource) {
		this.currentlyProcessedResource = currentlyProcessedResource;
	}

	public int getLastProcessNumberOfErrors() {
		return lastProcessNumberOfErrors;
	}

	public LinkedList<Pair<String, MessageTypes>> getMessages() {
		return messages;
	}

	public void addMessage(String message) {

		addMessage(message, MessageTypes.infoType);
	}

	public void addMessage(String message, MessageTypes type) {
		Pair<String, MessageTypes> pair = new Pair<String, MessageTypes>(
				message, type);
		messages.add(pair);
	}

	public int getTotalNumberOfDocuments() {
		return totalNumberOfDocuments;
	}

	public void setTotalNumberOfDocuments(int totalNumberOfDocuments) {
		this.totalNumberOfDocuments = totalNumberOfDocuments;
	}

	public int getNumberOfDocumentsProcessed() {
		return numberOfDocumentsProcessed;
	}

	public void setNumberOfDocumentsProcessed(int numberOfDocumentsProcessed) {
		this.numberOfDocumentsProcessed = numberOfDocumentsProcessed;
	}

	public float getPercentageOfDocumentProcessed() {
		if (this.totalNumberOfDocuments == 0)
			return 0;
		return (float) this.numberOfDocumentsProcessed
				/ (float) this.totalNumberOfDocuments;
	}

}
