package pl.matgwiazda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matgwiazda.domain.entity.Progress;

import java.util.UUID;

@Repository
public interface ProgressRepository extends JpaRepository<Progress, UUID> {
}

