package com.teleonome.framework.microcontroller.fieldpacketreceiver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.log4j.Logger;

import com.teleonome.framework.security.totp.TOTP;

// Checks a sender's TOTP code against the customer's shared secret (the same base32 secret every device of the
// customer has, Esp32SecretManager on the ESP32s - Paula's GetCommandCode). The current 30 s step and one on
// either side are accepted, for clock drift between Paula's RTC and the Pi. Codes are compared as numbers, so
// Paula's zero-padded "012345" and an unpadded "12345" are the same code.
public class FieldPacketTotp {

	private static final Logger logger = Logger.getLogger(FieldPacketTotp.class);
	private final FieldPacketSettings settings;

	public FieldPacketTotp(FieldPacketSettings s) {
		settings = s;
	}

	public boolean verify(String code) {
		if (code == null || !code.trim().matches("[0-9]{1,10}")) return false;
		String secret;
		try {
			secret = new String(Files.readAllBytes(settings.secretFile.toPath()), StandardCharsets.UTF_8).trim();
		} catch (Exception e) {
			logger.warn("Field Packet Receiver: cannot read the secret file " + settings.secretFile + ": " + e.getMessage());
			return false;
		}
		long given = Long.parseLong(code.trim());
		long now = System.currentTimeMillis();
		TOTP totp = new TOTP();
		for (int step = -1; step <= 1; step++) {
			try {
				String expected = totp.generateCurrentNumber(secret, now + step * TOTP.TIME_STEP_SECONDS * 1000L);
				if (Long.parseLong(expected.trim()) == given) return true;
			} catch (Exception e) {
				logger.warn("Field Packet Receiver: TOTP failed: " + e.getMessage());
				return false;
			}
		}
		return false;
	}
}
