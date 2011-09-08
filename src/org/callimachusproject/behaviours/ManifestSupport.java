package org.callimachusproject.behaviours;

import info.aduna.net.ParsedURI;

import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.callimachusproject.concepts.Manifest;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.traits.SelfAuthorizingTarget;
import org.openrdf.http.object.traits.Realm;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.RDFObject;

public abstract class ManifestSupport implements Manifest, RDFObject {
	private static final BasicStatusLine _401;
	private static final BasicStatusLine _403;
	static {
		ProtocolVersion HTTP11 = new ProtocolVersion("HTTP", 1, 1);
		_401 = new BasicStatusLine(HTTP11, 401, "Unauthorized");
		_403 = new BasicStatusLine(HTTP11, 403, "Forbidden");
	}

	@Override
	public final String allowOrigin() {
		StringBuilder sb = new StringBuilder();
		for (Object origin : getCalliOrigins()) {
			if (sb.length() > 0) {
				sb.append(", ");
			}
			String str = origin.toString();
			if (str.equals("*"))
				return str;
			sb.append(str);
		}
		if (sb.length() < 1)
			return null;
		return sb.toString();
	}

	@Override
	public final boolean withAgentCredentials(String origin) {
		for (Object script : getCalliOrigins()) {
			String ao = script.toString();
			// must be explicitly listed ('*' does not qualify)
			if (origin.startsWith(ao) || ao.startsWith(origin)
					&& ao.charAt(origin.length()) == '/')
				return true;
		}
		return false;
	}

	@Override
	public final HttpResponse forbidden(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		Page forbidden = getCalliForbidden();
		if (forbidden == null)
			return null;
		String url = request.get("request-target")[0];
		ParsedURI parsed = new ParsedURI(url);
		String query = parsed.getQuery();
		String html = forbidden.calliConstructHTML(resource, query);
		StringEntity entity = new StringEntity(html, "UTF-8");
		entity.setContentType("text/html;charset=\"UTF-8\"");
		HttpResponse resp = new BasicHttpResponse(_403);
		resp.setHeader("Cache-Control", "no-store");
		resp.setEntity(entity);
		return resp;
	}

	@Override
	public boolean authorizeCredential(Object credential, String method,
			Object resource, Map<String, String[]> request) {
		String query = new ParsedURI(request.get("request-target")[0]).getQuery();
		assert resource instanceof SelfAuthorizingTarget;
		SelfAuthorizingTarget target = (SelfAuthorizingTarget) resource;
		return target.calliIsAuthorized(credential, method, query);
	}

	@Override
	public final String protectionDomain() {
		StringBuilder sb = new StringBuilder();
		for (Realm realm : getCalliAuthentications()) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(realm.protectionDomain());
		}
		if (sb.length() < 1)
			return null;
		return sb.toString();
	}

	@Override
	public HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) throws Exception {
		HttpResponse unauth = null;
		Page unauthorized = getCalliUnauthorized();
		for (Realm realm : getCalliAuthentications()) {
			unauth = realm.unauthorized(method, resource, request);
			if (unauth != null)
				break;
		}
		if (unauthorized == null)
			return unauth;
		try {
			String url = request.get("request-target")[0];
			ParsedURI parsed = new ParsedURI(url);
			String query = parsed.getQuery();
			String html = unauthorized.calliConstructHTML(resource, query);
			StringEntity entity = new StringEntity(html, "UTF-8");
			entity.setContentType("text/html;charset=\"UTF-8\"");
			BasicHttpResponse resp = new BasicHttpResponse(_401);
			for (Header hd : unauth.getAllHeaders()) {
				if (hd.getName().equalsIgnoreCase("Transfer-Encoding"))
					continue;
				if (hd.getName().toLowerCase().startsWith("content-"))
					continue;
				resp.setHeader(hd);
			}
			resp.setHeader("Cache-Control", "no-store");
			resp.setEntity(entity);
			return resp;
		} finally {
			if (unauth != null) {
				HttpEntity entity = unauth.getEntity();
				if (entity != null) {
					entity.consumeContent();
				}
			}
		}
	}

	@Override
	public Object authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException {
		for (Realm realm : getCalliAuthentications()) {
			Object ret = realm.authenticateRequest(method, resource, request);
			if (ret != null)
				return ret;
		}
		return null;
	}

	@Override
	public HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request) {
		HttpMessage msg = null;
		for (Realm realm : getCalliAuthentications()) {
			HttpMessage resp = realm.authenticationInfo(method, resource, request);
			if (msg == null) {
				msg = resp;
			} else if (resp != null) {
				resp.setHeaders(msg.getAllHeaders());
				msg = resp;
			}
		}
		return msg;
	}

}
