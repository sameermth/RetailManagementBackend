package com.retailmanagement.modules.product.service;

import com.retailmanagement.modules.product.dto.request.BrandRequest;
import com.retailmanagement.modules.product.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse createBrand(BrandRequest request);

    BrandResponse updateBrand(Long id, BrandRequest request);

    BrandResponse getBrandById(Long id);

    List<BrandResponse> getAllBrands();

    void deleteBrand(Long id);

    boolean isBrandNameUnique(String name);
}