# 🌱 Spring Plus

Spring Boot 기반의 일정 관리 백엔드 애플리케이션입니다.  
JWT 인증, JPA, QueryDSL, Spring Security 등 다양한 기술 스택을 활용하여 개발.

---

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.3 |
| ORM | Spring Data JPA, QueryDSL |
| Security | Spring Security, JWT (jjwt 0.11.5) |
| DB | MySQL |
| Build | Gradle |
| 암호화 | BCrypt |

---

## 📁 프로젝트 구조

```
src/main/java/org/example/expert
├── aop/                  # AOP 관련
├── client/               # 외부 API 클라이언트 (날씨)
├── config/               # 설정 (JWT, Filter, Security 등)
├── domain/
│   ├── auth/             # 인증 (회원가입, 로그인)
│   ├── comment/          # 댓글
│   ├── common/           # 공통 (예외, DTO, 어노테이션)
│   ├── manager/          # 담당자
│   ├── todo/             # 일정
│   └── user/             # 유저
```

---

## ✅ 구현 기능

### Level 1

#### 1. @Transactional 이해 - 할 일 저장 오류 수정
- `TodoService`의 `saveTodo()` 메서드에 `@Transactional`이 누락되어 read-only 트랜잭션에서 INSERT가 실패하던 문제를 수정했습니다.
- 클래스 레벨의 `@Transactional(readOnly = true)` 아래에 메서드 레벨로 `@Transactional`을 추가하여 해결했습니다.

#### 2. JWT에 닉네임 추가
- `User` 엔티티에 `nickname` 컬럼을 추가했습니다.
- 회원가입 요청(`SignupRequest`) 및 응답 DTO에 닉네임 필드를 반영했습니다.
- JWT 토큰 생성 시 `nickname` claim을 포함하도록 `JwtUtil`을 수정했습니다.

#### 3. JPQL 기반 조건 검색 추가
- 할 일 목록 조회 시 `weather`와 수정일(`modifiedAt`) 범위를 선택적으로 필터링할 수 있도록 개선했습니다.
- JPQL을 사용하며, 각 조건은 `null`이면 무시됩니다.

```java
// 예시: weather + 기간 조건 검색
GET /todos?weather=Sunny&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
```

#### 4. 컨트롤러 테스트 수정
- `todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다()` 테스트가 실패하는 원인은, `InvalidRequestException` 발생 시 상태코드가 `400 BAD_REQUEST`임에도 테스트에서 `200 OK`를 기대하고 있었기 때문입니다.
- 테스트 코드를 아래와 같이 수정하여 통과시켰습니다.

```java
mockMvc.perform(get("/todos/{todoId}", todoId))
    .andExpect(status().isBadRequest())
    .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
    .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
    .andExpect(jsonPath("$.message").value("Todo not found"));
```

#### 5. AOP 수정
- `AdminAccessLoggingAspect`의 포인트컷이 `UserController.getUser()`를 가리키고 있었으나, 의도는 `UserAdminController.changeUserRole()` 실행 **전** 로깅이었습니다.
- 어드바이스를 `@After` → `@Before`로, 포인트컷을 `changeUserRole()`로 변경했습니다.

```java
@Before("execution(* org.example.expert.domain.user.controller.UserAdminController.changeUserRole(..))")
public void logBeforeChangeUserRole(JoinPoint joinPoint) { ... }
```

---

### Level 2

#### 6. JPA Cascade - 담당자 자동 등록
- `Todo` 생성 시 작성자가 담당자(`Manager`)로 자동 등록되어야 합니다.
- `Todo` 엔티티의 `managers` 컬렉션에 `cascade = CascadeType.PERSIST` 옵션을 추가하여, `Todo` 저장 시 `Manager`도 함께 저장되도록 구현했습니다.

```java
@OneToMany(mappedBy = "todo", cascade = CascadeType.PERSIST)
private List<Manager> managers = new ArrayList<>();
```

