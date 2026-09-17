package com.jumbo.trus.service.recap;

import java.util.List;

public record SeasonRecap(String seasonName, String from, String to, List<Page> pages) {
    public record Metric(String label, String value) {}
    public record Standing(long subjectId, String name, int rank, double value, boolean mine) {}
    public record Board(String title, String unit, List<Standing> rows) {}
    public record Page(String kind, String title, String text, List<Metric> metrics, List<Board> boards) {}
}
