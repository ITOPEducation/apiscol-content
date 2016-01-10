package fr.ac_versailles.crdp.apiscol.content.fileSystemAccess;

import fr.ac_versailles.crdp.apiscol.ApiscolException;

public class ForcedPreviewInvalidMimeTypeException extends ApiscolException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ForcedPreviewInvalidMimeTypeException(String fileName) {
		super(
				String.format(
						"You sent the file %s for forced preview but its not a convenient mime type.",
						fileName));
	}

}
