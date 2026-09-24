package ca.umika.api.menu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class MenuOptionAssignmentServiceTest {
    private static final UUID ITEM_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID OPTION_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final Authentication AUTH = UsernamePasswordAuthenticationToken.authenticated(
            "manager@umika.test", "n/a", List.of());

    private MenuItemRepository itemRepository;
    private MenuCategoryRepository menuCategoryRepository;
    private MenuOptionCategoryRepository categoryRepository;
    private MenuOptionRepository optionRepository;
    private MenuItemOptionCategoryAssignmentRepository assignmentRepository;
    private MenuItemOptionAssignmentSettingRepository itemSettingRepository;
    private MenuAccessService menuAccessService;
    private MenuOptionAssignmentService service;

    @BeforeEach
    void setUp() {
        itemRepository = mock(MenuItemRepository.class);
        menuCategoryRepository = mock(MenuCategoryRepository.class);
        categoryRepository = mock(MenuOptionCategoryRepository.class);
        optionRepository = mock(MenuOptionRepository.class);
        assignmentRepository = mock(MenuItemOptionCategoryAssignmentRepository.class);
        itemSettingRepository = mock(MenuItemOptionAssignmentSettingRepository.class);
        menuAccessService = mock(MenuAccessService.class);
        service = new MenuOptionAssignmentService(itemRepository, menuCategoryRepository, categoryRepository,
                optionRepository, assignmentRepository, itemSettingRepository, menuAccessService);

        MenuItemEntity item = new MenuItemEntity();
        item.setId(ITEM_ID);
        item.setCategoryId(CATEGORY_ID);
        item.setLocationId(null);
        when(itemRepository.findById(ITEM_ID)).thenReturn(Optional.of(item));
    }

    @Test
    void globalAssignmentAndOptionAreReturnedForLocation() {
        MenuOptionCategoryEntity category = category(1);
        MenuOptionEntity option = option();
        when(categoryRepository.findActiveAvailable(LOCATION_ID)).thenReturn(List.of(category));
        when(assignmentRepository.findByMenuCategoryIdInAndLocationIdIsNull(List.of(CATEGORY_ID)))
                .thenReturn(List.of(categoryAssignment(null, true)));
        when(assignmentRepository.findByMenuCategoryIdInAndLocationId(List.of(CATEGORY_ID), LOCATION_ID)).thenReturn(List.of());
        when(optionRepository.findByCategoryIdInAndIsActiveTrueOrderBySortOrderAsc(Set.of(CATEGORY_ID)))
                .thenReturn(List.of(option));

        List<MenuCatalogOptionGroupDto> groups = service.getGroups(ITEM_ID, LOCATION_ID);

        assertThat(groups).hasSize(1);
        assertThat(groups.getFirst().name()).isEqualTo("Extras");
        assertThat(groups.getFirst().options()).extracting(MenuCatalogOptionDto::name)
                .containsExactly("Cream cheese");
    }

    @Test
    void locationDisabledAssignmentOverridesGlobalAssignment() {
        when(categoryRepository.findActiveAvailable(LOCATION_ID)).thenReturn(List.of(category(0)));
        when(assignmentRepository.findByMenuCategoryIdInAndLocationIdIsNull(List.of(CATEGORY_ID)))
                .thenReturn(List.of(categoryAssignment(null, true)));
        when(assignmentRepository.findByMenuCategoryIdInAndLocationId(List.of(CATEGORY_ID), LOCATION_ID))
                .thenReturn(List.of(categoryAssignment(LOCATION_ID, false)));

        assertThat(service.getGroups(ITEM_ID, LOCATION_ID)).isEmpty();
    }

    @Test
    void selectionRulesAndAssignmentAreEnforced() {
        MenuOptionCategoryEntity category = category(1);
        category.setMaxSelect(1);
        MenuOptionEntity option = option();
        when(categoryRepository.findActiveAvailable(LOCATION_ID)).thenReturn(List.of(category));
        when(assignmentRepository.findByMenuCategoryIdInAndLocationIdIsNull(List.of(CATEGORY_ID)))
                .thenReturn(List.of(categoryAssignment(null, true)));
        when(assignmentRepository.findByMenuCategoryIdInAndLocationId(List.of(CATEGORY_ID), LOCATION_ID)).thenReturn(List.of());
        when(optionRepository.findByCategoryIdInAndIsActiveTrueOrderBySortOrderAsc(Set.of(CATEGORY_ID)))
                .thenReturn(List.of(option));

        assertThat(service.resolveSelections(ITEM_ID, LOCATION_ID, List.of(OPTION_ID)))
                .extracting(MenuOptionAssignmentService.SelectedMenuOption::priceModifier)
                .containsExactly(new BigDecimal("1.50"));
        assertThatThrownBy(() -> service.resolveSelections(ITEM_ID, LOCATION_ID, List.of()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Select at least 1");
        assertThatThrownBy(() -> service.resolveSelections(ITEM_ID, LOCATION_ID, List.of(UUID.randomUUID())))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void replacingLocationAssignmentsStoresDisabledOverrideForInheritedCategory() {
        MenuOptionCategoryEntity category = category(0);
        when(categoryRepository.findActiveAvailable(LOCATION_ID)).thenReturn(List.of(category));
        MenuCategoryEntity menuCategory = new MenuCategoryEntity();
        menuCategory.setId(CATEGORY_ID);
        menuCategory.setLocationId(null);
        when(menuCategoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(menuCategory));
        when(assignmentRepository.findByMenuCategoryIdAndLocationIdIsNull(CATEGORY_ID))
                .thenReturn(List.of(categoryAssignment(null, true)));
        when(assignmentRepository.findByMenuCategoryIdAndLocationId(CATEGORY_ID, LOCATION_ID)).thenReturn(List.of());

        service.replaceMenuCategoryAssignments(AUTH, CATEGORY_ID, LOCATION_ID,
                new MenuItemOptionAssignmentRequest(Set.of(), null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MenuItemOptionCategoryAssignmentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(assignmentRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).singleElement().satisfies(assignment -> {
            assertThat(assignment.getMenuCategoryId()).isEqualTo(CATEGORY_ID);
            assertThat(assignment.getItemId()).isNull();
            assertThat(assignment.getLocationId()).isEqualTo(LOCATION_ID);
            assertThat(assignment.getIsEnabled()).isFalse();
        });
    }

    private MenuOptionCategoryEntity category(int minSelect) {
        MenuOptionCategoryEntity category = new MenuOptionCategoryEntity();
        category.setId(CATEGORY_ID);
        category.setName("Extras");
        category.setIsActive(true);
        category.setIsRequired(minSelect > 0);
        category.setMinSelect(minSelect);
        category.setSortOrder(0);
        return category;
    }

    private MenuOptionEntity option() {
        MenuOptionEntity option = new MenuOptionEntity();
        option.setId(OPTION_ID);
        option.setCategoryId(CATEGORY_ID);
        option.setName("Cream cheese");
        option.setPriceModifier(new BigDecimal("1.50"));
        option.setSortOrder(0);
        option.setIsActive(true);
        return option;
    }

    private MenuItemOptionCategoryAssignmentEntity categoryAssignment(UUID locationId, boolean enabled) {
        MenuItemOptionCategoryAssignmentEntity assignment = new MenuItemOptionCategoryAssignmentEntity();
        assignment.setMenuCategoryId(CATEGORY_ID);
        assignment.setCategoryId(CATEGORY_ID);
        assignment.setLocationId(locationId);
        assignment.setIsEnabled(enabled);
        return assignment;
    }
}
