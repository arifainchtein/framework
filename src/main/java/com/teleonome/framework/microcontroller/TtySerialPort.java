package com.teleonome.framework.microcontroller;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * A USB serial port (/dev/ttyUSB0, /dev/serial/by-id/...) opened as a plain file, as a
 * replacement for jSerialComm's SerialPort where only the two streams are needed (Cleotilde,
 * Annabelle). No native library is loaded, so nothing native can leak: jSerialComm's native
 * memory grows while a port is held open and gets Ra's/Sento's Hypothalamus OOM-killed.
 *
 * The line settings are made once with stty:
 *   raw        no line editing, no echo, no CR/LF translation
 *   -hupcl     closing the port does not drop DTR/RTS (on Wally those drive EN/IO0, so a drop
 *              would reset the ESP32)
 *   clocal     ignore modem control lines, so open() never waits for carrier
 *   -crtscts -ixon -ixoff   no flow control
 *
 * Reads behave like jSerialComm's TIMEOUT_READ_SEMI_BLOCKING: read() waits up to
 * readTimeoutMillis for the first byte and then throws SerialTimeoutException (an IOException,
 * so existing catch blocks keep working). available() is the kernel's FIONREAD on the tty.
 */
public class TtySerialPort {

	/** Thrown by read() when nothing arrived within the read timeout. */
	public static class SerialTimeoutException extends InterruptedIOException {
		public SerialTimeoutException(String message) {
			super(message);
		}
	}

	private final String device;
	private final int baudRate;
	private final long readTimeoutMillis;
	private FileInputStream in;
	private FileOutputStream out;

	public TtySerialPort(String device, int baudRate, long readTimeoutMillis) {
		this.device = device;
		this.baudRate = baudRate;
		this.readTimeoutMillis = readTimeoutMillis;
	}

	public synchronized boolean openPort() {
		closePort();
		try {
			Process stty = new ProcessBuilder("stty", "-F", device, String.valueOf(baudRate),
					"raw", "-echo", "-hupcl", "clocal", "-crtscts", "-ixon", "-ixoff", "cs8", "-cstopb", "-parenb")
					.redirectErrorStream(true).start();
			if (!stty.waitFor(5, TimeUnit.SECONDS) || stty.exitValue() != 0) {
				stty.destroyForcibly();
				return false;
			}
			in = new FileInputStream(device);
			out = new FileOutputStream(device);
			return true;
		} catch (IOException e) {
			closePort();
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	public synchronized boolean isOpen() {
		return in != null && out != null;
	}

	public synchronized void closePort() {
		try { if (in != null) in.close(); } catch (IOException e) { }
		try { if (out != null) out.close(); } catch (IOException e) { }
		in = null;
		out = null;
	}

	public String getDevice() {
		return device;
	}

	public InputStream getInputStream() {
		return new InputStream() {
			@Override
			public int read() throws IOException {
				waitForData();
				return in.read();
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				if (len == 0) return 0;
				waitForData();
				return in.read(b, off, Math.min(len, Math.max(1, in.available())));
			}

			@Override
			public int available() throws IOException {
				return in.available();
			}
		};
	}

	public OutputStream getOutputStream() {
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				out.write(b);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				out.write(b, off, len);
			}

			@Override
			public void flush() throws IOException {
				out.flush();
			}
		};
	}

	/** Polls FIONREAD every 10 ms; a raw-mode read() would otherwise block forever. */
	private void waitForData() throws IOException {
		if (!isOpen()) throw new IOException(device + " is not open");
		long deadline = System.currentTimeMillis() + readTimeoutMillis;
		while (in.available() == 0) {
			if (System.currentTimeMillis() >= deadline) {
				throw new SerialTimeoutException("no data from " + device + " in " + readTimeoutMillis + " ms");
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new InterruptedIOException("interrupted waiting for " + device);
			}
		}
	}
}
