package com.paellasoft.CRUD.repository;

import com.paellasoft.CRUD.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IUserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

}
