package io.crate.cli.gui.common;


@FunctionalInterface
public interface EventListener<SourceType, EventDataType> {

    void onSourceEvent(SourceType source, Enum<?> eventType, EventDataType eventData);
}