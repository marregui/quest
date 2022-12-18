package io.quest.plot;

public interface Column {

    void append(double value);

    double get(int i);

    double min();

    double max();

    default double delta(double factor) {
        return Math.abs(max() - min()) * factor;
    }

    int size();
}
