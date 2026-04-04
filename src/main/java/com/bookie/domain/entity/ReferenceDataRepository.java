package com.bookie.domain.entity;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ReferenceDataRepository {

    private static final List<String> BOOKS = List.of(
            "CREDIT-NY", "CREDIT-LN", "MUNI-EAST", "MUNI-WEST", "HY-DESK"
    );

    private static final List<String> COUNTERPARTIES = List.of(
            "GOLDMAN", "MORGAN STANLEY", "BARCLAYS", "CITI", "DEUTSCHE",
            "UBS", "HSBC", "BOFA", "WELLS", "NOMURA"
    );

    public List<String> getAllBooks() {
        return BOOKS;
    }

    public List<String> getAllCounterparties() {
        return COUNTERPARTIES;
    }
}