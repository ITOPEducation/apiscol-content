package fr.ac_versailles.crdp.apiscol.content.thumbs;

import fr.ac_versailles.crdp.apiscol.content.ContentType;
import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;

public class ThumbExtracterFactory {

	public static ThumbExtracter getExtracter(
			IResourceDataHandler resourceDataHandler, String resourceId)
			throws DBAccessException, InexistentResourceInDatabaseException {
		String scormType = resourceDataHandler
				.getScormTypeForResource(resourceId);
		if (ContentType.isFile(scormType))
			return new FileThumbExtracter();
		else {
			String url = resourceDataHandler.getUrlForResource(resourceId);
			if (url.toLowerCase().endsWith(".pdf")) {
				return new RemotePdfThumbExtracter();
			}
			return new WebPageThumbExtracter();
		}
	}

}
