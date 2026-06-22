package ca.umika.api.order;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderDiscountService {

    private final OrderDiscountRepository repository;
    private final OrderDiscountMapper mapper;

    public OrderDiscountService(OrderDiscountRepository repository, OrderDiscountMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<OrderDiscountDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDiscountDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("OrderDiscount not found: " + id));
    }

    public OrderDiscountDto create(OrderDiscountDto dto) {
        OrderDiscountEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public OrderDiscountDto update(UUID id, OrderDiscountDto dto) {
        OrderDiscountEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderDiscount not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("OrderDiscount not found: " + id);
        }
        repository.deleteById(id);
    }
}
