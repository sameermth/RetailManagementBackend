package com.retailmanagement.modules.product.mapper;

import com.retailmanagement.modules.product.dto.request.BrandRequest;
import com.retailmanagement.modules.product.dto.response.BrandResponse;
import com.retailmanagement.modules.product.model.Brand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BrandMapper {

    @Mapping(target = "products", ignore = true)
    Brand toEntity(BrandRequest request);

    @Mapping(target = "productCount", expression = "java(brand.getProducts() != null ? brand.getProducts().size() : 0)")
    BrandResponse toResponse(Brand brand);

    List<BrandResponse> toResponseList(List<Brand> brands);
}