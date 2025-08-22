package com.example.demo.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class InMemoryPaperService {
	
	public static record PaperEntity(
			Long id,
			String title,
			String authors,
			Integer year,
			String url,
			long createdAt
	) {}
	
	private final Map<Long, PaperEntity> db = new ConcurrentHashMap<>();
	private final AtomicLong seq = new AtomicLong(1);
	
	public PaperEntity create(String title, String authors, Integer year, String url) {
		long id = seq.getAndIncrement();
		var e = new PaperEntity(id, title, authors, year, url, System.currentTimeMillis());
		db.put(id,  e);
		return e;
	}
	
	public Optional<PaperEntity> findById(Long id){
		return Optional.ofNullable(db.get(id));
	}
	
	public long count() {
		return db.size();
	}
	
	public List<PaperEntity> findAll(int page, int size){
		var sorted = db.values().stream()
				.sorted(Comparator.comparing(PaperEntity::id).reversed())
				.collect(Collectors.toList());
		
		int from = Math.max(0,  page * size);
		int to = Math.min(sorted.size(), from + size);
		if(from >= sorted.size()) return List.of();
		return sorted.subList(from, to);
	}
	
	public List<PaperEntity> findAll(int page, int size, String q){
		var stream = db.values().stream();
		if (q != null && !q.isBlank()) {
			var needle = q.toLowerCase();
			stream = stream.filter(e ->
					(e.title() != null && e.title().toLowerCase().contains(needle)) ||
					(e.authors() != null && e.authors().toLowerCase().contains(needle))
			);
		}
		var sorted = stream
				.sorted(Comparator.comparing(PaperEntity::id).reversed())
				.collect(Collectors.toList());
		
		int from = Math.max(0,  page * size);
		int to = Math.min(sorted.size(), from + size);
		if(from >= sorted.size()) return List.of();
		return sorted.subList(from, to);
	}
	
	public void delete(Long id) {
		db.remove(id);
	}
	
	public Optional<PaperEntity> update(Long id, String title, String authors, Integer year, String url){
		return Optional.ofNullable(db.computeIfPresent(id, (k, old) ->
				new PaperEntity(
						old.id(),
						title != null ? title : old.title(),
						authors != null ? authors : old.authors(),
						year != null ? year : old.year(),
						url != null ? url : old.url(),
						old.createdAt()
				)));
	}
	
	public long countFiltered(String q) {
		if (q == null || q.isBlank()) return db.size();
		var needle = q.toLowerCase();
		return db.values().stream()
				.filter(e -> 
					(e.title() != null && e.title().toLowerCase().contains(needle)) ||
					(e.authors() != null && e.authors().toLowerCase().contains(needle)))
				.count();
	}
}









































