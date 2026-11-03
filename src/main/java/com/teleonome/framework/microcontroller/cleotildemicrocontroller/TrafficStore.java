package com.teleonome.framework.microcontroller.cleotildemicrocontroller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;

/**
 * Cleotilde's traffic tables in the Teleonome Postgres (design: OpenWrtOne/documents/05-teleonome-design.md,
 * section 7c). Used by TrafficWatcher (writes) and by the web app's GetTraffic form (reads), so both use
 * the same SQL.
 *
 *   traffic_flows   bytes per hour x device x destination IP x port x protocol, with the destination's
 *                   country and domain name (from the device's own DNS lookup)
 *   traffic_dns     DNS queries per hour x device x domain
 *   traffic_events  the Suspicious Events the rules raised
 *   geoip_country   DB-IP Country Lite, loaded monthly by update_geoip.sh (OpenWrtOne/code/traffic)
 *
 * "mac" is the device's MAC address (lower case) when the device list knows the IP, "ip:192.168.1.x"
 * when it doesn't, and "router" for the router's own traffic.
 * Single tables (not the daily managed tables of Mnemosyne): rows are per hour, and a daily delete keeps
 * Traffic Flow Days / Traffic DNS Days of them.
 */
public class TrafficStore {

	public static final String ROUTER = "router";
	private static final int COUNTRY_CACHE_SIZE = 5000;

	private final Logger logger = Logger.getLogger(getClass());
	/** Where connections come from: the Teleonome's pool (or a plain JDBC URL in a test). */
	public interface ConnectionSource {
		Connection get() throws SQLException;

		void release(Connection c);
	}

