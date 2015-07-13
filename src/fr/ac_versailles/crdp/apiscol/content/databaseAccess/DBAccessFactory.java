package fr.ac_versailles.crdp.apiscol.content.databaseAccess;

import java.util.Map;

import fr.ac_versailles.crdp.apiscol.database.DBAccessException;

public class DBAccessFactory {

	private static DBTypes dbType;
	private static IResourceDataHandler resourceDataHandler;
	private static Map<String, String> parameters;

	public enum DBTypes {
		mongoDB("mongodb");
		private String value;

		private DBTypes(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return value;
		}
	}

	public IResourceDataHandler build()
			throws DBAccessException {
		if (resourceDataHandler != null)
			return resourceDataHandler;
		if (dbType == null)
			throw new DBAccessException(
					"Set dbType before asking for connexion");
		if (parameters == null)
			throw new DBAccessException("Please provide connexion parameters");
		switch (dbType) {
		case mongoDB:
			resourceDataHandler = new MongoResourceDataHandler(parameters);
			return resourceDataHandler;
		default:
			throw new DBAccessException(String.format(
					"Type of connexion %s not implemented", dbType.toString()));
		}
	}

	public DBAccessFactory setDbType(DBTypes dbType) {
		DBAccessFactory.dbType = dbType;
		return this;
	}

	public DBAccessFactory setParameters(Map<String, String> parameters) {
		DBAccessFactory.parameters = parameters;
		return this;
	}

	public static void deInitialize() {
		if (resourceDataHandler != null)
			resourceDataHandler.deInitialize();

	}

}
