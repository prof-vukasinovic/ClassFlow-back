package com.eidd.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eidd.model.ClassRoom;

@Repository
public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
	List<ClassRoom> findByOwner(String owner);

	Optional<ClassRoom> findByIdAndOwner(Long id, String owner);
}
