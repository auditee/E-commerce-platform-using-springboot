/**
 * Repository Package
 * ==================
 * This package contains repository interfaces.
 * Repositories talk directly to the database.
 *
 * Spring Data JPA is magical here — you just write an interface
 * (not even a full class!) and Spring automatically provides
 * methods like:
 *   .save(entity)     → INSERT into the database
 *   .findById(id)     → SELECT by primary key
 *   .findAll()        → SELECT all rows
 *   .deleteById(id)   → DELETE by primary key
 *
 * You don't write a single line of SQL!
 */
package com.ecommerce.repository;
