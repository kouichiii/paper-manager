package com.example.demo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.domain.Paper;

public interface PaperRepository extends JpaRepository<Paper, Long> {
	
	@Query("""
	  select distinct p from Paper p
	  left join p.tags t
	  where (:q is null or :q = ''
	         or lower(p.title) like lower(concat('%', :q, '%'))
	         or lower(p.authors) like lower(concat('%', :q, '%')))
	    and (:st is null or p.status = :st)
	    and (:tags is null or t.name in :tags)
	""")
	Page<Paper> search(
	  @Param("q") String q,
	  @Param("st") com.example.demo.domain.Paper.Status st,
	  @Param("tags") java.util.List<String> tags,
	  org.springframework.data.domain.Pageable pageable
	);

	@Query("""
	  select count(distinct p) from Paper p
	  left join p.tags t
	  where (:q is null or :q = ''
	         or lower(p.title) like lower(concat('%', :q, '%'))
	         or lower(p.authors) like lower(concat('%', :q, '%')))
	    and (:st is null or p.status = :st)
	    and (:tags is null or t.name in :tags)
	""")
	long countSearch(@Param("q") String q,
	                 @Param("st") com.example.demo.domain.Paper.Status st,
	                 @Param("tags") java.util.List<String> tags);
}
