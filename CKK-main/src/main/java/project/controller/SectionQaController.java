package project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import project.service.SectionService;
import project.service.UserStoryService;

@Controller
public class SectionQaController {

    @Autowired
    private SectionService sectionService;

    @Autowired
    private UserStoryService userStoryService;

    // Страница детализации секции для QA
//    @GetMapping("/section-qa/{id}")
//    public String showSectionQaDetailPage(@PathVariable Long id, Model model) {
//        SectionDTO sectionDTO = sectionService.getSectionByIdAsDTO(id);
//        if (sectionDTO == null) {
//            return "redirect:/project-qa";
//        }
//
//        List<UserStoryDTO> userStories = userStoryService.getUserStoriesBySectionId(id);
//
//        model.addAttribute("section", sectionDTO);
//        model.addAttribute("userStories", userStories);
//        return "section-qa-detail";
//    }
}