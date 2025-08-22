package com.example.demo.service;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.demo.domain.Paper;
import com.example.demo.repository.PaperRepository;
import com.example.demo.repository.TagRepository;

@Service
public class PaperJpaService implements PaperService {

    private final PaperRepository repo;
    private final TagRepository tagRepo;
    public PaperJpaService(PaperRepository repo, TagRepository tagRepo) { 
    	this.repo = repo;
    	this.tagRepo = tagRepo;
    }

    private static PaperService.PaperRow row(Paper p) {
    	var tags = p.getTags() == null ? java.util.List.<String>of()
                : p.getTags().stream().map(t -> t.getName()).sorted().toList();
        long created = p.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        String st = (p.getStatus() == null) ? "UNREAD" : p.getStatus().name();
        return new PaperRow(p.getId(), p.getTitle(), p.getAuthors(), p.getPubYear(), p.getUrl(), created, st, tags);
    }

    @Override
    public PaperRow create(String title, String authors, Integer year, String url) {
        var p = Paper.builder().title(title).authors(authors).pubYear(year).url(url).status(Paper.Status.UNREAD).build();
        return row(repo.save(p));
    }

    @Override
    public Optional<PaperRow> findById(Long id) {
        return repo.findById(id).map(PaperJpaService::row);
    }

    @Override
    public long count() { return repo.count(); }

    @Override
    public List<PaperRow> findAll(int page, int size) {
        var pr = PageRequest.of(page, size, Sort.by("id").descending());
        return repo.findAll(pr).map(PaperJpaService::row).getContent();
    }

    @Override
    public List<PaperRow> findAll(int page, int size, String q, String status, java.util.List<String> tags) {
        var pr = PageRequest.of(page, size, Sort.by("id").descending());
        com.example.demo.domain.Paper.Status st = null;
        if (status != null && !status.isBlank()) st = com.example.demo.domain.Paper.Status.valueOf(status.toUpperCase());
        List<String> tagList = (tags == null || tags.isEmpty()) ? null
                : tags.stream().filter(s -> s != null && !s.isBlank())
                       .map(String::toLowerCase).distinct().toList();
        return repo.search(q, st, tagList, pr).map(PaperJpaService::row).getContent();
    }

    @Override
    public long countFiltered(String q, String status, java.util.List<String> tags) {
    	com.example.demo.domain.Paper.Status st = null;
        if (status != null && !status.isBlank()) st = com.example.demo.domain.Paper.Status.valueOf(status.toUpperCase());
        List<String> tagList = (tags == null || tags.isEmpty()) ? null
                : tags.stream().filter(s -> s != null && !s.isBlank())
                       .map(String::toLowerCase).distinct().toList();
        return repo.countSearch(q, st, tagList);
    }

    @Override
    public void delete(Long id) { repo.deleteById(id); }

    @Override
    public Optional<PaperRow> update(Long id, String title, String authors, Integer year, String url) {
        return repo.findById(id).map(old -> {
            if (title   != null) old.setTitle(title);
            if (authors != null) old.setAuthors(authors);
            if (year    != null) old.setPubYear(year);
            if (url     != null) old.setUrl(url);
            return row(repo.save(old));
        });
    }
    
    public Optional<PaperRow> setStatus(Long id, String status){
    	return repo.findById(id).map(old -> {
    		if (status  != null) old.setStatus(com.example.demo.domain.Paper.Status.valueOf(status));
    		return row(repo.save(old));
    	});
    }
    
    @Override
    public Optional<PaperRow> addTag(Long paperId, String tagName) {
        if (tagName == null || tagName.isBlank()) return Optional.empty();
        String norm = tagName.trim().toLowerCase();
        return repo.findById(paperId).map(p -> {
            var tag = tagRepo.findByName(norm).orElseGet(() -> tagRepo.save(
                    com.example.demo.domain.Tag.builder().name(norm).build()));
            p.getTags().add(tag);
            return row(repo.save(p));
        });
    }

    @Override
    public Optional<PaperRow> removeTag(Long paperId, String tagName) {
        if (tagName == null || tagName.isBlank()) return Optional.empty();
        String norm = tagName.trim().toLowerCase();
        return repo.findById(paperId).map(p -> {
            tagRepo.findByName(norm).ifPresent(t -> p.getTags().remove(t));
            return row(repo.save(p));
        });
    }
}
