package ca.umika.api.cart;

import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.menu.LocationMenuOverrideEntity;
import ca.umika.api.menu.LocationMenuOverrideRepository;
import ca.umika.api.menu.MenuCategoryEntity;
import ca.umika.api.menu.MenuCategoryRepository;
import ca.umika.api.menu.MenuItemEntity;
import ca.umika.api.menu.MenuItemImageEntity;
import ca.umika.api.menu.MenuItemImageRepository;
import ca.umika.api.menu.MenuItemOptionEntity;
import ca.umika.api.menu.MenuItemOptionRepository;
import ca.umika.api.menu.MenuItemRepository;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private static final String ACTIVE = "ACTIVE";
    private static final String CATEGORY = "CATEGORY";
    private static final String ITEM = "ITEM";

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemImageRepository menuItemImageRepository;
    private final MenuItemOptionRepository menuItemOptionRepository;
    private final LocationMenuOverrideRepository overrideRepository;
    private final ObjectMapper objectMapper;

    public CartService(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            LocationRepository locationRepository,
            MenuCategoryRepository categoryRepository,
            MenuItemRepository menuItemRepository,
            MenuItemImageRepository menuItemImageRepository,
            MenuItemOptionRepository menuItemOptionRepository,
            LocationMenuOverrideRepository overrideRepository,
            ObjectMapper objectMapper
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.menuItemImageRepository = menuItemImageRepository;
        this.menuItemOptionRepository = menuItemOptionRepository;
        this.overrideRepository = overrideRepository;
        this.objectMapper = objectMapper;
    }

    public CartResponse getOrCreateActiveCart(Authentication authentication, CartCreateRequest request) {
        UUID locationId = requireLocationId(request.locationId());
        ensureLocationExists(locationId);
        CartOwner owner = resolveOwner(authentication, request.sessionId(), true);

        List<CartEntity> activeCarts = owner.userId() != null
                ? cartRepository.findByUserIdAndLocationIdAndStatusOrderByCreatedAtDesc(owner.userId(), locationId, ACTIVE)
                : cartRepository.findBySessionIdAndLocationIdAndStatusOrderByCreatedAtDesc(owner.sessionId(), locationId, ACTIVE);
        CartEntity cart = resolveSingleActiveCart(activeCarts).orElseGet(() -> createCart(owner, locationId));

        if (owner.userId() != null && request.sessionId() != null && !request.sessionId().isBlank()) {
            cart = mergeGuestCartIntoUserCart(cart, owner.userId(), request.sessionId().trim(), locationId);
        }

        return toResponse(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(Authentication authentication, UUID id, String sessionId) {
        CartEntity cart = findCart(id);
        assertOwner(authentication, sessionId, cart);
        return toResponse(cart);
    }

    public CartResponse addItem(Authentication authentication, UUID cartId, CartAddItemRequest request, String sessionId) {
        CartEntity cart = findCart(cartId);
        assertOwner(authentication, sessionId, cart);
        assertActive(cart);

        int quantity = normalizeQuantity(request.quantity());
        ResolvedCartItem resolved = resolveMenuItem(cart.getLocationId(), request.menuItemId(), request.optionIds(), request.note());

        for (CartItemEntity existing : cartItemRepository.findByCart_Id(cart.getId())) {
            if (existing.getMenuItemId().equals(request.menuItemId()) && sameOptions(existing.getOptions(), resolved.optionsJson())) {
                existing.setQuantity(existing.getQuantity() + quantity);
                cartItemRepository.save(existing);
                recalculateSubtotal(cart);
                return toResponse(cart);
            }
        }

        CartItemEntity item = new CartItemEntity();
        item.setCart(cart);
        item.setMenuItemId(request.menuItemId());
        item.setItemName(resolved.itemName());
        item.setImageUrl(resolved.imageUrl());
        item.setQuantity(quantity);
        item.setUnitPrice(resolved.unitPrice());
        item.setOptions(resolved.optionsJson());
        cartItemRepository.save(item);
        recalculateSubtotal(cart);
        return toResponse(cart);
    }

    public CartResponse updateItem(Authentication authentication, UUID cartId, UUID itemId, CartUpdateItemRequest request, String sessionId) {
        CartEntity cart = findCart(cartId);
        assertOwner(authentication, sessionId, cart);
        assertActive(cart);
        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
        if (!item.getCart().getId().equals(cartId)) {
            throw new ResourceNotFoundException("Cart item not found: " + itemId);
        }

        int quantity = normalizeQuantity(request.quantity());
        item.setQuantity(quantity);
        cartItemRepository.save(item);
        recalculateSubtotal(cart);
        return toResponse(cart);
    }

    public CartResponse deleteItem(Authentication authentication, UUID cartId, UUID itemId, String sessionId) {
        CartEntity cart = findCart(cartId);
        assertOwner(authentication, sessionId, cart);
        assertActive(cart);
        CartItemEntity item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found: " + itemId));
        if (!item.getCart().getId().equals(cartId)) {
            throw new ResourceNotFoundException("Cart item not found: " + itemId);
        }

        cartItemRepository.delete(item);
        recalculateSubtotal(cart);
        return toResponse(cart);
    }

    public void abandonCart(Authentication authentication, UUID cartId, String sessionId) {
        CartEntity cart = findCart(cartId);
        assertOwner(authentication, sessionId, cart);
        cart.setStatus("ABANDONED");
        cartRepository.save(cart);
    }

    private CartEntity createCart(CartOwner owner, UUID locationId) {
        CartEntity cart = new CartEntity();
        cart.setUserId(owner.userId());
        cart.setSessionId(owner.sessionId());
        cart.setLocationId(locationId);
        cart.setStatus(ACTIVE);
        cart.setSubtotal(BigDecimal.ZERO);
        return cartRepository.save(cart);
    }

    private Optional<CartEntity> resolveSingleActiveCart(List<CartEntity> activeCarts) {
        if (activeCarts == null || activeCarts.isEmpty()) {
            return Optional.empty();
        }

        CartEntity keeper = activeCarts.get(0);
        if (activeCarts.size() == 1) {
            return Optional.of(keeper);
        }

        log.warn("duplicate active carts found ownerUserId={} ownerSessionId={} locationId={} activeCount={} keeperCartId={}",
                keeper.getUserId(), keeper.getSessionId(), keeper.getLocationId(), activeCarts.size(), keeper.getId());
        for (int i = 1; i < activeCarts.size(); i++) {
            CartEntity duplicate = activeCarts.get(i);
            cartItemRepository.findByCart_Id(duplicate.getId()).forEach(item -> {
                item.setCart(keeper);
                cartItemRepository.save(item);
            });
            duplicate.setStatus("ABANDONED");
            cartRepository.save(duplicate);
        }
        recalculateSubtotal(keeper);
        return Optional.of(keeper);
    }

    private CartEntity mergeGuestCartIntoUserCart(CartEntity userCart, UUID userId, String sessionId, UUID locationId) {
        List<CartEntity> guestCarts = cartRepository.findBySessionIdAndLocationIdAndStatusOrderByCreatedAtDesc(sessionId, locationId, ACTIVE).stream()
                .filter(cart -> cart.getUserId() == null || !cart.getUserId().equals(userId))
                .toList();

        if (guestCarts.isEmpty()) {
            return userCart;
        }

        for (CartEntity guestCart : guestCarts) {
            mergeCartItems(guestCart, userCart);
            guestCart.setStatus("ABANDONED");
            cartRepository.save(guestCart);
        }

        recalculateSubtotal(userCart);
        return userCart;
    }

    private void mergeCartItems(CartEntity sourceCart, CartEntity targetCart) {
        List<CartItemEntity> targetItems = new ArrayList<>(cartItemRepository.findByCart_Id(targetCart.getId()));

        for (CartItemEntity sourceItem : cartItemRepository.findByCart_Id(sourceCart.getId())) {
            Optional<CartItemEntity> matchingTarget = targetItems.stream()
                    .filter(targetItem -> targetItem.getMenuItemId().equals(sourceItem.getMenuItemId()) && sameOptions(targetItem.getOptions(), sourceItem.getOptions()))
                    .findFirst();

            if (matchingTarget.isPresent()) {
                CartItemEntity targetItem = matchingTarget.get();
                targetItem.setQuantity(targetItem.getQuantity() + sourceItem.getQuantity());
                cartItemRepository.save(targetItem);
                cartItemRepository.delete(sourceItem);
            } else {
                sourceItem.setCart(targetCart);
                cartItemRepository.save(sourceItem);
                targetItems.add(sourceItem);
            }
        }
    }

    private ResolvedCartItem resolveMenuItem(UUID locationId, UUID menuItemId, List<UUID> optionIds, String note) {
        if (menuItemId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "menuItemId is required");
        }

        MenuItemEntity item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + menuItemId));
        if (item.getLocationId() != null && !item.getLocationId().equals(locationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item does not belong to this cart location");
        }
        if (Boolean.TRUE.equals(item.getIsDeleted()) || Boolean.FALSE.equals(item.getIsActive()) || Boolean.FALSE.equals(item.getIsAvailable())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item is not available");
        }

        MenuCategoryEntity category = categoryRepository.findById(item.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found: " + item.getCategoryId()));
        if (Boolean.TRUE.equals(category.getIsDeleted()) || Boolean.FALSE.equals(category.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu category is not available");
        }

        LocationMenuOverrideEntity categoryOverride = overrideRepository
                .findByLocationIdAndTargetTypeAndTargetId(locationId, CATEGORY, category.getId())
                .orElse(null);
        if (categoryOverride != null && Boolean.FALSE.equals(categoryOverride.getIsVisible())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu category is not available at this location");
        }

        LocationMenuOverrideEntity itemOverride = overrideRepository
                .findByLocationIdAndTargetTypeAndTargetId(locationId, ITEM, item.getId())
                .orElse(null);
        if (itemOverride != null && Boolean.FALSE.equals(itemOverride.getIsVisible())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item is not available at this location");
        }

        String itemName = pickString(item.getName(), itemOverride == null ? null : itemOverride.getCustomName());
        BigDecimal basePrice = pickBigDecimal(item.getPrice(), itemOverride == null ? null : itemOverride.getCustomPrice());
        String imageUrl = pickString(resolveGlobalImageUrl(item.getId()), itemOverride == null ? null : itemOverride.getCustomImageUrl());
        List<OptionSnapshot> optionSnapshots = resolveOptions(item.getId(), optionIds);
        BigDecimal optionTotal = optionSnapshots.stream()
                .map(OptionSnapshot::priceModifier)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String optionsJson = writeOptionsJson(optionSnapshots, note);

        return new ResolvedCartItem(itemName, imageUrl, basePrice.add(optionTotal), optionsJson);
    }

    private List<OptionSnapshot> resolveOptions(UUID menuItemId, List<UUID> optionIds) {
        if (optionIds == null || optionIds.isEmpty()) {
            return List.of();
        }
        List<MenuItemOptionEntity> options = menuItemOptionRepository.findByIdInAndItemIdAndIsActiveTrue(optionIds, menuItemId);
        if (options.size() != optionIds.stream().distinct().count()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid menu item option");
        }
        return options.stream()
                .sorted(Comparator.comparingInt(option -> option.getSortOrder() == null ? 0 : option.getSortOrder()))
                .map(option -> new OptionSnapshot(option.getId(), option.getName(), nullToZero(option.getPriceModifier())))
                .toList();
    }

    private String resolveGlobalImageUrl(UUID menuItemId) {
        return menuItemImageRepository.findByMenuItemIdIn(List.of(menuItemId)).stream()
                .sorted(Comparator
                        .comparing((MenuItemImageEntity image) -> !Boolean.TRUE.equals(image.getIsPrimary()))
                        .thenComparing(image -> image.getSortOrder() == null ? 0 : image.getSortOrder())
                        .thenComparing(MenuItemImageEntity::getId))
                .map(MenuItemImageEntity::getImageUrl)
                .findFirst()
                .orElse(null);
    }

    private void recalculateSubtotal(CartEntity cart) {
        BigDecimal subtotal = cartItemRepository.findByCart_Id(cart.getId()).stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setSubtotal(subtotal);
        cartRepository.save(cart);
    }

    private CartResponse toResponse(CartEntity cart) {
        List<CartItemResponse> items = cartItemRepository.findByCart_Id(cart.getId()).stream()
                .sorted(Comparator.comparing(CartItemEntity::getCreatedAt))
                .map(this::toItemResponse)
                .toList();
        return new CartResponse(
                cart.getId(),
                cart.getUserId(),
                cart.getSessionId(),
                cart.getLocationId(),
                cart.getStatus(),
                nullToZero(cart.getSubtotal()),
                items,
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }

    private CartItemResponse toItemResponse(CartItemEntity item) {
        BigDecimal unitPrice = nullToZero(item.getUnitPrice());
        return new CartItemResponse(
                item.getId(),
                item.getMenuItemId(),
                item.getItemName(),
                item.getImageUrl(),
                item.getQuantity(),
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())),
                item.getOptions()
        );
    }

    private CartEntity findCart(UUID id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + id));
    }

    private void assertOwner(Authentication authentication, String sessionId, CartEntity cart) {
        CartOwner owner = resolveOwner(authentication, sessionId, false);
        if (cart.getUserId() != null && cart.getUserId().equals(owner.userId())) {
            return;
        }
        if (cart.getSessionId() != null && cart.getSessionId().equals(owner.sessionId())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cart access denied");
    }

    private CartOwner resolveOwner(Authentication authentication, String sessionId, boolean allowNewGuest) {
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            UUID userId = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()))
                    .getId();
            return new CartOwner(userId, null);
        }

        if (sessionId != null && !sessionId.isBlank()) {
            return new CartOwner(null, sessionId.trim());
        }

        if (allowNewGuest) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId is required for guest cart");
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cart authentication required");
    }

    private void assertActive(CartEntity cart) {
        if (!ACTIVE.equalsIgnoreCase(cart.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is not active");
        }
    }

    private UUID requireLocationId(UUID locationId) {
        if (locationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId is required");
        }
        return locationId;
    }

    private void ensureLocationExists(UUID locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }

    private int normalizeQuantity(Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "quantity must be greater than 0");
        }
        return quantity;
    }

    private String writeOptionsJson(List<OptionSnapshot> options, String note) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("selectedOptions", options);
        if (note != null && !note.isBlank()) {
            payload.put("note", note.trim());
        }
        try {
        return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write cart options", e);
        }
    }

    private boolean sameOptions(String existingOptions, String requestedOptions) {
        try {
            JsonNode existing = objectMapper.readTree(existingOptions == null || existingOptions.isBlank() ? "{}" : existingOptions);
            JsonNode requested = objectMapper.readTree(requestedOptions == null || requestedOptions.isBlank() ? "{}" : requestedOptions);
            return existing.equals(requested);
        } catch (JsonProcessingException e) {
            return String.valueOf(existingOptions).equals(String.valueOf(requestedOptions));
        }
    }

    private String pickString(String baseValue, String overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }

    private BigDecimal pickBigDecimal(BigDecimal baseValue, BigDecimal overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record CartOwner(UUID userId, String sessionId) {
    }

    private record ResolvedCartItem(String itemName, String imageUrl, BigDecimal unitPrice, String optionsJson) {
    }

    private record OptionSnapshot(UUID id, String name, BigDecimal priceModifier) {
    }
}
