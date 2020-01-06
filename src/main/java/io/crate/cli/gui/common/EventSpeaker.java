package io.crate.cli.gui.common;


public interface EventSpeaker<EventType extends Enum<?>> {

    default EventType eventType(Enum<?> eventType) {
        return (EventType) EventType.valueOf(eventType.getClass(), eventType.name());
    }
}