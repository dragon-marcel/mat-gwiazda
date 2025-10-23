package pl.matgwiazda.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matgwiazda.domain.entity.Task;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    // Paged query for level and active
    Page<Task> findByLevelAndIsActive(short level, boolean isActive, Pageable pageable);

    // Paged query for level only
    Page<Task> findByLevel(short level, Pageable pageable);

    // Paged query for level and createdBy
    Page<Task> findByLevelAndCreatedById(short level, UUID createdById, Pageable pageable);

    // Paged query for level, createdBy and isActive
    Page<Task> findByLevelAndCreatedByIdAndIsActive(short level, UUID createdById, boolean isActive, Pageable pageable);

    // Paged query for active/inactive tasks
    Page<Task> findByIsActive(boolean isActive, Pageable pageable);

    // Paged tasks created by a specific user
    Page<Task> findByCreatedById(UUID createdById, Pageable pageable);

    // Paged tasks created by a specific user and active flag
    Page<Task> findByCreatedByIdAndIsActive(UUID createdById, boolean isActive, Pageable pageable);

    // Find active task by id
    Optional<Task> findByIdAndIsActive(UUID id, boolean isActive);
}
