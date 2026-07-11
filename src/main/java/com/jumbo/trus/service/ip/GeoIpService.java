package com.jumbo.trus.service.ip;

import com.jumbo.trus.entity.codebook.CountryEntity;
import com.jumbo.trus.repository.codebook.CountryRepository;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CountryResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;

@Service
@Slf4j
public class GeoIpService {

    private DatabaseReader databaseReader;
    private CountryRepository countryRepository;

    @PostConstruct
    public void init() throws IOException {
        try (InputStream is = getClass()
                .getResourceAsStream("/maxmind/GeoLite2-Country.mmdb")) {

            databaseReader = new DatabaseReader.Builder(is).build();
        }
    }

    public CountryEntity getCountry(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            CountryResponse response = databaseReader.country(address);
            String countryCode = response.getCountry().getIsoCode();

            return countryRepository.findById(countryCode)
                    .orElse(null);
        } catch (Exception e) {
            log.error("error ",e);
            return null;
        }
    }
}