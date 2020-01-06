package io.crate.cli.gui.common;


@FunctionalInterface
public interface EventListener<SourceType extends EventSpeaker, EventDataType> {

    void onSourceEvent(SourceType source, Enum<?> eventType, EventDataType eventData);
}