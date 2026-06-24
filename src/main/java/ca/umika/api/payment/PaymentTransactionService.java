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
public class PaymentTransactionService {

    private final PaymentTransactionRepository repository;
    private final PaymentTransactionMapper mapper;

    public PaymentTransactionService(PaymentTransactionRepository repository, PaymentTransactionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<PaymentTransactionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public PaymentTransactionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction not found: " + id));
    }

    public PaymentTransactionDto create(PaymentTransactionDto dto) {
        PaymentTransactionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public PaymentTransactionDto update(UUID id, PaymentTransactionDto dto) {
        PaymentTransactionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("PaymentTransaction not found: " + id);
        }
        repository.deleteById(id);
    }
}
