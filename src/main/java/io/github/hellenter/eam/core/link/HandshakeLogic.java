package io.github.hellenter.eam.core.link;

/**
 * Decides whether a client may join. The protocol number changes only when client and server stop understanding
 * each other (new blocks, items, entities, payloads), so builds with the same protocol can play together.
 */
public final class HandshakeLogic {
	/** Bump when a server can no longer accept clients built before this change. */
	public static final int PROTOCOL = 1;

	public enum Result { OK, MISSING, INCOMPATIBLE }

	private HandshakeLogic() {
	}

	/** Called before the reply: a client that cannot receive the hello has no EAM at all. */
	public static Result beforeHello(boolean clientHasChannel) {
		return clientHasChannel ? Result.OK : Result.MISSING;
	}

	public static Result afterReply(int serverProtocol, int clientProtocol) {
		return serverProtocol == clientProtocol ? Result.OK : Result.INCOMPATIBLE;
	}

	/** Fills {@code {url}}, {@code {version}} and {@code {client}} in a configured message; {@code \n} starts a new line. */
	public static String format(String template, String url, String serverVersion, String clientVersion) {
		String link = url == null || url.isBlank() ? "спроси у администратора" : url;
		return template.replace("\\n", "\n")
				.replace("{url}", link)
				.replace("{version}", serverVersion)
				.replace("{client}", clientVersion);
	}
}