#### 7. N+1 문제 해결 - 댓글 조회
- `CommentRepository.findByTodoIdWithUser()`의 JPQL을 `JOIN FETCH`로 수정하여 연관된 `User`를 한 번의 쿼리로 가져오도록 개선했습니다.

```java
@Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.todo.id = :todoId")
List<Comment> findByTodoIdWithUser(@Param("todoId") Long todoId);
```

#### 8. QueryDSL 전환 - Todo 단건 조회
- JPQL로 작성된 `findByIdWithUser()`를 QueryDSL로 전환했습니다.
- `fetchJoin()`을 사용하여 N+1 문제가 발생하지 않도록 처리했습니다.

```java
return queryFactory
    .selectFrom(todo)
    .leftJoin(todo.user, user).fetchJoin()
    .where(todo.id.eq(todoId))
    .fetchOne();
```
---

### Level 3 (도전)

#### 10. QueryDSL + Projection 기반 검색 API
- 새로운 API `GET /todos/search`를 추가했습니다.
- 검색 조건: 제목(부분 일치), 생성일 범위, 담당자 닉네임(부분 일치)
- `Projections.constructor`를 활용하여 필요한 필드(제목, 담당자 수, 댓글 수)만 반환합니다.
- 결과는 페이징 처리되어 반환됩니다.

```java
// 응답 예시
{
  "title": "스프링 공부",
  "managerCount": 3,
  "commentCount": 5
}
```

#### 11. Transaction 심화 - 매니저 등록 로그 ----> 구현 예정
- 매니저 등록 요청 시 `log` 테이블에 요청 로그를 저장합니다.
- `@Transactional(propagation = Propagation.REQUIRES_NEW)`를 사용하여 매니저 등록 트랜잭션과 로그 저장 트랜잭션을 분리했습니다.
- 매니저 등록이 실패하더라도 로그는 반드시 저장됩니다.

---

## 🔑 API 명세

### Auth

| Method | URI | 설명 |
|--------|-----|------|
| POST | `/auth/signup` | 회원가입 |
| POST | `/auth/signin` | 로그인 |

### User

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| GET | `/users/{userId}` | 유저 조회 | 인증 |
| PUT | `/users` | 비밀번호 변경 | 인증 |
| PATCH | `/admin/users/{userId}` | 유저 역할 변경 | ADMIN |

### Todo

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| POST | `/todos` | 일정 생성 | 인증 |
| GET | `/todos` | 일정 목록 조회 (페이징) | 인증 |
| GET | `/todos/{todoId}` | 일정 단건 조회 | 인증 |
| GET | `/todos/search` | 일정 검색 (QueryDSL) | 인증 |

### Comment

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| POST | `/todos/{todoId}/comments` | 댓글 생성 | 인증 |
| GET | `/todos/{todoId}/comments` | 댓글 목록 조회 | 인증 |

### Manager

| Method | URI | 설명 | 권한 |
|--------|-----|------|------|
| POST | `/todos/{todoId}/managers` | 담당자 등록 | 인증 |
| GET | `/todos/{todoId}/managers` | 담당자 목록 조회 | 인증 |
| DELETE | `/todos/{todoId}/managers/{managerId}` | 담당자 삭제 | 인증 |
---

## 📝 코드개선

### @Transactional readOnly와 쓰기 작업
- **문제**: 클래스 레벨에 `@Transactional(readOnly = true)`가 선언된 상태에서 `saveTodo()`에 별도 `@Transactional`이 없어 INSERT 쿼리 실행 시 예외 발생
- **해결**: 쓰기 작업이 필요한 메서드에 `@Transactional`을 명시적으로 추가

### AOP 포인트컷 오류
- **문제**: `@After` 어드바이스가 잘못된 대상 메서드를 가리키고 있어 의도한 동작 불가
- **해결**: 포인트컷 대상과 어드바이스 종류(`@Before`)를 모두 수정

### N+1 문제
- **문제**: 댓글 조회 시 각 댓글마다 유저를 별도 쿼리로 조회하여 N+1 발생
- **해결**: `JOIN FETCH`로 연관 엔티티를 한 번에 조회
