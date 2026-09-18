package com.exa.rest.dto;

/** Subject frequency returned by the popular-message endpoint. */
public class PopularSubject {
    public String objet;
    public long occurrences;
    public String dernierePublication;

    public PopularSubject(String objet, long occurrences, String dernierePublication) {
        this.objet = objet;
        this.occurrences = occurrences;
        this.dernierePublication = dernierePublication;
    }
}
