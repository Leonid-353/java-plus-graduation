package ru.yandex.practicum.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.yandex.practicum.model.User;

import java.util.Collection;
import java.util.List;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.id IN :ids")
    List<User> findUsersByIds(@Param("ids") Collection<Long> ids, Pageable pageable);

    List<User> findByIdIn(List<Long> userIds);
}
