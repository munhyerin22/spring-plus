package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TodoCustomRepository {
    // 1번 할 일을 조회할 때 작성자(User) 정보까지 한 번에(Fetch Join) 가져오는 기능을 만들겠어!
    Optional<Todo> findByIdWithUser(Long todoId);

    Page<TodoSearchResponse> searchTodos(
            TodoSearchRequest request, Pageable pageable
    );
}
