package fr.ac_versailles.crdp.apiscol.content.previews;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.resources.ResourcesLoader;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.JSonUtils;

public class RemoteResourceSnapshotPreviewMaker extends AbstractPreviewMaker {

	public RemoteResourceSnapshotPreviewMaker(String resourceId,
			String previewsRepoPath, String entryPoint, String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, previewUri);

	}

	@Override
	protected void createNewPreview() {
		// TODO check that the url is valid
		trackingObject.updateStateAndMessage(States.pending,
				"The web page is being converted to jpeg image.");
		Set<String> outputs = new HashSet<String>();
		outputs.add("image/png");

		List<String> images = ConversionServerInterface.askForConversion(
				entryPoint, outputs);
		if (images == null) {
			// TODO enregistrer en base l'absence de preview
			String message = "No preview image obtained from conversion server interface for resource"
					+ resourceId;
			logger.error(message);
			trackingObject.updateStateAndMessage(States.aborted, message);
			return;
		}
		trackingObject
				.updateStateAndMessage(
						States.pending,
						"The web page has been converted to a jpeg image and will be fetched back to ApiScol Content handler.");
		writePreviewFileToDisk(images.get(0), "snapshot", true);
		String imgSrc = previewUri + "/snapshot.png";

		trackingObject
				.updateStateAndMessage(
						States.pending,
						"The jpeg preview has been returned to ApiScol content. Preview web page is going to be built.");
		InputStream is = null;
		String path = "templates/remoteresourcesnapshotwidget.html";

		is = ResourcesLoader.loadResource(path);

		if (is == null) {
			trackingObject.updateStateAndMessage(States.aborted,
					"Impossible to copy the preview template : " + path);
			return;
		}
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("src", imgSrc);
		tokens.put("preview-id", resourceId);
		MapTokenResolver resolver = new MapTokenResolver(tokens);

		Reader source = new InputStreamReader(is);

		Reader reader = new TokenReplacingReader(source, resolver);

		String htmlWidgetFilePath = previewDirectoryPath + "/widget.html";

		FileUtils.writeDataToFile(reader, htmlWidgetFilePath);
		JSonUtils.convertHtmlFileToJson(htmlWidgetFilePath, "index.html.js");
		String pageHtml = "";
		String pagePath = "templates/previewpage.html";
		is = ResourcesLoader.loadResource(pagePath);
		if (is == null) {
			trackingObject.updateStateAndMessage(States.aborted,
					"The conversion process failed because of a template handling problem : "
							+ pagePath);
			return;
		}
		try {
			pageHtml = IOUtils.toString(is, "UTF-8");
			String widgetHtml = FileUtils.readFileAsString(htmlWidgetFilePath);
			pageHtml = pageHtml.replace("WIDGET", widgetHtml);
		} catch (IOException e) {
			trackingObject.updateStateAndMessage(
					States.aborted,
					"Impossible to copy the preview template : "
							+ e.getMessage());
			e.printStackTrace();
			return;
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		String htmlPageFilePath = previewDirectoryPath + "/index.html";
		FileUtils.writeDataToFile(new StringReader(pageHtml), htmlPageFilePath);
		trackingObject
				.updateStateAndMessage(
						States.done,
						"The preview template has been successfully copied and parameterized for this web page.");
	}
}
