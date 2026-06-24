package ca.umika.api.payment;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PaymentWebhookLogService {

    private final PaymentWebhookLogRepository repository;
    private final PaymentWebhookLogMapper mapper;

    public PaymentWebhookLogService(PaymentWebhookLogRepository repository, PaymentWebhookLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<PaymentWebhookLogDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public PaymentWebhookLogDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentWebhookLog not found: " + id));
    }

    public PaymentWebhookLogDto create(PaymentWebhookLogDto dto) {
        PaymentWebhookLogEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public PaymentWebhookLogDto update(UUID id, PaymentWebhookLogDto dto) {
        PaymentWebhookLogEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentWebhookLog not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("PaymentWebhookLog not found: " + id);
        }
        repository.deleteById(id);
    }
}
