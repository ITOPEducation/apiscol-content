package fr.ac_versailles.crdp.apiscol.content.thumbs;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.previews.AbstractPreviewMaker;
import fr.ac_versailles.crdp.apiscol.database.DBAccessException;
import fr.ac_versailles.crdp.apiscol.database.InexistentResourceInDatabaseException;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;

public class RemotePdfThumbExtracter extends AbstractThumbExtracter {
	public RemotePdfThumbExtracter() {
		createLogger();
	}

	@Override
	public Map<String, Point> getThumbsFromResource(String resourceId,
			IResourceDataHandler resourceDataHandler, String baseUrl,
			String mainFileName, double minDimensionsSum)
			throws ResourceDirectoryNotFoundException, DBAccessException,
			InexistentResourceInDatabaseException {
		String url = resourceDataHandler.getUrlForResource(resourceId);
		if (StringUtils.isEmpty(url) || !url.endsWith(".pdf")) {
			logger.warn(String
					.format("The provided url for remote pdf thumb extraction in content web service for resource %s is void or does not end by .pdf",
							resourceId));
			return new HashMap<String, Point>();
		}
		mainFileName = ResourceDirectoryInterface
				.downloadRemoteFileInResourceDirectory(resourceId, url);
		if (StringUtils.isEmpty(mainFileName)) {
			logger.warn(String
					.format("Impossible to get remote pdf file from url %s for resource %s",
							url, resourceId));
			return new HashMap<String, Point>();
		}
		Map<String, Point> urlList = new HashMap<String, Point>();
		Map<String, Point> imagesFilePathList = ResourceDirectoryInterface
				.getImagesList(resourceId, mainFileName);
		Iterator<String> it = imagesFilePathList.keySet().iterator();
		while (it.hasNext()) {
			String filePath = (String) it.next();
			Point image = imagesFilePathList.get(filePath);
			try {
				if (!imageSizeIsSufficient(image, minDimensionsSum))
					continue;
			} catch (InvalidImageException e) {
				e.printStackTrace();
				continue;
			}
			String resourceThumbUrlFromFilePath = getResourceThumbUrlFromFilePath(
					baseUrl, resourceId, filePath);
			urlList.put(resourceThumbUrlFromFilePath, image);
		}
		return urlList;
	}

}
