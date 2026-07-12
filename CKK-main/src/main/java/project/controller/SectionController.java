package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import project.model.Section;
import project.model.dto.SectionDTO;
import project.service.SectionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SectionController {

    @Autowired
    private SectionService sectionService;

    // ========== API ENDPOINTS ==========

    @ResponseBody
    @GetMapping("/api/projects/{projectId}/sections")
    public List<SectionDTO> getSectionsByProject(@PathVariable Long projectId) {
        System.out.println("Запрос секций для projectId: " + projectId);
        List<SectionDTO> sections = sectionService.getSectionsByProjectId(projectId);
        System.out.println("Найдено секций: " + sections.size());
        return sections;
    }

    @ResponseBody
    @PostMapping("/api/projects/{projectId}/sections")
    public Map<String, Object> createSection(@PathVariable Long projectId, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            if (name != null && !name.trim().isEmpty()) {
                SectionDTO createdSection = sectionService.createSection(projectId, name.trim());
                if (createdSection != null) {
                    response.put("success", true);
                    response.put("message", "Секция создана успешно!");
                    response.put("section", createdSection);
                } else {
                    response.put("success", false);
                    response.put("message", "Проект не найден!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название секции не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при создании секции: " + e.getMessage());
        }
        return response;
    }

    // Новый метод для импорта секций и user stories
    @ResponseBody
    @PostMapping("/api/projects/{projectId}/sections/import")
    public Map<String, Object> importSections(@PathVariable Long projectId, @RequestBody Map<String, Object> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, String>> importData = (List<Map<String, String>>) payload.get("importData");
            if (importData != null && !importData.isEmpty()) {
                Map<String, Object> importResult = sectionService.importSectionsWithUserStories(projectId, importData);
                response.put("success", true);
                response.put("message", "Импорт успешно завершен!");
                response.put("importedSections", importResult.get("importedSections"));
                response.put("importedUserStories", importResult.get("importedUserStories"));
            } else {
                response.put("success", false);
                response.put("message", "Данные для импорта отсутствуют!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при импорте: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @GetMapping("/api/sections/{id}")
    public Map<String, Object> getSection(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Section section = sectionService.getSectionById(id).orElse(null);
            if (section != null) {
                response.put("success", true);
                response.put("section", section);
            } else {
                response.put("success", false);
                response.put("message", "Секция не найдена!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при получении секции: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @PutMapping("/api/sections/{id}")
    public Map<String, Object> updateSection(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();
        try {
            String name = payload.get("name");
            if (name != null && !name.trim().isEmpty()) {
                SectionDTO updatedSection = sectionService.updateSection(id, name.trim());
                if (updatedSection != null) {
                    response.put("success", true);
                    response.put("message", "Секция обновлена успешно!");
                    response.put("section", updatedSection);
                } else {
                    response.put("success", false);
                    response.put("message", "Секция не найдена!");
                }
            } else {
                response.put("success", false);
                response.put("message", "Название секции не может быть пустым!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при обновлении секции: " + e.getMessage());
        }
        return response;
    }

    @GetMapping("/sections/{id}")
    public String showSectionPage(@PathVariable Long id, Model model) {
        Section section = sectionService.getSectionById(id).orElse(null);
        if (section == null) {
            return "redirect:/";
        }
        model.addAttribute("section", section);
        return "section-detail";
    }

    @ResponseBody
    @DeleteMapping("/api/sections/{id}")
    public Map<String, Object> deleteSection(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = sectionService.deleteSection(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Секция удалена успешно!");
            } else {
                response.put("success", false);
                response.put("message", "Секция не найдена!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении секции: " + e.getMessage());
        }
        return response;
    }

    @ResponseBody
    @DeleteMapping("/api/sections/{id}/delete-with-user-stories")
    public Map<String, Object> deleteSectionWithUserStories(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = sectionService.deleteSectionWithUserStories(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "Секция и все связанные user stories успешно удалены!");
            } else {
                response.put("success", false);
                response.put("message", "Секция не найдена!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при удалении секции: " + e.getMessage());
        }
        return response;
    }
}