package com.example.styloflow.repository;

import com.example.styloflow.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Customers whose name, phone or tax id match {@code pattern}.
     *
     * @param pattern  lower-case LIKE pattern (e.g. {@code %ana%}); {@code null} returns every customer
     * @param pageable page and sort
     * @return the matching page
     */
    @Query("""
            SELECT c FROM Customer c
            WHERE :pattern IS NULL
               OR LOWER(c.name) LIKE :pattern
               OR LOWER(c.phone) LIKE :pattern
               OR LOWER(c.taxId) LIKE :pattern""")
    Page<Customer> search(@Param("pattern") String pattern, Pageable pageable);
}
