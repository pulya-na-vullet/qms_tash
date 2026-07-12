package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import project.model.dto.UserStoryDTO;
import project.service.UserStoryService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UserStoryController {

    @Autowired
    private UserStoryService userStoryService;

    // ========== API ENDPOINTS ==========

    @ResponseBody
    @GetMapping("/api/sections/{sectionId}/user-stories")
    public List<UserStoryDTO> getUserStoriesBySection(@PathVariable Long sectionId) {
        return userStoryService.getUserStoriesBySectionId(sectionId);
    }

    @ResponseBody
    @PostMapping("/api/sections/{sectionId}/user-stories")
    public Map<String, Object> createUserStory(@PathVariable Long sectionId, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            if (name != null && !name.trim().isEmpty()) {
                UserStoryDTO createdUserStory = userStoryService.createUserStory(sectionId, name.trim());
                if (createdUserStory != null) {
                    response.put("success", true);
                    response.put("message", "User story создана успешно!");
                    response.put("userStory", createdUserStory);
                } else {
                    response.put("success", false);
                    response.put("message", "Секция не найдена!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название user story не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при создании user story: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @PostMapping("/api/sections/{sectionId}/user-stories/import")
    public Map<String, Object> importUserStories(@PathVariable Long sectionId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<String> userStories = (List<String>) payload.get("userStories");
            if (userStories != null && !userStories.isEmpty()) {
                int importedCount = userStoryService.importUserStories(sectionId, userStories);
                response.put("success", true);
                response.put("message", "Успешно импортировано " + importedCount + " user stories");
                response.put("importedCount", importedCount);
            } else {
                response.put("success", false);
                response.put("message", "Список user stories пуст!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при импорте user stories: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @GetMapping("/api/user-stories/{id}")
    public Map<String, Object> getUserStory(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            UserStoryDTO userStory = userStoryService.getUserStoryById(id).orElse(null);
            if (userStory != null) {
                response.put("success", true);
                response.put("userStory", userStory);
            } else {
                response.put("success", false);
                response.put("message", "User story не найдена!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при получении user story: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @PutMapping("/api/user-stories/{id}")
    public Map<String, Object> updateUserStory(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            if (name != null && !name.trim().isEmpty()) {
                UserStoryDTO updatedUserStory = userStoryService.updateUserStory(id, name.trim());
                if (updatedUserStory != null) {
                    response.put("success", true);
                    response.put("message", "User story обновлена успешно!");
                    response.put("userStory", updatedUserStory);
                } else {
                    response.put("success", false);
                    response.put("message", "User story не найдена!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название user story не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении user story: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @DeleteMapping("/api/user-stories/{id}")
    public Map<String, Object> deleteUserStory(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = userStoryService.deleteUserStory(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "User story удалена успешно!");
            } else {
                response.put("success", false);
                response.put("message", "User story не найдена!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении user story: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @GetMapping("/api/projects/{projectId}/user-stories")
    public List<UserStoryDTO> getUserStoriesByProject(@PathVariable Long projectId) {
        return userStoryService.getUserStoriesByProjectId(projectId);
    }
}