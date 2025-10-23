package pl.matgwiazda.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matgwiazda.domain.entity.Progress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, UUID> {

    // find existing progress for a user/task (optional)
    Optional<Progress> findByUserIdAndTaskId(UUID userId, UUID taskId);

    // Non-paged variants for endpoints that return all items
    List<Progress> findByUserId(UUID userId, Sort sort);

}
