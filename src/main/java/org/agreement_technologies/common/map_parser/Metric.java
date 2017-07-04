package org.agreement_technologies.common.map_parser;

public interface Metric {

    int getMetricType();

    String getPreference();

    double getNumber();

    int getNumTerms();

    Metric getTerm(int index);
}
