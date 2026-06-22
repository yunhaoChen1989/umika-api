package ca.umika.api.payment;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentIdempotencyKeyService {

    private final PaymentIdempotencyKeyRepository repository;
    private final PaymentIdempotencyKeyMapper mapper;

    public PaymentIdempotencyKeyService(PaymentIdempotencyKeyRepository repository, PaymentIdempotencyKeyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentIdempotencyKeyDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentIdempotencyKeyDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentIdempotencyKey not found: " + id));
    }

    public PaymentIdempotencyKeyDto create(PaymentIdempotencyKeyDto dto) {
        PaymentIdempotencyKeyEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public PaymentIdempotencyKeyDto update(UUID id, PaymentIdempotencyKeyDto dto) {
        PaymentIdempotencyKeyEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentIdempotencyKey not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("PaymentIdempotencyKey not found: " + id);
        }
        repository.deleteById(id);
    }
}
