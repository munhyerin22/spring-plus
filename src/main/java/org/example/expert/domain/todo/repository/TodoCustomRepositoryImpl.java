package org.example.expert.domain.todo.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.request.TodoSearchRequest;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

@RequiredArgsConstructor
public class TodoCustomRepositoryImpl implements TodoCustomRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        return Optional.ofNullable(
                queryFactory
                    .select(todo)
                    .leftJoin(todo.user).fetchJoin()
                    .where(todo.id.eq(todoId))
                    .fetchOne()
        );
    }

    @Override
    public Page<TodoSearchResponse> searchTodos(
            TodoSearchRequest request, Pageable pageable) {

        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(request.getTitle())){
            builder.and(todo.title.containsIgnoreCase(request.getTitle()));
        }
        if(request.getStartDate() != null){
            builder.and(todo.createdAt.goe(request.getStartDate()));
        }
        if(request.getEndDate() != null){
            builder.and(todo.createdAt.goe(request.getEndDate()));
        }
        if (StringUtils.hasText(request.getManagerNickname())){
            builder.and(todo.user.nickname.containsIgnoreCase(request.getManagerNickname()));
        }

        List<TodoSearchResponse> content = queryFactory
                .select(Projections.constructor(TodoSearchResponse.class,
                        todo.title,
                        manager.id.countDistinct(),
                        comment.id.countDistinct()
                ))
                .from(todo)
                .leftJoin(manager).on(manager.todo.eq(todo))
                .leftJoin(manager.user, user)
                .leftJoin(comment).on(comment.todo.eq(todo))
                .where(builder)
                .groupBy(todo.id)
                .orderBy(todo.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(todo.id.countDistinct())
                .from(todo)
                .leftJoin(manager).on(manager.todo.eq(todo))
                .leftJoin(manager.user, user)
                .where(builder)
                .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0) ;
    }
}
