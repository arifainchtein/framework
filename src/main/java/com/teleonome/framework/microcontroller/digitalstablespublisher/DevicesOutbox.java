package com.teleonome.framework.microcontroller.digitalstablespublisher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.postgresql.util.PGobject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.persistence.PostgresqlPersistenceManager;
import com.teleonome.framework.utils.Utils;

/**
 * The readings waiting to go to devices.digitalstables.com (DevicesDigitalStables design doc §16.6).
 * AnnabelleReader adds one row per (serialnumber, secondsTime) when "Upload To DigitalStables" is true;
 * DigitalStablesPublisherWriter sends the rows not yet acked and marks them acked when devicesheart
 * answers. The table is the buffer: an internet outage or a restart of the Pi or of devicesheart loses
 * nothing, the rows are just sent later. Created here on first use, like the Teleonome's daily tables.
 */
public class DevicesOutbox {

	/** The DeneWord that turns the upload on or off, in the publisher's config dene of the same name. */
	public static final String UPLOAD_DENEWORD = "Upload To DigitalStables";
	private static final long UPLOAD_FLAG_CACHE_MILLIS = 60 * 1000L;
	// a row sent but not acked after this long is sent again
	static final int RESEND_AFTER_SECONDS = 60;
	static final int KEEP_ACKED_DAYS = 30;

	private static final Logger logger = Logger.getLogger(DevicesOutbox.class);
	private static boolean tableChecked = false;
	private static boolean uploadEnabled = false;
	private static long uploadFlagReadAt = 0;

	/** One row to send: its id and the reading. */
	public static class Row {
		public final long id;
		public final JSONObject reading;
		public final long createdMillis;

		Row(long id, JSONObject reading, long createdMillis) {
			this.id = id;
			this.reading = reading;
			this.createdMillis = createdMillis;
		}
	}

	private DevicesOutbox() {
	}

	/**
	 * Called by AnnabelleReader for every telepathon it stores. Does nothing unless the Denome has
	 * "Upload To DigitalStables" = true. Never throws: the outbox must not disturb the reading itself.
	 */
	public static void addReading(DenomeManager aDenomeManager, JSONObject telepathon, long secondsTime) {
		try {
			if (!isUploadEnabled(aDenomeManager)) return;
			String serialnumber = serialOf(telepathon);
			if (serialnumber == null || serialnumber.isEmpty() || secondsTime <= 0) {
				logger.debug("not queued for DigitalStables: no serial number or time in telepathon " + telepathon.optString("Name"));
				return;
			}
			add(serialnumber, secondsTime, toReading(telepathon, serialnumber, secondsTime));
		} catch (Exception e) {
			logger.warn("could not queue a reading for DigitalStables: " + Utils.getStringException(e));
		}
	}

	/** True when the Denome's "Upload To DigitalStables" DeneWord is true; re-read once a minute. */
	public static synchronized boolean isUploadEnabled(DenomeManager aDenomeManager) {
		long now = System.currentTimeMillis();
		if (now - uploadFlagReadAt < UPLOAD_FLAG_CACHE_MILLIS) return uploadEnabled;
		uploadFlagReadAt = now;
		try {
			String pointer = "@" + aDenomeManager.getDenomeName() + ":" + TeleonomeConstants.NUCLEI_INTERNAL + ":"
					+ TeleonomeConstants.DENECHAIN_COMPONENTS + ":" + UPLOAD_DENEWORD + ":" + UPLOAD_DENEWORD;
			Object value = aDenomeManager.getDeneWordAttributeByIdentity(new Identity(pointer), TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			uploadEnabled = value != null && Boolean.parseBoolean(value.toString().trim());
		} catch (Exception e) {
			// not in this Denome: no publisher configured, nothing to queue
			uploadEnabled = false;
		}
		return uploadEnabled;
	}

	// Most telepathons carry "Serial Number" at the top; Gloria's only in her Configuration dene.
	static String serialOf(JSONObject telepathon) {
		String serial = telepathon.optString("Serial Number", "");
		if (!serial.isEmpty()) return serial;
		Object fromDene = valueOf(telepathon, TeleonomeConstants.TELEPATHON_DENE_CONFIGURATION, "Serial Number");
		return fromDene == null ? null : fromDene.toString();
	}

	/**
	 * The reading as devicesheart stores it: serialnumber and secondsTime at the top (they identify the
	 * reading on every path, §5.4), the device's name and type, every DeneWord value grouped by dene,
	 * and the raw line Annabelle sent.
	 */
	static JSONObject toReading(JSONObject telepathon, String serialnumber, long secondsTime) {
		JSONObject reading = new JSONObject();
		reading.put("serialnumber", serialnumber);
		reading.put("secondsTime", secondsTime);
		reading.put("devicename", telepathon.optString("Name", ""));
		reading.put("deviceshortname", telepathon.optString("Short Name", ""));
		Object type = valueOf(telepathon, TeleonomeConstants.TELEPATHON_DENE_CONFIGURATION, "Device Type Id");
		reading.put("deviceTypeId", type == null ? "" : type.toString());
		JSONObject values = new JSONObject();
		JSONArray denes = telepathon.optJSONArray("Denes");
		if (denes != null) {
			for (int i = 0; i < denes.length(); i++) {
				JSONObject dene = denes.optJSONObject(i);
				if (dene == null) continue;
				JSONObject words = new JSONObject();
				JSONArray deneWords = dene.optJSONArray("DeneWords");
				if (deneWords != null) {
					for (int j = 0; j < deneWords.length(); j++) {
						JSONObject w = deneWords.optJSONObject(j);
						if (w != null && w.has("Name")) words.put(w.getString("Name"), w.opt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE));
					}
				}
				values.put(dene.optString(TeleonomeConstants.DENE_NAME_ATTRIBUTE, "dene" + i), words);
			}
		}
		reading.put("values", values);
		reading.put("raw", telepathon.optString("Raw Data", ""));
		return reading;
	}

