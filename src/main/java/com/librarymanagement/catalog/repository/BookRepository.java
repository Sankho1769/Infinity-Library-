package com.librarymanagement.catalog.repository;

import com.librarymanagement.catalog.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    long countByActive(boolean active);

    @Query("SELECT b FROM Book b WHERE b.active = true AND " +
           "(:query IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(b.isbn) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:genre IS NULL OR LOWER(b.genre) = LOWER(:genre))")
    Page<Book> searchBooks(@Param("query") String query, @Param("genre") String genre, Pageable pageable);
}
