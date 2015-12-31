package fr.ac_versailles.crdp.apiscol.content.thumbs;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import fr.ac_versailles.crdp.apiscol.content.databaseAccess.IResourceDataHandler;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryNotFoundException;
import fr.ac_versailles.crdp.apiscol.content.previews.AbstractPreviewMaker;

public class FileThumbExtracter extends  AbstractThumbExtracter {

	@Override
	public Map<String, Point> getThumbsFromResource(String resourceId,
			IResourceDataHandler resourceDataHandler, String baseUrl,
			String mainFileName, double minDimensionsSum)
			throws ResourceDirectoryNotFoundException {
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
			String resourceThumbUrlFromFilePath = getResourceThumbUrlFromFilePath(baseUrl, resourceId,
					filePath);
			urlList.put(
					resourceThumbUrlFromFilePath, image);
		}
		return urlList;
	}

	

	

	

}
