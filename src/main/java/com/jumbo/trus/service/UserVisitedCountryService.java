package com.jumbo.trus.service;

import com.jumbo.trus.dto.VisitedCountryResponse;
import com.jumbo.trus.entity.country.UserVisitedCountryEntity;
import com.jumbo.trus.repository.UserVisitedCountryRepository;
import com.jumbo.trus.repository.codebook.CountryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserVisitedCountryService {

    private final UserVisitedCountryRepository userVisitedCountryRepository;
    private final CountryRepository countryRepository;

    @Transactional
    public VisitedCountryResponse addVisitedCountry(
            Long userId,
            String countryCode
    ) {
        if (userId == null
                || countryCode == null
                || countryCode.isBlank()) {
            return null;
        }

        String normalizedCode = countryCode
                .trim()
                .toUpperCase(Locale.ROOT);

        if (!countryRepository.existsByCodeAndActiveTrue(normalizedCode)) {
            return null;
        }

        return userVisitedCountryRepository
                .insertIfNotExists(
                        userId,
                        normalizedCode,
                        LocalDateTime.now()
                )
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<VisitedCountryResponse> getVisitedCountryResponses(Long userId) {
        return userVisitedCountryRepository
                .findAllByUserIdOrderByFirstVisitedAtAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private VisitedCountryResponse toResponse(UserVisitedCountryEntity entity) {
        return new VisitedCountryResponse(
                entity.getCountry().getCode(),
                entity.getCountry().getNameCs(),
                entity.getFirstVisitedAt(),
                entity.getCountry().getContinent().getCode());
    }
}