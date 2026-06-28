package ca.umika.api.menu;

import java.util.List;

public record MenuCatalogResponseDto(
        List<MenuCatalogCategoryDto> categories
) {
}
