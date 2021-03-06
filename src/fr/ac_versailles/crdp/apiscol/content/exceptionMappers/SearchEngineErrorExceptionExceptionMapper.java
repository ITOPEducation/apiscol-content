package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.searchEngine.SearchEngineErrorException;

@Provider
public class SearchEngineErrorExceptionExceptionMapper implements
		ExceptionMapper<SearchEngineErrorException> {
	@Override
	public Response toResponse(SearchEngineErrorException e) {
		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.type(MediaType.APPLICATION_XML).entity(e.getXMLMessage())
				.build();
	}
}