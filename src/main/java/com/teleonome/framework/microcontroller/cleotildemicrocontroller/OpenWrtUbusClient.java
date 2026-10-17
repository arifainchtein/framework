package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Talks to the OpenWrt router's ubus over HTTP JSON-RPC (http://router/ubus), logged in as the
 * restricted rpcd user "cleotilde" whose ACL (/usr/share/rpcd/acl.d/cleotilde.json on the router)
 * only allows the read calls Cleotilde needs. The session id is cached and renewed when the router
 * answers "Access denied" (sessions expire after 300 s of inactivity).
 */
public class OpenWrtUbusClient {

	private static final String NULL_SESSION = "00000000000000000000000000000000";
	private static final int ACCESS_DENIED = -32002;

	private final String url;
	private final String username;
	private final String password;
	private String session = null;
	private int timeoutMillis = 5000;
	Logger logger;

	public OpenWrtUbusClient(String routerAddress, String u, String p) {
		url = "http://" + routerAddress + "/ubus";
		username = u;
		password = p;
		logger = Logger.getLogger(getClass());
	}

	/**
	 * Calls object.method(args) and returns the data part of the answer (an empty JSONObject when
	 * the call succeeds without data). Throws IOException when the router can't be reached or the
	 * call fails.
	 */
	public synchronized JSONObject call(String object, String method, JSONObject args) throws IOException {
		if (session == null) login();
		JSONObject response = post(session, object, method, args);
		if (isAccessDenied(response)) {
			logger.debug("ubus session expired, logging in again");
			login();
			response = post(session, object, method, args);
		}
		return dataFrom(response, object + "." + method);
	}

	public JSONObject call(String object, String method) throws IOException {
		return call(object, method, new JSONObject());
	}

	private void login() throws IOException {
		JSONObject args = new JSONObject();
		args.put("username", username);
		args.put("password", password);
		JSONObject data = dataFrom(post(NULL_SESSION, "session", "login", args), "session.login");
		session = data.getString("ubus_rpc_session");
	}

	private boolean isAccessDenied(JSONObject response) {
		JSONObject error = response.optJSONObject("error");
		return error != null && error.optInt("code") == ACCESS_DENIED;
	}

	private JSONObject dataFrom(JSONObject response, String what) throws IOException {
		if (response.has("error")) {
			throw new IOException("ubus " + what + " failed: " + response.getJSONObject("error").optString("message"));
		}
		JSONArray result = response.getJSONArray("result");
		int status = result.getInt(0);
		if (status != 0) throw new IOException("ubus " + what + " returned status " + status);
		return result.length() > 1 ? result.getJSONObject(1) : new JSONObject();
	}

	private JSONObject post(String sid, String object, String method, JSONObject args) throws IOException {
		JSONObject request = new JSONObject();
		request.put("jsonrpc", "2.0");
		request.put("id", 1);
		request.put("method", "call");
		request.put("params", new JSONArray().put(sid).put(object).put(method).put(args));

		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setConnectTimeout(timeoutMillis);
		conn.setReadTimeout(timeoutMillis);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);
		try (OutputStream out = conn.getOutputStream()) {
			out.write(request.toString().getBytes(StandardCharsets.UTF_8));
		}
		try (InputStream in = conn.getInputStream()) {
			return new JSONObject(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		} finally {
			conn.disconnect();
		}
	}
}
