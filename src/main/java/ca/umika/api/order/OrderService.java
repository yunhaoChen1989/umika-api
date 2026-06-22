package ca.umika.api.order;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;

    public OrderService(OrderRepository repository, OrderMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    public OrderDto create(OrderDto dto) {
        OrderEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public OrderDto update(UUID id, OrderDto dto) {
        OrderEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Order not found: " + id);
        }
        repository.deleteById(id);
    }
}
