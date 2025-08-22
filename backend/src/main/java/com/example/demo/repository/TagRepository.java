package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.Tag;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
}
