package halite.hlt;

import java.io.FileWriter;
import java.io.IOException;

public class F_Log {
	private final FileWriter file;

	private static F_Log INSTANCE;
	private static int TURN_NUMBER = 1;

	private F_Log(final FileWriter f) {
		this.file = f;
	}

	static void updateTurnNumber(final int turnNumber) {
		TURN_NUMBER = turnNumber;
	}

	static void open(final int botId) {
		if (INSTANCE != null) {
			throw new IllegalStateException(
					"Error: f-log: tried to open (" + botId + ") but we have already opened before");
		}

		final String fileName = "replays/f-logs/bot-" + botId + "-F_Log.json";
		final FileWriter writer;
		try {
			writer = new FileWriter(fileName);
		} catch (final IOException e) {
			throw new IllegalStateException(e);
		}
		INSTANCE = new F_Log(writer);
	}

	/**
	 * Writes the given message to the f-log
	 * 
	 * @param message
	 * @param args
	 */
	public static void log(final String message, Object... args) {
		if (INSTANCE == null) {
			return;
		}

		try {
			INSTANCE.file.append(String.format(message, args)).append('\n').flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes a line to the f-log in the following format:
	 * 
	 * <pre>
	 * {"t": turnNumber, "x": pos.x, "y": pos.y, "msg": msg, "color": color},
	 * </pre>
	 * 
	 * @param turnNumber - The turn number for which to display a message
	 * @param pos        - The Position for which to display a message
	 * @param color      - A color given to the cell at the provided Position
	 * @param msg        - The message to display
	 * @param msgArgs    - Optional
	 */
	public static void log(final Position pos, final String color, final String msg, Object... msgArgs) {
		if (INSTANCE == null) {
			return;
		}

		final String message;
		if (color.equals("")) {
			message = String.format("{\"t\": %d, \"x\": %d, \"y\": %d, \"msg\": \"%s\"},",
				TURN_NUMBER, pos.x, pos.y, String.format(msg, msgArgs));
		} else {
			message = String.format("{\"t\": %d, \"x\": %d, \"y\": %d, \"msg\": \"%s\", \"color\": \"%s\"},",
				TURN_NUMBER, pos.x, pos.y, String.format(msg, msgArgs), color);
		}

		try {
			INSTANCE.file.append(String.format(message)).append('\n').flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes a line to the f-log in the following format:
	 * 
	 * <pre>
	 * {"t": turnNumber, "x": pos.x, "y": pos.y, "color": color},
	 * </pre>
	 * 
	 * @param turnNumber - The turn number for which to display a message
	 * @param pos        - The Position for which to display a message
	 * @param color      - A color given to the cell at the provided Position
	 */
	public static void log(final Position pos, final String color) {
		if (INSTANCE == null) {
			return;
		}

		final String message = String.format("{\"t\": %d, \"x\": %d, \"y\": %d, \"color\": \"%s\"},", TURN_NUMBER,
				pos.x, pos.y, color);

		try {
			INSTANCE.file.append(String.format(message)).append('\n').flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}