package fr.ac_versailles.crdp.apiscol.content.exceptionMappers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import fr.ac_versailles.crdp.apiscol.content.fileSystemAccess.ForcedPreviewInvalidMimeTypeException;

@Provider
public class ForcedPreviewInvalidMimeTypeExceptionMapper implements
		ExceptionMapper<ForcedPreviewInvalidMimeTypeException> {

	@Override
	public Response toResponse(ForcedPreviewInvalidMimeTypeException e) {
		return Response.status(Status.NOT_ACCEPTABLE)
				.type(MediaType.TEXT_PLAIN).entity(e.getMessage()).build();
	}
}