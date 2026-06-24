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
public class OrderStatusHistoryService {

    private final OrderStatusHistoryRepository repository;
    private final OrderStatusHistoryMapper mapper;

    public OrderStatusHistoryService(OrderStatusHistoryRepository repository, OrderStatusHistoryMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<OrderStatusHistoryDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public OrderStatusHistoryDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("OrderStatusHistory not found: " + id));
    }

    public OrderStatusHistoryDto create(OrderStatusHistoryDto dto) {
        OrderStatusHistoryEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public OrderStatusHistoryDto update(UUID id, OrderStatusHistoryDto dto) {
        OrderStatusHistoryEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderStatusHistory not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("OrderStatusHistory not found: " + id);
        }
        repository.deleteById(id);
    }
}
