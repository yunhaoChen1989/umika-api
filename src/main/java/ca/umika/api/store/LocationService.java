package ca.umika.api.store;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {

    private static final String LOCATION_CODE_PREFIX = "LOC";
    private static final int LOCATION_CODE_RANDOM_LENGTH = 5;
    private static final int MAX_LOCATION_CODE_ATTEMPTS = 20;
    private static final String LOCATION_CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final LocationRepository repository;
    private final LocationMapper mapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public LocationService(LocationRepository repository, LocationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<LocationDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public LocationDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + id));
    }

    @Transactional(readOnly = true)
    public LocationDto findByLocationCode(String locationCode) {
        return repository.findByLocationCodeIgnoreCase(normalizeLocationCode(locationCode))
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationCode));
    }

    @Transactional(readOnly = true)
    public LocationDto findCurrent(String locationCode) {
        if (locationCode != null && !locationCode.isBlank()) {
            return findByLocationCode(locationCode);
        }
        return repository.findFirstByOrderByCreatedAtAsc()
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("No locations found"));
    }

    public LocationDto create(LocationDto dto) {
        LocationEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        entity.setLocationCode(generateUniqueLocationCode());
        return mapper.toDto(repository.save(entity));
    }

    public LocationDto update(String locationCode, LocationDto dto) {
        LocationEntity entity = repository.findByLocationCodeIgnoreCase(normalizeLocationCode(locationCode))
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationCode));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(String locationCode) {
        LocationEntity entity = repository.findByLocationCodeIgnoreCase(normalizeLocationCode(locationCode))
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationCode));
        repository.delete(entity);
    }

    private String generateUniqueLocationCode() {
        for (int attempt = 0; attempt < MAX_LOCATION_CODE_ATTEMPTS; attempt++) {
            String code = LOCATION_CODE_PREFIX + randomBase36(LOCATION_CODE_RANDOM_LENGTH);
            if (!repository.existsByLocationCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Unable to generate a unique location code");
    }

    private String randomBase36(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(LOCATION_CODE_ALPHABET.charAt(secureRandom.nextInt(LOCATION_CODE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private String normalizeLocationCode(String locationCode) {
        return locationCode == null ? null : locationCode.trim().toUpperCase();
    }
}
