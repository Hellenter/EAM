package io.github.hellenter.eam.core.link;

/**
 * Whether EAM rules apply in this game instance. A dedicated server and a singleplayer world always run EAM.
 * A client connected to someone else's server runs EAM only when that server answered with the EAM hello:
 * on a vanilla server (a hub without EAM) the client must predict vanilla block breaking, stack sizes and items,
 * or blocks look unbreakable and inventories desync.
 */
public final class LinkState {
	private static volatile boolean dedicatedServer;
	private static volatile boolean localServer;
	private static volatile boolean remoteHasEam;

	private LinkState() {
	}

	public static boolean active() {
		return dedicatedServer || localServer || remoteHasEam;
	}

	static void setDedicatedServer(boolean value) {
		dedicatedServer = value;
	}

	/** An integrated (singleplayer or LAN host) server is running in this game. */
	static void setLocalServer(boolean value) {
		localServer = value;
	}

	/** Client: the server we are connecting to sent the EAM hello. Reset at the start of every connection. */
	public static void setRemoteHasEam(boolean value) {
		remoteHasEam = value;
	}
}
