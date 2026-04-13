package com.ecommerce.productservice.repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.ecommerce.productservice.dto.ProductSearchCriteria;
import com.ecommerce.productservice.entity.ProductView;
import com.mongodb.client.result.UpdateResult;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class ProductViewRepositoryCustomImpl implements ProductViewRepositoryCustom {
	private static final Logger log = LoggerFactory.getLogger(ProductViewRepositoryCustomImpl.class);

	private final MongoTemplate mongoTemplate;

	@Override
	public Page<ProductView> findByFilters(ProductSearchCriteria criteria, Pageable pageable) {
		Query query = new Query();
		List<Criteria> criteriaList = new ArrayList<>();

		// 1. Keyword Search (Matches name or description)
		if (StringUtils.hasText(criteria.getSearchTerm())) {
			String regex = ".*" + criteria.getSearchTerm().trim() + ".*";
			criteriaList.add(new Criteria().orOperator(Criteria.where("name").regex(regex, "i"),
					Criteria.where("description").regex(regex, "i")));
		}

		// 2. Exact Match Filters
		if (StringUtils.hasText(criteria.getBrandName())) {
			criteriaList.add(Criteria.where("brandName").is(criteria.getBrandName().trim()));
		}
		if (StringUtils.hasText(criteria.getCategoryName())) {
			criteriaList.add(Criteria.where("categoryName").is(criteria.getCategoryName().trim()));
		}

		// 3. Nested Variant Filtering (Price and Stock Status)
		// elemMatch ensures the criteria apply to the SAME variant object
		List<Criteria> variantSubCriteria = new ArrayList<>();

		if (criteria.getMinPrice() != null)
			variantSubCriteria.add(Criteria.where("price").gte(criteria.getMinPrice()));
		if (criteria.getMaxPrice() != null)
			variantSubCriteria.add(Criteria.where("price").lte(criteria.getMaxPrice()));
		if (Boolean.TRUE.equals(criteria.getInStockOnly()))
			variantSubCriteria.add(Criteria.where("inStock").is(true));

		if (!variantSubCriteria.isEmpty()) {
			Criteria variantCriteria = new Criteria().andOperator(variantSubCriteria.toArray(new Criteria[0]));
			criteriaList.add(Criteria.where("variants").elemMatch(variantCriteria));
		}

		// 4. Combine all criteria (If empty, MongoDB performs a find all)
		if (!criteriaList.isEmpty()) {
			query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
		}

		// 5. Apply Pagination and Execute
		Query countQuery = Query.of(query); // Clone query for count
		query.with(pageable);

		List<ProductView> products = mongoTemplate.find(query, ProductView.class);

		return PageableExecutionUtils.getPage(products, pageable,
				() -> mongoTemplate.count(countQuery, ProductView.class));
	}

	public void updateInventory(String skuId, Integer newQuantity) {
		// 1. Calculate the inStock status based on quantity
		boolean inStock = newQuantity > 0;

		// 2. Create the Query to find the document containing the specific SKU
		// "variants.skuId" searches inside the array
		Query query = new Query(Criteria.where("variants.skuId").is(skuId));

		// 3. Create the Update using the positional operator ($)
		// The '$' represents the index of the element that matched in the query above
		Update update = new Update().set("variants.$.availableQuantity", newQuantity).set("variants.$.inStock", inStock)
				.set("lastSyncedAt", Instant.now());

		// 4. Execute the update
		UpdateResult result = mongoTemplate.updateFirst(query, update, ProductView.class);

		if (result.getMatchedCount() == 0) {
			log.warn("Inventory update failed: No product found with SKU ID: {}", skuId);
		} else {
			log.info("Successfully updated inventory for SKU: {} to {}", skuId, newQuantity);
		}
	}
}