package com.retailmanagement.modules.erp.catalog.service;

import com.retailmanagement.modules.erp.catalog.entity.Brand;
import com.retailmanagement.modules.erp.catalog.entity.Category;
import com.retailmanagement.modules.erp.catalog.entity.TaxGroup;
import com.retailmanagement.modules.erp.catalog.entity.Uom;
import com.retailmanagement.modules.erp.catalog.repository.BrandRepository;
import com.retailmanagement.modules.erp.catalog.repository.CategoryRepository;
import com.retailmanagement.modules.erp.catalog.repository.TaxGroupRepository;
import com.retailmanagement.modules.erp.catalog.repository.UomRepository;
import com.retailmanagement.modules.erp.common.security.ErpAccessGuard;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogReferenceService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final UomRepository uomRepository;
    private final TaxGroupRepository taxGroupRepository;
    private final ErpAccessGuard accessGuard;

    public List<Category> searchCategories(Long organizationId, String query) {
        accessGuard.assertOrganizationAccess(organizationId);
        String q = normalize(query);
        return q == null
                ? categoryRepository.findTop30ByOrganizationIdAndIsActiveTrueOrderByNameAsc(organizationId)
                : categoryRepository.findTop30ByOrganizationIdAndIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(organizationId, q);
    }

    public List<Brand> searchBrands(Long organizationId, String query) {
        accessGuard.assertOrganizationAccess(organizationId);
        String q = normalize(query);
        return q == null
                ? brandRepository.findTop30ByOrganizationIdAndIsActiveTrueOrderByNameAsc(organizationId)
                : brandRepository.findTop30ByOrganizationIdAndIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(organizationId, q);
    }

    public List<Uom> searchUoms(String query) {
        String q = normalize(query);
        return q == null
                ? uomRepository.findTop30ByIsActiveTrueOrderByCodeAsc()
                : uomRepository.findTop30ByIsActiveTrueAndCodeContainingIgnoreCaseOrIsActiveTrueAndNameContainingIgnoreCaseOrderByCodeAsc(q, q);
    }

    public List<TaxGroup> searchTaxGroups(Long organizationId, String query) {
        accessGuard.assertOrganizationAccess(organizationId);
        String q = normalize(query);
        if (q == null) {
            return taxGroupRepository.findTop30ByOrganizationIdAndIsActiveTrueOrderByNameAsc(organizationId);
        }
        Map<Long, TaxGroup> deduped = new HashMap<>();
        taxGroupRepository.findTop30ByOrganizationIdAndIsActiveTrueAndCodeContainingIgnoreCaseOrderByNameAsc(organizationId, q)
                .forEach(group -> deduped.put(group.getId(), group));
        taxGroupRepository.findTop30ByOrganizationIdAndIsActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(organizationId, q)
                .forEach(group -> deduped.put(group.getId(), group));
        return deduped.values().stream()
                .sorted((left, right) -> left.getName().compareToIgnoreCase(right.getName()))
                .toList();
    }

    public Map<Long, Category> categoriesByIds(Collection<Long> ids) {
        return toMap(categoryRepository.findAllById(ids), Category::getId);
    }

    public Map<Long, Brand> brandsByIds(Collection<Long> ids) {
        return toMap(brandRepository.findAllById(ids), Brand::getId);
    }

    public Map<Long, Uom> uomsByIds(Collection<Long> ids) {
        return toMap(uomRepository.findAllById(ids), Uom::getId);
    }

    public Map<Long, TaxGroup> taxGroupsByIds(Collection<Long> ids) {
        return toMap(taxGroupRepository.findAllById(ids), TaxGroup::getId);
    }

    private String normalize(String query) {
        if (query == null) {
            return null;
        }
        String normalized = query.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private <T, K> Map<K, T> toMap(Iterable<T> values, java.util.function.Function<T, K> keyExtractor) {
        Map<K, T> map = new HashMap<>();
        for (T value : values) {
            map.put(keyExtractor.apply(value), value);
        }
        return map;
    }
}
