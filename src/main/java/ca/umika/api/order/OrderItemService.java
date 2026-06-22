package ca.umika.api.order;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderItemService {

    private final OrderItemRepository repository;
    private final OrderItemMapper mapper;

    public OrderItemService(OrderItemRepository repository, OrderItemMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<OrderItemDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderItemDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found: " + id));
    }

    public OrderItemDto create(OrderItemDto dto) {
        OrderItemEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public OrderItemDto update(UUID id, OrderItemDto dto) {
        OrderItemEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("OrderItem not found: " + id);
        }
        repository.deleteById(id);
    }
}