	private static Object valueOf(JSONObject telepathon, String deneName, String deneWordName) {
		JSONArray denes = telepathon.optJSONArray("Denes");
		if (denes == null) return null;
		for (int i = 0; i < denes.length(); i++) {
			JSONObject dene = denes.optJSONObject(i);
			if (dene == null || !deneName.equals(dene.optString(TeleonomeConstants.DENE_NAME_ATTRIBUTE))) continue;
			JSONArray deneWords = dene.optJSONArray("DeneWords");
			if (deneWords == null) return null;
			for (int j = 0; j < deneWords.length(); j++) {
				JSONObject w = deneWords.optJSONObject(j);
				if (w != null && deneWordName.equals(w.optString("Name"))) return w.opt(TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
			}
		}
		return null;
	}

	/** Adds a reading once; a second copy of the same (serialnumber, secondsTime) is ignored. */
	static void add(String serialnumber, long secondsTime, JSONObject reading) throws SQLException {
		ensureTable();
		String sql = "insert into devices_outbox(serialnumber, secondstime, payload) values(?,?,?) on conflict (serialnumber, secondstime) do nothing";
		Connection connection = PostgresqlPersistenceManager.instance().getConnection();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, serialnumber);
			statement.setLong(2, secondsTime);
			statement.setObject(3, jsonb(reading.toString()));
			statement.executeUpdate();
		} finally {
			PostgresqlPersistenceManager.instance().closeConnection(connection);
		}
	}

	/** Rows not acked that are new or were sent more than RESEND_AFTER_SECONDS ago, oldest first. */
	static List<Row> due(int limit) throws SQLException {
		ensureTable();
		String sql = "select id, payload::text, (extract(epoch from created_at)*1000)::bigint from devices_outbox "
				+ "where acked_at is null and (sent_at is null or sent_at < now() - interval '" + RESEND_AFTER_SECONDS + " seconds') "
				+ "order by id limit ?";
		List<Row> rows = new ArrayList<>();
		Connection connection = PostgresqlPersistenceManager.instance().getConnection();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setInt(1, limit);
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) rows.add(new Row(rs.getLong(1), new JSONObject(rs.getString(2)), rs.getLong(3)));
			}
		} finally {
			PostgresqlPersistenceManager.instance().closeConnection(connection);
		}
		return rows;
	}

	static void markSent(long id) throws SQLException {
		update("update devices_outbox set sent_at=now() where id=?", id, null);
	}

	/** The final answer from devicesheart (stored, duplicate, or a rejection): never sent again. */
	static void markAcked(long id, String outcome) throws SQLException {
		update("update devices_outbox set acked_at=now(), outcome=? where id=? and acked_at is null", id, outcome);
	}

	static int deleteOldAcked() throws SQLException {
		ensureTable();
		Connection connection = PostgresqlPersistenceManager.instance().getConnection();
		try (Statement statement = connection.createStatement()) {
			return statement.executeUpdate("delete from devices_outbox where acked_at < now() - interval '" + KEEP_ACKED_DAYS + " days'");
		} finally {
			PostgresqlPersistenceManager.instance().closeConnection(connection);
		}
	}

	/** {"pending": n, "oldestPendingSeconds": s} for the publisher's answer and the log. */
	static JSONObject status() throws SQLException {
		ensureTable();
		JSONObject status = new JSONObject();
		Connection connection = PostgresqlPersistenceManager.instance().getConnection();
		try (Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("select count(*), coalesce(extract(epoch from now()-min(created_at))::bigint,0) from devices_outbox where acked_at is null")) {
			rs.next();
			status.put("pending", rs.getLong(1));
			status.put("oldestPendingSeconds", rs.getLong(2));
		} finally {
			PostgresqlPersistenceManager.instance().closeConnection(connection);
		}
		return status;
	}

	private static void update(String sql, long id, String outcome) throws SQLException {
		Connection connection = PostgresqlPersistenceManager.instance().getConnection();
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			if (outcome != null) {
				statement.setString(1, outcome);
				statement.setLong(2, id);
			} else {
				statement.setLong(1, id);
			}
			statement.executeUpdate();
		} finally {
			PostgresqlPersistenceManager.instance().closeConnection(connection);
		}
	}

	private static synchronized void ensureTable() throws SQLException {
		if (tableChecked) return;
		Connection connection = PostgresqlPersistenceManager.instance().getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("create table if not exists devices_outbox ("
					+ "id bigserial primary key, "
					+ "serialnumber varchar(20) not null, "
					+ "secondstime bigint not null, "
					+ "payload jsonb not null, "
					+ "created_at timestamptz not null default now(), "
					+ "sent_at timestamptz, "
					+ "acked_at timestamptz, "
					+ "outcome varchar(20), "
					+ "unique (serialnumber, secondstime))");
			statement.executeUpdate("create index if not exists devices_outbox_pending on devices_outbox (id) where acked_at is null");
			tableChecked = true;
		} finally {
			PostgresqlPersistenceManager.instance().closeConnection(connection);
		}
	}

	private static PGobject jsonb(String json) throws SQLException {
		PGobject o = new PGobject();
		o.setType("jsonb");
		o.setValue(json);
		return o;
	}
}
