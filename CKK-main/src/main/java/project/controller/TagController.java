package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import project.model.dto.TagDTO;
import project.service.TagService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TagController {

    @Autowired
    private TagService tagService;

    // ========== API ENDPOINTS ==========

    // Получить все теги проекта
    @ResponseBody
    @GetMapping("/api/projects/{projectId}/tags")
    public List<TagDTO> getTagsByProject(@PathVariable Long projectId) {
        return tagService.getTagsByProjectId(projectId);
    }

    // Создать тег
    @ResponseBody
    @PostMapping("/api/projects/{projectId}/tags")
    public Map<String, Object> createTag(@PathVariable Long projectId, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            String color = payload.get("color");
            if (name != null && !name.trim().isEmpty()) {
                TagDTO createdTag = tagService.createTag(projectId, name.trim(), color != null ? color : "#6c757d");
                if (createdTag != null) {
                    response.put("success", true);
                    response.put("message", "Тег создан успешно!");
                    response.put("tag", createdTag);
                } else {
                    response.put("success", false);
                    response.put("message", "Проект не найден!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название тега не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при создании тега: " + e.getMessage());
        }
        return response;
    }

    // Получить тег по ID
    @ResponseBody
    @GetMapping("/api/tags/{id}")
    public Map<String, Object> getTag(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            TagDTO tag = tagService.getTagById(id);
            if (tag != null) {
                response.put("success", true);
                response.put("tag", tag);
            } else {
                response.put("success", false);
                response.put("message", "Тег не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при получении тега: " + e.getMessage());
        }
        return response;
    }

    // Обновить тег
    @ResponseBody
    @PutMapping("/api/tags/{id}")
    public Map<String, Object> updateTag(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            String color = payload.get("color");
            if (name != null && !name.trim().isEmpty()) {
                TagDTO updatedTag = tagService.updateTag(id, name.trim(), color != null ? color : "#6c757d");
                if (updatedTag != null) {
                    response.put("success", true);
                    response.put("message", "Тег обновлен успешно!");
                    response.put("tag", updatedTag);
                } else {
                    response.put("success", false);
                    response.put("message", "Тег не найден!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название тега не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении тега: " + e.getMessage());
        }
        return response;
    }

    // Удалить тег
    @ResponseBody
    @DeleteMapping("/api/tags/{id}")
    public Map<String, Object> deleteTag(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = tagService.deleteTag(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Тег удален успешно!");
            } else {
                response.put("success", false);
                response.put("message", "Тег не найден!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении тега: " + e.getMessage());
        }
        return response;
    }
}