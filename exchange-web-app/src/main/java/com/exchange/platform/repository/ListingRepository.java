package com.exchange.platform.repository;

import com.exchange.platform.entity.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
	// 搜尋卡片名稱、藝人名稱、團體名稱或描述
	Page<Listing> findByCardNameContainingIgnoreCaseOrArtistNameContainingIgnoreCaseOrGroupNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
		String cardName, String artistName, String groupName, String description, Pageable pageable);
	
	// 查詢指定使用者的刊登
	Page<Listing> findByUserId(Long userId, Pageable pageable);
	
	// 排除指定使用者的刊登
	Page<Listing> findByUserIdNot(Long userId, Pageable pageable);
}
