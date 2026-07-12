package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import project.model.User;
import project.model.dto.CommentDTO;
import project.service.CommentService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    @Autowired
    private CommentService commentService;

    // Создание комментария
    @PostMapping
    public ResponseEntity<?> createComment(@RequestBody Map<String, String> payload,
                                           @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long testCaseId = Long.parseLong(payload.get("testCaseId"));
            String content = payload.get("content");

            CommentDTO comment = commentService.createManualComment(testCaseId, content, currentUser.getId());

            response.put("success", true);
            response.put("message", "Комментарий добавлен");
            response.put("comment", comment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при добавлении комментария: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Обновление комментария
    @PutMapping("/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable Long commentId,
                                           @RequestBody Map<String, String> payload,
                                           @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            String content = payload.get("content");

            CommentDTO updatedComment = commentService.updateComment(commentId, content, currentUser.getId());

            response.put("success", true);
            response.put("message", "Комментарий обновлен");
            response.put("comment", updatedComment);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при обновлении комментария: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Удаление комментария
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId,
                                           @AuthenticationPrincipal User currentUser) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = commentService.deleteComment(commentId, currentUser.getId());

            response.put("success", true);
            response.put("message", "Комментарий удален");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при удалении комментария: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // *** ДОПОЛНИТЕЛЬНЫЕ МЕТОДЫ ДЛЯ УДОБСТВА ***

    // Получить комментарии для тест-кейса
    @GetMapping("/test-case/{testCaseId}")
    public ResponseEntity<?> getCommentsByTestCase(@PathVariable Long testCaseId) {
        Map<String, Object> response = new HashMap<>();
        try {
            var comments = commentService.getCommentsByTestCase(testCaseId);
            response.put("success", true);
            response.put("comments", comments);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при получении комментариев: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Создание AI комментария (для администраторов)
    @PostMapping("/ai/{testCaseId}")
    public ResponseEntity<?> createAIComment(@PathVariable Long testCaseId,
                                             @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String content = payload.get("content");
            if (content != null && !content.trim().isEmpty()) {
                CommentDTO comment = commentService.createAIComment(testCaseId, content.trim());
                response.put("success", true);
                response.put("message", "AI комментарий добавлен");
                response.put("comment", comment);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Содержание комментария не может быть пустым");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Ошибка при создании AI комментария: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}