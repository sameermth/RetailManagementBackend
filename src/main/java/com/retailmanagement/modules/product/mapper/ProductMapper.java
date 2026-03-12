package com.retailmanagement.modules.product.mapper;

import com.retailmanagement.modules.product.dto.request.ProductRequest;
import com.retailmanagement.modules.product.dto.response.ProductListResponse;
import com.retailmanagement.modules.product.dto.response.ProductResponse;
import com.retailmanagement.modules.product.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {CategoryMapper.class, BrandMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "category", ignore = true)
    @Mapping(target = "brand", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "variants", ignore = true)
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "stockQuantity", ignore = true)
    @Mapping(target = "stockStatus", ignore = true)
    @Mapping(target = "primaryImageUrl", ignore = true)
    ProductListResponse toListResponse(Product product);

    List<ProductResponse> toResponseList(List<Product> products);
    List<ProductListResponse> toListResponseList(List<Product> products);
}