	private final ConnectionSource db;
	private boolean tablesReady = false;
	private Boolean geoIpLoaded = null;
	private final Map<String, String> countryCache = new LinkedHashMap<String, String>(256, 0.75f, true) {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, String> e) {
			return size() > COUNTRY_CACHE_SIZE;
		}
	};

	public TrafficStore(PostgresqlPersistenceManager pm) {
		this(new ConnectionSource() {
			public Connection get() throws SQLException {
				return pm.getConnection();
			}

			public void release(Connection c) {
				pm.closeConnection(c);
			}
		});
	}

	public TrafficStore(ConnectionSource source) {
		this.db = source;
	}

	/** One accumulated row for traffic_flows (the watcher adds to it, the store upserts it). */
	public static class Flow {
		public final long hourMillis;
		public final String mac, remoteIp, proto;
		public final int remotePort;
		public String device = "", country = "", domain = "";
		public long upBytes, downBytes;
		public int connections;

		public Flow(long hourMillis, String mac, String remoteIp, int remotePort, String proto) {
			this.hourMillis = hourMillis;
			this.mac = mac;
			this.remoteIp = remoteIp;
			this.remotePort = remotePort;
			this.proto = proto;
		}

		public String key() {
			return hourMillis + "|" + mac + "|" + remoteIp + "|" + remotePort + "|" + proto;
		}
	}

	// ------------------------------------------------------------------ schema

	public synchronized boolean ensureTables() {
		if (tablesReady) return true;
		String[] ddl = {
				"create table if not exists traffic_flows (hour_millis bigint not null, mac text not null, device text,"
						+ " remote_ip inet not null, remote_port int not null, proto text not null, country text, domain text,"
						+ " up_bytes bigint not null default 0, down_bytes bigint not null default 0, connections int not null default 0,"
						+ " primary key (hour_millis, mac, remote_ip, remote_port, proto))",
				"create index if not exists traffic_flows_mac_hour on traffic_flows (mac, hour_millis)",
				"create table if not exists traffic_dns (hour_millis bigint not null, mac text not null, device text,"
						+ " domain text not null, queries int not null default 0, primary key (hour_millis, mac, domain))",
				"create index if not exists traffic_dns_mac_hour on traffic_dns (mac, hour_millis)",
				"create table if not exists traffic_events (id bigserial primary key, event_millis bigint not null,"
						+ " mac text, device text, rule text, detail text, country text, remote_ip text, domain text)",
				"create index if not exists traffic_events_millis on traffic_events (event_millis)",
				"create table if not exists geoip_country (start_ip inet primary key, end_ip inet not null, country text not null)" };
		Connection c = null;
		try {
			c = db.get();
			try (Statement s = c.createStatement()) {
				for (String sql : ddl) s.execute(sql);
			}
			tablesReady = true;
		} catch (SQLException e) {
			logger.warn("Could not create the traffic tables: " + e.getMessage());
		} finally {
			close(c);
		}
		return tablesReady;
	}

	// ------------------------------------------------------------------ writes

	/** Adds the rows to traffic_flows (bytes and connections add up when the row exists). */
	public boolean storeFlows(Collection<Flow> flows) {
		if (flows.isEmpty()) return true;
		String sql = "insert into traffic_flows (hour_millis, mac, device, remote_ip, remote_port, proto, country, domain,"
				+ " up_bytes, down_bytes, connections) values (?,?,?,?::inet,?,?,?,?,?,?,?)"
				+ " on conflict (hour_millis, mac, remote_ip, remote_port, proto) do update set"
				+ " up_bytes = traffic_flows.up_bytes + excluded.up_bytes,"
				+ " down_bytes = traffic_flows.down_bytes + excluded.down_bytes,"
				+ " connections = traffic_flows.connections + excluded.connections,"
				+ " device = excluded.device,"
				+ " domain = case when excluded.domain <> '' then excluded.domain else traffic_flows.domain end,"
				+ " country = case when excluded.country <> '' then excluded.country else traffic_flows.country end";
		Connection c = null;
		try {
			c = db.get();
			c.setAutoCommit(false);
			try (PreparedStatement p = c.prepareStatement(sql)) {
				for (Flow f : flows) {
					p.setLong(1, f.hourMillis);
					p.setString(2, f.mac);
					p.setString(3, f.device);
					p.setString(4, f.remoteIp);
					p.setInt(5, f.remotePort);
					p.setString(6, f.proto);
					p.setString(7, f.country);
					p.setString(8, f.domain);
					p.setLong(9, f.upBytes);
					p.setLong(10, f.downBytes);
					p.setInt(11, f.connections);
					p.addBatch();
				}
				p.executeBatch();
			}
			c.commit();
			return true;
		} catch (SQLException e) {
			logger.warn("Could not store " + flows.size() + " traffic rows: " + e.getMessage());
			rollback(c);
			return false;
		} finally {
			restoreAndClose(c);
		}
	}

	/** key "hourMillis|mac|domain" -> {device, queries}. */
	public boolean storeDns(Map<String, Object[]> counts) {
		if (counts.isEmpty()) return true;
		String sql = "insert into traffic_dns (hour_millis, mac, device, domain, queries) values (?,?,?,?,?)"
				+ " on conflict (hour_millis, mac, domain) do update set queries = traffic_dns.queries + excluded.queries,"
				+ " device = excluded.device";
		Connection c = null;
		try {
			c = db.get();
			c.setAutoCommit(false);
			try (PreparedStatement p = c.prepareStatement(sql)) {
				for (Map.Entry<String, Object[]> e : counts.entrySet()) {
					String[] k = e.getKey().split("\\|", 3);
					p.setLong(1, Long.parseLong(k[0]));
					p.setString(2, k[1]);
					p.setString(3, (String) e.getValue()[0]);
					p.setString(4, k[2]);
					p.setInt(5, (Integer) e.getValue()[1]);
					p.addBatch();
				}
				p.executeBatch();
			}
			c.commit();
			return true;
		} catch (SQLException e) {
			logger.warn("Could not store " + counts.size() + " DNS rows: " + e.getMessage());
			rollback(c);
			return false;
		} finally {
			restoreAndClose(c);
		}
	}

	public void storeEvent(JSONObject ev) {
		update("insert into traffic_events (event_millis, mac, device, rule, detail, country, remote_ip, domain) values (?,?,?,?,?,?,?,?)",
				ev.getLong("Time"), ev.optString("Mac"), ev.optString("Device"), ev.optString("Rule"), ev.optString("Detail"),
				ev.optString("Country"), ev.optString("Remote IP"), ev.optString("Domain"));
	}

	/** Deletes flows older than flowDays and DNS rows older than dnsDays (events are kept a year). */
	public void deleteOld(int flowDays, int dnsDays) {
		long now = System.currentTimeMillis();
		update("delete from traffic_flows where hour_millis < ?", now - flowDays * 86_400_000L);
		update("delete from traffic_dns where hour_millis < ?", now - dnsDays * 86_400_000L);
		update("delete from traffic_events where event_millis < ?", now - 366 * 86_400_000L);
	}

	// ------------------------------------------------------------------ GeoIP

	/** Two-letter country of an internet IPv4 address, "" when unknown or the table is empty. */
	public String country(String ip) {
		synchronized (countryCache) {
			String c = countryCache.get(ip);
			if (c != null) return c;
		}
		if (Boolean.FALSE.equals(geoIpLoaded)) return "";
		String country = "";
		Connection c = null;
		try {
			c = db.get();
			if (geoIpLoaded == null) {
				try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("select exists (select 1 from geoip_country)")) {
					geoIpLoaded = rs.next() && rs.getBoolean(1);
				}
				if (!geoIpLoaded) {
					logger.warn("geoip_country is empty: run update_geoip.sh on the Pi; countries stay blank until then");
					return "";
				}
			}
			try (PreparedStatement p = c.prepareStatement(
					"select country, end_ip >= ?::inet from geoip_country where start_ip <= ?::inet order by start_ip desc limit 1")) {
				p.setString(1, ip);
				p.setString(2, ip);
				try (ResultSet rs = p.executeQuery()) {
					if (rs.next() && rs.getBoolean(2)) country = rs.getString(1);
					if ("ZZ".equals(country)) country = "";  // DB-IP: reserved / unknown
				}
			}
		} catch (SQLException e) {
			logger.debug("country lookup " + ip + ": " + e.getMessage());
			return "";
		} finally {
			close(c);
		}
		synchronized (countryCache) {
			countryCache.put(ip, country);
		}
		return country;
	}

	/** After update_geoip.sh has run: look again. */
	public void forgetGeoIpState() {
		geoIpLoaded = null;
		synchronized (countryCache) {
			countryCache.clear();
		}
	}

	// ------------------------------------------------------------------ reads for the rules

	/** mac -> {upBytes, downBytes} for [from, to). */
	public Map<String, long[]> totalsByDevice(long from, long to) {
		Map<String, long[]> out = new HashMap<>();
		query("select mac, sum(up_bytes), sum(down_bytes) from traffic_flows where hour_millis >= ? and hour_millis < ? group by mac",
				rs -> out.put(rs.getString(1), new long[] { rs.getLong(2), rs.getLong(3) }), from, to);
		return out;
	}

	/** mac -> countries contacted before 'before' (for the New Country rule). */
	public Map<String, Set<String>> countriesByDevice(long before) {
		Map<String, Set<String>> out = new HashMap<>();
		query("select distinct mac, country from traffic_flows where hour_millis < ? and country <> ''",
				rs -> out.computeIfAbsent(rs.getString(1), k -> new HashSet<>()).add(rs.getString(2)), before);
		return out;
	}

	/** mac -> first hour it was seen talking (for the learning period of the New Country rule). */
	public Map<String, Long> firstSeen() {
		Map<String, Long> out = new HashMap<>();
		query("select mac, min(hour_millis) from traffic_flows group by mac", rs -> out.put(rs.getString(1), rs.getLong(2)));
		return out;
	}

	/** The events since 'since', newest first. */
	public JSONArray events(long since, int limit) {
		JSONArray out = new JSONArray();
		query("select id, event_millis, mac, device, rule, detail, country, remote_ip, domain from traffic_events"
				+ " where event_millis >= ? order by event_millis desc limit ?",
				rs -> out.put(new JSONObject().put("Id", rs.getLong(1)).put("Time", rs.getLong(2)).put("Mac", rs.getString(3))
						.put("Device", rs.getString(4)).put("Rule", rs.getString(5)).put("Detail", rs.getString(6))
						.put("Country", rs.getString(7)).put("Remote IP", rs.getString(8)).put("Domain", rs.getString(9))),
				since, limit);
		return out;
	}

	// ------------------------------------------------------------------ reads for the web page

	/** Per device since 'since': up/down bytes, connections, and its top countries. */
	public JSONArray devices(long since) {
		Map<String, JSONObject> byMac = new LinkedHashMap<>();
		query("select mac, max(device), sum(up_bytes), sum(down_bytes), sum(connections), count(distinct remote_ip),"
				+ " max(hour_millis) from traffic_flows where hour_millis >= ? group by mac order by sum(up_bytes) + sum(down_bytes) desc",
				rs -> byMac.put(rs.getString(1), new JSONObject().put("Mac", rs.getString(1)).put("Device", rs.getString(2))
						.put("Up", rs.getLong(3)).put("Down", rs.getLong(4)).put("Connections", rs.getLong(5))
						.put("Destinations", rs.getLong(6)).put("Last", rs.getLong(7)).put("Countries", new JSONObject())),
				since);
		query("select mac, country, sum(up_bytes + down_bytes) from traffic_flows where hour_millis >= ? and country <> ''"
				+ " group by mac, country order by 3 desc",
				rs -> {
					JSONObject d = byMac.get(rs.getString(1));
					if (d != null) d.getJSONObject("Countries").put(rs.getString(2), rs.getLong(3));
				}, since);
		return new JSONArray(byMac.values());
	}

	/** One device's top destinations since 'since', grouped by IP + port. */
	public JSONArray destinations(String mac, long since, int limit) {
		JSONArray out = new JSONArray();
		query("select host(remote_ip), remote_port, proto, max(country), max(domain), sum(up_bytes), sum(down_bytes),"
				+ " sum(connections), min(hour_millis), max(hour_millis) from traffic_flows where mac = ? and hour_millis >= ?"
				+ " group by remote_ip, remote_port, proto order by sum(up_bytes) + sum(down_bytes) desc limit ?",
				rs -> out.put(new JSONObject().put("IP", rs.getString(1)).put("Port", rs.getInt(2)).put("Proto", rs.getString(3))
						.put("Country", rs.getString(4)).put("Domain", rs.getString(5)).put("Up", rs.getLong(6))
						.put("Down", rs.getLong(7)).put("Connections", rs.getLong(8)).put("First", rs.getLong(9))
						.put("Last", rs.getLong(10))),
				mac, since, limit);
		return out;
	}

	/** One device's (or every device's, mac = "") most looked-up domains since 'since'. */
	public JSONArray domains(String mac, long since, int limit) {
		JSONArray out = new JSONArray();
		String sql = "select domain, sum(queries), max(hour_millis), max(device), count(distinct mac) from traffic_dns where hour_millis >= ?"
				+ (mac.isEmpty() ? "" : " and mac = ?") + " group by domain order by sum(queries) desc limit ?";
		RowHandler h = rs -> out.put(new JSONObject().put("Domain", rs.getString(1)).put("Queries", rs.getLong(2))
				.put("Last", rs.getLong(3)).put("Device", rs.getString(4)).put("Devices", rs.getLong(5)));
		if (mac.isEmpty()) query(sql, h, since, limit);
		else query(sql, h, since, mac, limit);
		return out;
	}

	/** Hourly up/down bytes since 'since', for one device or (mac = "") all of them. */
	public JSONArray hours(String mac, long since) {
		JSONArray out = new JSONArray();
		String sql = "select hour_millis, sum(up_bytes), sum(down_bytes) from traffic_flows where hour_millis >= ?"
				+ (mac.isEmpty() ? "" : " and mac = ?") + " group by hour_millis order by hour_millis";
		RowHandler h = rs -> out.put(new JSONArray().put(rs.getLong(1)).put(rs.getLong(2)).put(rs.getLong(3)));
		if (mac.isEmpty()) query(sql, h, since);
		else query(sql, h, since, mac);
		return out;
	}

	/** Bytes per country since 'since', all devices. */
	public JSONObject countries(long since) {
		JSONObject out = new JSONObject();
		query("select country, sum(up_bytes), sum(down_bytes), count(distinct mac) from traffic_flows where hour_millis >= ?"
				+ " group by country order by sum(up_bytes) + sum(down_bytes) desc",
				rs -> out.put(rs.getString(1) == null || rs.getString(1).isEmpty() ? "?" : rs.getString(1),
						new JSONObject().put("Up", rs.getLong(2)).put("Down", rs.getLong(3)).put("Devices", rs.getLong(4))),
				since);
		return out;
	}

	// ------------------------------------------------------------------ plumbing

	interface RowHandler {
		void row(ResultSet rs) throws SQLException;
	}

	private void query(String sql, RowHandler handler, Object... params) {
		Connection c = null;
		try {
			c = db.get();
			try (PreparedStatement p = c.prepareStatement(sql)) {
				bind(p, params);
				try (ResultSet rs = p.executeQuery()) {
					while (rs.next()) handler.row(rs);
				}
			}
		} catch (SQLException e) {
			logger.warn("traffic query failed: " + e.getMessage() + " -- " + sql);
		} finally {
			close(c);
		}
	}

	private boolean update(String sql, Object... params) {
		Connection c = null;
		try {
			c = db.get();
			try (PreparedStatement p = c.prepareStatement(sql)) {
				bind(p, params);
				p.executeUpdate();
			}
			return true;
		} catch (SQLException e) {
			logger.warn("traffic update failed: " + e.getMessage() + " -- " + sql);
			return false;
		} finally {
			close(c);
		}
	}

	private static void bind(PreparedStatement p, Object... params) throws SQLException {
		for (int i = 0; i < params.length; i++) {
			Object o = params[i];
			if (o instanceof Long) p.setLong(i + 1, (Long) o);
			else if (o instanceof Integer) p.setInt(i + 1, (Integer) o);
			else p.setString(i + 1, o == null ? "" : o.toString());
		}
	}

	private void rollback(Connection c) {
		try {
			if (c != null) c.rollback();
		} catch (SQLException e) {
			logger.debug(Utils.getStringException(e));
		}
	}

	private void restoreAndClose(Connection c) {
		try {
			if (c != null) c.setAutoCommit(true);
		} catch (SQLException e) {
			logger.debug(Utils.getStringException(e));
		}
		close(c);
	}

	private void close(Connection c) {
		if (c != null) db.release(c);
	}
}
