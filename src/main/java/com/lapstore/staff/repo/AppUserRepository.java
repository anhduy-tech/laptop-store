package com.lapstore.staff.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.staff.domain.AppUser;

public interface  AppUserRepository extends JpaRepository<AppUser, Long>{
    
}
