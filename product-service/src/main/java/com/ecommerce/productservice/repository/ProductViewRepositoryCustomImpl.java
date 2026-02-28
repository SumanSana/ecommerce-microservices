package com.ecommerce.productservice.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.ecommerce.productservice.entity.ProductView;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class ProductViewRepositoryCustomImpl implements ProductViewRepositoryCustom {

	private final MongoTemplate mongoTemplate;

	@Override
	public Page<ProductView> findByFilters(String brand, String category, BigDecimal minPrice, BigDecimal maxPrice,
			Pageable pageable) {

		Query query = new Query().with(pageable);
		List<Criteria> criteriaList = new ArrayList<>();

		if (brand != null && !brand.isEmpty()) {
			criteriaList.add(Criteria.where("brandName").is(brand));
		}
		if (category != null && !category.isEmpty()) {
			criteriaList.add(Criteria.where("categoryName").is(category));
		}

		if (minPrice != null && maxPrice != null) {
			criteriaList.add(Criteria.where("price").gte(minPrice).lte(maxPrice));
		} else if (minPrice != null) {
			criteriaList.add(Criteria.where("price").gte(minPrice));
		} else if (maxPrice != null) {
			criteriaList.add(Criteria.where("price").lte(maxPrice));
		}

		if (!criteriaList.isEmpty()) {
			query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
		}

		List<ProductView> products = mongoTemplate.find(query, ProductView.class);

		return PageableExecutionUtils.getPage(products, pageable,
				() -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ProductView.class));
	}
}
