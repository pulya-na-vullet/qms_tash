package project.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import project.model.Tag;
import project.model.Project;
import project.model.dto.TagDTO;
import project.repository.TagRepository;
import project.repository.ProjectRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TagService {

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ProjectRepository projectRepository;

    public List<TagDTO> getTagsByProjectId(Long projectId) {
        List<Tag> tags = tagRepository.findByProjectIdOrderByCreatedAtAsc(projectId);
        return tags.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public TagDTO getTagById(Long id) {
        Optional<Tag> tagOptional = tagRepository.findById(id);
        return tagOptional.map(this::convertToDTO).orElse(null);
    }

    public TagDTO createTag(Long projectId, String name, String color) {
        Optional<Project> projectOptional = projectRepository.findById(projectId);
        if (projectOptional.isPresent()) {
            // Проверяем, существует ли уже тег с таким именем в проекте
            Optional<Tag> existingTag = tagRepository.findByProjectIdAndName(projectId, name.trim());
            if (existingTag.isPresent()) {
                return convertToDTO(existingTag.get());
            }

            Tag tag = new Tag(name.trim(), projectOptional.get(), color);
            Tag savedTag = tagRepository.save(tag);
            return convertToDTO(savedTag);
        }
        return null;
    }

    public List<TagDTO> searchTags(Long projectId, String searchTerm) {
        List<Tag> tags = tagRepository.findByProjectIdAndNameContainingIgnoreCase(projectId, searchTerm);
        return tags.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // Новый метод для обновления тега
    public TagDTO updateTag(Long id, String name, String color) {
        Optional<Tag> tagOptional = tagRepository.findById(id);
        if (tagOptional.isPresent()) {
            Tag tag = tagOptional.get();
            tag.setName(name.trim());
            if (color != null) {
                tag.setColor(color);
            }
            Tag updatedTag = tagRepository.save(tag);
            return convertToDTO(updatedTag);
        }
        return null;
    }

    // Новый метод для удаления тега
    public boolean deleteTag(Long id) {
        if (tagRepository.existsById(id)) {
            tagRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private TagDTO convertToDTO(Tag tag) {
        return new TagDTO(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getCreatedAt(),
                tag.getProject().getId()
        );
    }
}