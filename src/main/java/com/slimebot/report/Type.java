package com.slimebot.report;

public enum Type {
	MSG("Nachricht"),
	USER("User");

	public final String str;

	Type(String str) {
		this.str = str;
	}
}
