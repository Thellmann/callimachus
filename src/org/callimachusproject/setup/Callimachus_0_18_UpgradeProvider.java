package org.callimachusproject.setup;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.repository.object.exceptions.RDFObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Callimachus_0_18_UpgradeProvider extends UpdateProvider {
	private static final String MOVED_FILE = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "SELECT DISTINCT ?file { </callimachus/> calli:hasComponent ?file FILTER (?file=</callimachus/profile> || EXISTS { ?file a foaf:Document }) }";
	private static final String MOVED_FOLDER = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			+ "SELECT DISTINCT ?folder { </callimachus/> calli:hasComponent ?folder . ?folder a calli:Folder\n"
			+ "FILTER(?folder IN (</callimachus/types/>, </callimachus/editor/>, </callimachus/images/>, </callimachus/pages/>, </callimachus/pipelines/>, </callimachus/queries/>, </callimachus/schemas/>, </callimachus/scripts/>, </callimachus/styles/>, </callimachus/templates/>, </callimachus/theme/>, </callimachus/transforms/>)) }";

	private final Logger logger = LoggerFactory.getLogger(Callimachus_0_18_UpgradeProvider.class);

	@Override
	public Updater updateFrom(final String origin, String version)
			throws IOException {
		if (!"0.18".equals(version))
			return null;
		return new Updater() {
			public boolean update(String webapp,
					CalliRepository repository) throws IOException,
					OpenRDFException {
				boolean modified = false;
				modified |= trimNamespaces(repository);
				modified |= deleteFolders(origin, webapp, repository);
				modified |= deleteFiles(origin, webapp, repository);
				upgradeFrom1_0_beta(webapp, repository);
				return modified;
			}
		};
	}

	void upgradeFrom1_0_beta(String webapp,
			CalliRepository repository) throws OpenRDFException {
		String files = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n"
				 + "DELETE {\n"
				 + "    ?file ?p ?o\n"
				 + "} WHERE {\n"
				 + "    ?file a foaf:Document; ?p ?o\n"
				 + "    FILTER strstarts(str(?file), str(</callimachus/1.0/>))\n"
				 + "};";
		String folders = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
			 + "DELETE {\n"
			 + "    ?folder ?p ?o\n"
			 + "} WHERE {\n"
			 + "    ?folder a calli:Folder; ?p ?o\n"
			 + "    FILTER strstarts(str(?file), str(</callimachus/1.0/>))\n"
			 + "};";
		String components = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
				 + "DELETE {\n"
				 + "    ?folder calli:hasComponent ?file\n"
				 + "} WHERE {\n"
				 + "    ?folder calli:hasComponent ?file\n"
				 + "    FILTER strstarts(str(?folder), str(</callimachus/1.0/>))\n"
				 + "    FILTER strstarts(str(?file), str(</callimachus/1.0/>))\n"
				 + "};";
		String graphs = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n"
				 + "DELETE {\n"
				 + "    GRAPH ?graph { ?s ?p ?o }\n"
				 + "} WHERE {\n"
				 + "    GRAPH ?graph { ?s ?p ?o }\n"
				 + "    FILTER strstarts(str(?graph), str(</callimachus/1.0/>))\n"
				 + "};";
		ObjectConnection con = repository.getConnection();
		try {
			con.prepareUpdate(QueryLanguage.SPARQL, files, webapp).execute();
			con.prepareUpdate(QueryLanguage.SPARQL, folders, webapp).execute();
			con.prepareUpdate(QueryLanguage.SPARQL, components, webapp).execute();
			con.prepareUpdate(QueryLanguage.SPARQL, graphs, webapp).execute();
		} finally {
			con.close();
		}
	}

	boolean trimNamespaces(CalliRepository repository)
			throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			List<Namespace> result = con.getNamespaces().asList();
			boolean modified = false;
			Set<String> namespaces = new HashSet<String>();
			for (Namespace ns : result) {
				if (!namespaces.add(ns.getName())) {
					con.removeNamespace(ns.getPrefix());
					modified = true;
				}
			}
			return modified;
		} finally {
			con.close();
		}
	}

	boolean deleteFiles(String origin, String webapp, CalliRepository repository)
			throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			ValueFactory vf = con.getValueFactory();
			TupleQueryResult results = con.prepareTupleQuery(
					QueryLanguage.SPARQL, MOVED_FILE, webapp).evaluate();
			if (!results.hasNext())
				return false;
			try {
				while (results.hasNext()) {
					String file = results.next().getValue("file").stringValue();
					con.getBlobObject(file).delete();
					URI target = vf.createURI(file);
					con.remove(vf.createURI(origin + "/callimachus/"), null, target);
					con.remove(target, null, null);
					con.remove((Resource)null, null, null, target);
				}
			} finally {
				results.close();
			}
			con.commit();
		} finally {
			con.close();
		}
		return true;
	}

	boolean deleteFolders(final String o, String webapp,
			CalliRepository repository) throws RepositoryException,
			ObjectStoreConfigException {
		repository.setSchemaGraphType(o + "/callimachus/SchemaGraph");
		repository.addSchemaGraphType(o + "/callimachus/types/SchemaGraph");
		repository.addSchemaGraphType(o + "/callimachus/1.0/types/SchemaGraph");
		try {
			repository.setCompileRepository(true);
			List<String> folders = getFolders(webapp, repository);
			if (!folders.isEmpty()) {
				deleteFolders(folders, o, repository);
				return true;
			}
		} catch (OpenRDFException e) {
			logger.warn(e.toString());
		} catch (RDFObjectException e) {
			logger.warn(e.toString());
		} finally {
			repository.setCompileRepository(false);
		}
		return false;
	}

	private List<String> getFolders(String webapp,
			CalliRepository repository) throws OpenRDFException {
		List<String> list = new ArrayList<String>();
		ObjectConnection con = repository.getConnection();
		try {
			TupleQueryResult results = con.prepareTupleQuery(
					QueryLanguage.SPARQL, MOVED_FOLDER, webapp).evaluate();
			try {
				while (results.hasNext()) {
					list.add(results.next().getValue("folder").stringValue());
				}
			} finally {
				results.close();
			}
		} finally {
			con.close();
		}
		return list;
	}

	private void deleteFolders(List<String> folders, String origin,
			CalliRepository repository) throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			ValueFactory vf = con.getValueFactory();
			for (String folderUri : folders) {
				Object folder = con.getObject(folderUri);
				Method DeleteComponents = findDeleteComponents(folder);
				try {
					int argc = DeleteComponents.getParameterTypes().length;
					DeleteComponents.invoke(folder, new Object[argc]);
					URI target = vf.createURI(folderUri);
					con.remove(vf.createURI(origin + "/callimachus/"), null, target);
					con.remove(target, null, null);
					con.remove((Resource)null, null, null, target);
					con.commit();
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
					return;
				}
			}
			con.commit();
		} catch (IllegalAccessException e) {
			logger.warn(e.toString());
		} catch (NoSuchMethodException e) {
			logger.warn(e.toString());
		} finally {
			con.rollback();
			con.close();
		}
	}

	private Method findDeleteComponents(Object folder)
			throws NoSuchMethodException {
		for (Method method : folder.getClass().getMethods()) {
			if ("DeleteComponents".equals(method.getName()))
				return method;
		}
		throw new NoSuchMethodException("DeleteComponents in " + folder);
	}

}
