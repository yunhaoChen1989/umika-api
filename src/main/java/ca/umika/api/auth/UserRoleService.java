package ca.umika.api.auth;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserRoleService {

    private final UserRoleRepository repository;
    private final UserRoleMapper mapper;

    public UserRoleService(UserRoleRepository repository, UserRoleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<UserRoleDto> findAll(Pageable pageable) {
        Pageable effectivePageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                mapSort(pageable.getSort())
        );
        return repository.findAll(effectivePageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserRoleDto findById(UUID userId, UUID roleId) {
        UserRoleId id = new UserRoleId(userId, roleId);
        return repository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole not found"));
    }

    public UserRoleDto create(UserRoleDto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    public void delete(UUID userId, UUID roleId) {
        UserRoleId id = new UserRoleId(userId, roleId);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("UserRole not found");
        }
        repository.deleteById(id);
    }

    private Sort mapSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return Sort.by(Sort.Order.asc("id.userId"), Sort.Order.asc("id.roleId"));
        }

        List<Sort.Order> mappedOrders = sort.stream()
                .map(order -> switch (order.getProperty()) {
                    case "userId" -> new Sort.Order(order.getDirection(), "id.userId");
                    case "roleId" -> new Sort.Order(order.getDirection(), "id.roleId");
                    case "id.userId", "id.roleId" -> order;
                    default -> order;
                })
                .toList();
        return Sort.by(mappedOrders);
    }
}
