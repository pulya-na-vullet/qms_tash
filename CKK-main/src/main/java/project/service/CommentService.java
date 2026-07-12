package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.model.Comment;
import project.model.CommentType;
import project.model.TestCase;
import project.model.User;
import project.model.dto.CommentDTO;
import project.repository.CommentRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestCaseService testCaseService;

    @Autowired
    private UserService userService;

    // Создание ручного комментария
    public CommentDTO createManualComment(Long testCaseId, String content, Long userId) {
        TestCase testCase = testCaseService.findById(testCaseId);
        if (testCase == null) {
            throw new RuntimeException("Тест-кейс не найден");
        }

        // *** ИСПРАВЛЕННАЯ СТРОКА ***
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Comment comment = new Comment(content, testCase, user, CommentType.MANUAL);
        Comment savedComment = commentRepository.save(comment);

        return convertToDTO(savedComment);
    }

    // Создание AI комментария
    public CommentDTO createAIComment(Long testCaseId, String content) {
        TestCase testCase = testCaseService.findById(testCaseId);
        if (testCase == null) {
            throw new RuntimeException("Тест-кейс не найден");
        }

        Comment comment = new Comment();
        comment.setContent(content);
        comment.setTestCase(testCase);
        comment.setCommentType(CommentType.AI_GENERATED);
        // user остается null для AI комментариев

        Comment savedComment = commentRepository.save(comment);
        return convertToDTO(savedComment);
    }

    // Получение комментариев для тест-кейса
    public List<CommentDTO> getCommentsByTestCase(Long testCaseId) {
        List<Comment> comments = commentRepository.findByTestCaseIdWithUser(testCaseId);
        return comments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Обновление комментария
    public CommentDTO updateComment(Long commentId, String content, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        // Проверка прав доступа (только автор может редактировать)
        if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Недостаточно прав для редактирования комментария");
        }

        comment.setContent(content);
        Comment updatedComment = commentRepository.save(comment);
        return convertToDTO(updatedComment);
    }

    // Удаление комментария
    public boolean deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        // Проверка прав доступа (только автор может удалять)
        if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Недостаточно прав для удаления комментария");
        }

        commentRepository.delete(comment);
        return true;
    }

    // Конвертация в DTO
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        dto.setTestCaseId(comment.getTestCase().getId());
        dto.setCommentType(comment.getCommentType());

        if (comment.getUser() != null) {
            dto.setUserId(comment.getUser().getId());
            dto.setUserUsername(comment.getUser().getUsername());
            dto.setUserFullName(comment.getUser().getFullName());
        } else {
            // Для AI комментариев
            dto.setUserUsername("AI Assistant");
            dto.setUserFullName("AI Assistant");
        }

        return dto;
    }
}