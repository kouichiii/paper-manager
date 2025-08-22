package com.example.demo.service;

import java.util.List;
import java.util.Optional;

public interface PaperService {
	
	record PaperRow(Long id, String title, String authors, Integer year, String url, long createdAt, String status, java.util.List<String> tags) {};
	
	PaperRow create(String title, String authors, Integer year, String url);
	Optional<PaperRow> findById(Long id);
	long count();
	List<PaperRow> findAll(int page, int size);
	List<PaperRow> findAll(int page, int size, String q, String status, java.util.List<String> tags);
	long countFiltered(String q, String status, java.util.List<String> tags);
	void delete(Long id);
	Optional<PaperRow> update(Long id, String title, String authors, Integer year, String url);
	Optional<PaperRow> setStatus(Long id, String status);
	java.util.Optional<PaperRow> addTag(Long paperId, String tagName);
	java.util.Optional<PaperRow> removeTag(Long paperId, String tagName);
}
