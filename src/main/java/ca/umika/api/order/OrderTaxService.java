package ca.umika.api.order;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderTaxService {

    private final OrderTaxRepository repository;
    private final OrderTaxMapper mapper;

    public OrderTaxService(OrderTaxRepository repository, OrderTaxMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<OrderTaxDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public OrderTaxDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("OrderTax not found: " + id));
    }

    public OrderTaxDto create(OrderTaxDto dto) {
        OrderTaxEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public OrderTaxDto update(UUID id, OrderTaxDto dto) {
        OrderTaxEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderTax not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("OrderTax not found: " + id);
        }
        repository.deleteById(id);
    }
}
