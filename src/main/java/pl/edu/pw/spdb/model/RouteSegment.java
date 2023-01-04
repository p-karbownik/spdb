package pl.edu.pw.spdb.model;

import java.math.BigInteger;

public record RouteSegment(BigInteger node, BigInteger edge, double cost, double aggregatedCost) {
}
