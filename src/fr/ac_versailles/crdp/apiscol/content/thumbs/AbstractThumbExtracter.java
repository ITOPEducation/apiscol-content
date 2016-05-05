package fr.ac_versailles.crdp.apiscol.content.thumbs;

import java.awt.Point;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import fr.ac_versailles.crdp.apiscol.auth.oauth.OauthServersProxy;
import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ResourceDirectoryInterface;
import fr.ac_versailles.crdp.apiscol.content.previews.AbstractPreviewMaker;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.LogUtility;

public abstract class AbstractThumbExtracter implements ThumbExtracter {

	protected static Logger logger;
	protected OauthServersProxy oauthServersProxy;

	protected boolean imageSizeIsSufficient(Point imageSize,
			double minDimensionSum) throws InvalidImageException {
		double width = imageSize.getX();
		double height = imageSize.getY();
		return width + height > minDimensionSum;
	}

	protected void createLogger() {
		if (logger == null)
			logger = LogUtility
					.createLogger(this.getClass().getCanonicalName());
	}

	protected String getPreviewThumbUrlFromFilePath(String baseUri,
			String resourceId, String filePath) {
		return String
				.format("%s/previews%s",
						baseUri,
						FileUtils.getFilePathHierarchy("", resourceId + "/"
								+ filePath));
	}

	@Override
	public Map<String, Point> getThumbsFromPreview(String resourceId,
			String previewsRepoPath, String baseUrl) {
		Map<String, Point> urlList = new HashMap<String, Point>();
		Map<String, Point> imagesFilePathList = ResourceDirectoryInterface
				.getImagesInPreviewDirectoryList(AbstractPreviewMaker
						.buildPreviewsDirectoryPath(previewsRepoPath,
								resourceId));
		Iterator<String> it = imagesFilePathList.keySet().iterator();
		while (it.hasNext()) {
			String filePath = (String) it.next();
			Point image = imagesFilePathList.get(filePath);
			urlList.put(
					getPreviewThumbUrlFromFilePath(baseUrl, resourceId,
							filePath), image);
		}
		return urlList;
	}

	protected String getResourceThumbUrlFromFilePath(String baseUri,
			String resourceId, String filePath) {
		return String
				.format("%s/resources%s",
						baseUri,
						FileUtils.getFilePathHierarchy("", resourceId + "/"
								+ filePath));
	}

	public void setOAuthServersProxy(OauthServersProxy oauthServersProxy) {
		this.oauthServersProxy = oauthServersProxy;

	}

}
