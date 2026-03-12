package com.retailmanagement.modules.product.service.impl;

import com.retailmanagement.common.exceptions.BusinessException;
import com.retailmanagement.common.exceptions.ResourceNotFoundException;
import com.retailmanagement.modules.product.dto.request.BrandRequest;
import com.retailmanagement.modules.product.dto.response.BrandResponse;
import com.retailmanagement.modules.product.mapper.BrandMapper;
import com.retailmanagement.modules.product.model.Brand;
import com.retailmanagement.modules.product.repository.BrandRepository;
import com.retailmanagement.modules.product.service.BrandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    public BrandResponse createBrand(BrandRequest request) {
        log.info("Creating new brand with name: {}", request.getName());

        if (brandRepository.existsByName(request.getName())) {
            throw new BusinessException("Brand with name " + request.getName() + " already exists");
        }

        Brand brand = brandMapper.toEntity(request);
        Brand savedBrand = brandRepository.save(brand);
        log.info("Brand created successfully with ID: {}", savedBrand.getId());

        return brandMapper.toResponse(savedBrand);
    }

    @Override
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        log.info("Updating brand with ID: {}", id);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        // Check name uniqueness if changed
        if (!brand.getName().equals(request.getName()) &&
                brandRepository.existsByName(request.getName())) {
            throw new BusinessException("Brand with name " + request.getName() + " already exists");
        }

        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setWebsite(request.getWebsite());
        brand.setIsActive(request.getIsActive());

        Brand updatedBrand = brandRepository.save(brand);
        log.info("Brand updated successfully with ID: {}", updatedBrand.getId());

        return brandMapper.toResponse(updatedBrand);
    }

    @Override
    public BrandResponse getBrandById(Long id) {
        log.debug("Fetching brand with ID: {}", id);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        return brandMapper.toResponse(brand);
    }

    @Override
    public List<BrandResponse> getAllBrands() {
        log.debug("Fetching all brands");

        return brandRepository.findAll().stream()
                .map(brandMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteBrand(Long id) {
        log.info("Deleting brand with ID: {}", id);

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        // Check if brand has products
        if (brand.getProducts() != null && !brand.getProducts().isEmpty()) {
            throw new BusinessException("Cannot delete brand with associated products");
        }

        brandRepository.delete(brand);
        log.info("Brand deleted successfully with ID: {}", id);
    }

    @Override
    public boolean isBrandNameUnique(String name) {
        return !brandRepository.existsByName(name);
    }
}