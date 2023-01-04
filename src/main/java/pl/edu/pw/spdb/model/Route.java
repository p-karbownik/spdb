package pl.edu.pw.spdb.model;

import java.util.List;

public record Route(List<RouteSegment> segments, double distance, double estimatedTime) {
}
