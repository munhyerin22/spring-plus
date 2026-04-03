package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;

import java.util.Optional;

public interface TodoCustomRepository {
    // 1번 할 일을 조회할 때 작성자(User) 정보까지 한 번에(Fetch Join) 가져오는 기능을 만들겠어!
    Optional<Todo> findByIdWithUser(Long todoId);
}
