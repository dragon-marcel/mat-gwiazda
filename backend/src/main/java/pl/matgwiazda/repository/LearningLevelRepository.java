package pl.matgwiazda.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.matgwiazda.domain.entity.LearningLevel;

@Repository
public interface LearningLevelRepository extends JpaRepository<LearningLevel, Short> {
}

