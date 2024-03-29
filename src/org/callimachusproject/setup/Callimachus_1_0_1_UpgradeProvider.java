package org.callimachusproject.setup;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.SystemProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.exceptions.RDFObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Callimachus_1_0_1_UpgradeProvider extends UpdateProvider {
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String SERVE_ALL = "../everything-else-public.ttl";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";

	private static final String RESOURCE_STRSTARTS = "SELECT ?resource { ?resource ?p ?o FILTER strstarts(str(?resource), str(<>)) }";
	private static final String GRAPH_STRSTARTS = "SELECT ?graph { GRAPH ?graph { ?s ?p ?o } FILTER strstarts(str(?graph), str(<>)) }";

	private final Logger logger = LoggerFactory
			.getLogger(Callimachus_1_0_1_UpgradeProvider.class);

	@Override
	public Updater updateFrom(final String origin, String version)
			throws IOException {
		if (!"1.0.1".equals(version))
			return null;
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				if (!isPresent(webapp, repository))
					return false;
				if (SystemProperties.getWebappCarFile().canRead()) {
					logger.info("Initializing {}", origin);
					clearCallimachusWebapp(origin, webapp, repository);
				}
				removeServeAll(repository);
				return true;
			}
		};
	}

	/**
	 * Remove the Callimachus webapp currently installed in <code>origin</code>.
	 * 
	 * @param origin
	 * @return <code>true</code> if the webapp was successfully removed
	 * @throws OpenRDFException
	 */
	boolean clearCallimachusWebapp(String origin, String webapp,
			CalliRepository repository) throws OpenRDFException {
		return deleteComponents(origin, webapp, repository)
				|| removeAllComponents(origin, webapp, repository);
	}

	private boolean isPresent(String webapp, CalliRepository repository)
			throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			return con.hasStatement(vf.createURI(webapp), null, null);
		} finally {
			con.close();
		}
	}

	private boolean deleteComponents(String origin, String webapp,
			CalliRepository repository) {
		try {
			try {
				repository.setSchemaGraphType(webapp + SCHEMA_GRAPH);
				repository.setCompileRepository(true);
				ObjectConnection con = repository.getConnection();
				try {
					con.begin();
					RDFObject folder = (RDFObject) con.getObject(webapp);
					Method DeleteComponents = findDeleteComponents(folder);
					try {
						logger.info("Removing {}", folder);
						invokeAndRemove(DeleteComponents, folder, origin, con);
						con.commit();
						return true;
					} catch (InvocationTargetException e) {
						try {
							throw e.getCause();
						} catch (Exception cause) {
							logger.warn(cause.toString());
						} catch (Error cause) {
							logger.warn(cause.toString());
						} catch (Throwable cause) {
							logger.warn(cause.toString());
						}
						con.rollback();
						return false;
					}
				} catch (IllegalAccessException e) {
					logger.debug(e.toString());
				} catch (NoSuchMethodException e) {
					logger.debug(e.toString());
				} finally {
					con.rollback();
					repository.setCompileRepository(false);
					con.close();
				}
			} finally {
				repository.setCompileRepository(false);
			}
		} catch (RDFObjectException e) {
			logger.debug(e.toString());
		} catch (OpenRDFException e) {
			logger.debug(e.toString());
		}
		return false;
	}

	private void invokeAndRemove(Method DeleteComponents, RDFObject folder,
			String origin, ObjectConnection con) throws IllegalAccessException,
			InvocationTargetException, OpenRDFException {
		int argc = DeleteComponents.getParameterTypes().length;
		DeleteComponents.invoke(folder, new Object[argc]);
		Resource target = folder.getResource();
		ValueFactory vf = con.getValueFactory();
		String parent = getParentFolder(target.stringValue());
		if (parent != null) {
			con.remove(vf.createURI(parent), null, target);
		}
		con.remove(target, null, null);
		con.remove((Resource) null, null, null, target);
	}

	private Method findDeleteComponents(Object folder)
			throws NoSuchMethodException {
		for (Method method : folder.getClass().getMethods()) {
			if ("DeleteComponents".equals(method.getName()))
				return method;
		}
		throw new NoSuchMethodException("DeleteComponents in " + folder);
	}

	private boolean removeAllComponents(String origin, String webapp,
			CalliRepository repository) throws OpenRDFException {
		boolean modified = false;
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			con.begin();
			String folder = webapp;
			TupleQueryResult results;
			results = con.prepareTupleQuery(QueryLanguage.SPARQL,
					GRAPH_STRSTARTS, folder).evaluate();
			try {
				while (results.hasNext()) {
					if (!modified) {
						modified = true;
						logger.info("Expunging {}", folder);
					}
					URI graph = (URI) results.next().getValue("graph");
					con.clear(graph);
				}
			} finally {
				results.close();
			}
			results = con.prepareTupleQuery(QueryLanguage.SPARQL,
					RESOURCE_STRSTARTS, folder).evaluate();
			try {
				while (results.hasNext()) {
					if (!modified) {
						modified = true;
						logger.info("Expunging {}", folder);
					}
					URI resource = (URI) results.next().getValue("resource");
					if (folder.equals(resource.stringValue())) {
						URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
						con.remove(resource, hasComponent, null);
					} else {
						con.remove(resource, null, null);
					}
				}
			} finally {
				results.close();
			}
			con.commit();
			return modified;
		} finally {
			con.rollback();
			con.close();
		}
	}

	private String getParentFolder(String folder) {
		int idx = folder.lastIndexOf('/', folder.length() - 2);
		if (idx < 0)
			return null;
		String parent = folder.substring(0, idx + 1);
		if (parent.endsWith("://"))
			return null;
		return parent;
	}

	boolean removeServeAll(CalliRepository repository)
			throws RepositoryException {
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			boolean modified = false;
			modified |= stopServingOther(con);
			con.commit();
			return modified;
		} finally {
			con.close();
		}
	}

	private boolean stopServingOther(ObjectConnection con)
			throws RepositoryException {
		boolean modified = false;
		ValueFactory vf = con.getValueFactory();
		URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
		URI NamedGraph = vf
				.createURI("http://www.w3.org/ns/sparql-service-description#NamedGraph");
		RepositoryResult<Statement> stmts = con.getStatements(null, RDF.TYPE,
				NamedGraph);
		try {
			while (stmts.hasNext()) {
				Statement st = stmts.next();
				Resource serve = st.getSubject();
				if (serve.stringValue().matches(".*" + SERVE_ALL)) {
					logger.info(
							"Other resources are no longer served publicly through {}",
							st.getSubject());
					con.clear(serve);
					con.remove((Resource) null, hasComponent, serve);
					modified = true;
				}
			}
		} finally {
			stmts.close();
		}
		return modified;
	}

}
