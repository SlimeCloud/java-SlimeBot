package de.slimecloud.slimeball.features.report;

import de.cyklon.jevent.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportCreateEvent extends Event {
	private final Report report;
}
