package com.exchange.platform.repository;

import com.exchange.platform.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
	Page<Listing> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description, Pageable pageable);
	
	// 查詢指定擁有者的刊登
	Page<Listing> findByOwnerId(Long ownerId, Pageable pageable);
	
	// 查詢指定擁有者的刊登（含搜尋）
	Page<Listing> findByOwnerIdAndTitleContainingIgnoreCaseOrOwnerIdAndDescriptionContainingIgnoreCase(
		Long ownerId1, String title, Long ownerId2, String description, Pageable pageable);
	
	// 排除指定擁有者的刊登
	Page<Listing> findByOwnerIdNot(Long ownerId, Pageable pageable);
	
	// 排除指定擁有者的刊登（含搜尋）
	Page<Listing> findByOwnerIdNotAndTitleContainingIgnoreCaseOrOwnerIdNotAndDescriptionContainingIgnoreCase(
		Long ownerId1, String title, Long ownerId2, String description, Pageable pageable);
}
