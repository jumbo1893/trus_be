package com.jumbo.trus.service.football.match;

public enum MatchProcessingResult {
    FULLY_PROCESSED,   // 1: Přidávají/upravují se detaily
    PARTIALLY_PROCESSED, // 2: Přidává/upravuje se zápas bez detailu
    UNPROCESSED         // 0: Zápas se nezpracovává
}