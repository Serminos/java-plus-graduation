package ru.practicum.category.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.service.CategoryService;
import ru.practicum.dto.category.CategoryDto;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryPublicController {

    private final CategoryService categoryService;

    @GetMapping("/{catId}")
    public CategoryDto getById(@PathVariable Long catId) {
        log.info("GET запрос на получение категории(public) с id {}", catId);
        return categoryService.getById(catId);
    }

    @GetMapping
    public List<CategoryDto> getAllPaged(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("GET запрос на получение всех категорий(public) с параметрами from={}, size={}",
                from, size);
        return categoryService.getAllPaged(from, size);
    }

}

