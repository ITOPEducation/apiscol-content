package fr.ac_versailles.crdp.apiscol.content.representations;

import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.core.MediaType;

import fr.ac_versailles.crdp.apiscol.CustomMediaType;

public class EntitiesRepresentationBuilderFactory {

	public static IEntitiesRepresentationBuilder<?> getRepresentationBuilder(
			String requestedFormat, ServletContext context,
			Map<String, String> dbParams)
			throws UnknownMediaTypeForResponseException {

		if (requestedFormat.equals(MediaType.APPLICATION_XML.toString())
				|| requestedFormat.equals(MediaType.APPLICATION_ATOM_XML
						.toString())) {
			return new XMLRepresentationBuilder(dbParams);
		} else if (requestedFormat.equals(CustomMediaType.SCORM_XML.toString())) {
			return new ScormRepresentationBuilder(dbParams);
		} else if (requestedFormat.contains(MediaType.APPLICATION_XHTML_XML
				.toString())
				|| requestedFormat.contains(MediaType.TEXT_HTML.toString())) {
			return new XHTMLRepresentationBuilder(dbParams);
		}
		throw new UnknownMediaTypeForResponseException(requestedFormat);
	}

}
