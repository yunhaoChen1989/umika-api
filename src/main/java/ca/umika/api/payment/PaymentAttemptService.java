package ca.umika.api.payment;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentAttemptService {

    private final PaymentAttemptRepository repository;
    private final PaymentAttemptMapper mapper;

    public PaymentAttemptService(PaymentAttemptRepository repository, PaymentAttemptMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<PaymentAttemptDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentAttemptDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentAttempt not found: " + id));
    }

    public PaymentAttemptDto create(PaymentAttemptDto dto) {
        PaymentAttemptEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public PaymentAttemptDto update(UUID id, PaymentAttemptDto dto) {
        PaymentAttemptEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentAttempt not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("PaymentAttempt not found: " + id);
        }
        repository.deleteById(id);
    }
}
