package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;


public interface UserRepository extends CrudRepository<User, Integer>{
	@Query ("select u from User u where u.email=:email")
	User findByEmail(String email);
}
