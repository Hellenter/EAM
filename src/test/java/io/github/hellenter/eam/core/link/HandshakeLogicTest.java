package io.github.hellenter.eam.core.link;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.hellenter.eam.core.link.HandshakeLogic.Result;

import org.junit.jupiter.api.Test;

class HandshakeLogicTest {
	@Test
	void clientWithoutChannelIsMissingTheMod() {
		assertEquals(Result.MISSING, HandshakeLogic.beforeHello(false));
		assertEquals(Result.OK, HandshakeLogic.beforeHello(true));
	}

	@Test
	void protocolsMustMatch() {
		assertEquals(Result.OK, HandshakeLogic.afterReply(1, 1));
		assertEquals(Result.INCOMPATIBLE, HandshakeLogic.afterReply(2, 1));
	}

	@Test
	void messageFillsPlaceholders() {
		assertEquals("EAM 0.2.0, have 0.1.0\nhttp://x",
				HandshakeLogic.format("EAM {version}, have {client}\\n{url}", "http://x", "0.2.0", "0.1.0"));
	}

	@Test
	void emptyUrlAsksTheAdmin() {
		assertEquals("спроси у администратора", HandshakeLogic.format("{url}", "", "1", "1"));
	}
}
