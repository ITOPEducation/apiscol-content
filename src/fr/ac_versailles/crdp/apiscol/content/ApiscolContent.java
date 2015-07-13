package fr.ac_versailles.crdp.apiscol.content;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.core.Application;

import com.sun.jersey.spi.container.servlet.ServletContainer;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.DBAccessBuilder.DBTypes;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;

public class ApiscolContent extends ServletContainer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ApiscolContent() {

	}

	public ApiscolContent(Class<? extends Application> appClass) {
		super(appClass);
	}

	public ApiscolContent(Application app) {
		super(app);
	}

	@PreDestroy
	public void deinitialize() {
		DBAccessBuilder.deInitialize();
		ResourceApi.stopExecutors();
	}

	@PostConstruct
	public void initialize() {
		// nothing at this time
	}
}
