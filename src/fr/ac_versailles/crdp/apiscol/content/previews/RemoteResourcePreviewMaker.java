package fr.ac_versailles.crdp.apiscol.content.previews;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import fr.ac_versailles.crdp.apiscol.content.RefreshProcessRegistry.States;
import fr.ac_versailles.crdp.apiscol.content.resources.ResourcesLoader;
import fr.ac_versailles.crdp.apiscol.utils.FileUtils;
import fr.ac_versailles.crdp.apiscol.utils.JSonUtils;

public class RemoteResourcePreviewMaker extends AbstractPreviewMaker {

	public RemoteResourcePreviewMaker(String resourceId,
			String previewsRepoPath, String entryPoint, 
			String previewUri) {
		super(resourceId, previewsRepoPath, entryPoint, previewUri);

	}

	@Override
	protected void createNewPreview() {
		trackingObject
				.updateStateAndMessage(
						States.pending,
						"The preview template is going to be copied and parameterized for this web page.");
		InputStream is = null;
		String path = "templates/remoteresourcepreviewwidget.html";

		is = ResourcesLoader.loadResource(path);

		if (is == null) {
			trackingObject.updateStateAndMessage(States.aborted,
					"Impossible to copy the preview template : " + path);
			return;
		}
		Map<String, String> tokens = new HashMap<String, String>();
		tokens.put("src", entryPoint);
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
		}
		String htmlPageFilePath = previewDirectoryPath + "/index.html";
		FileUtils.writeDataToFile(new StringReader(pageHtml), htmlPageFilePath);
		trackingObject
				.updateStateAndMessage(
						States.done,
						"The preview template has been successfully copied and parameterized for this web page.");
	}
}
