package ru.practicum.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.validate.NotOnlySpaces;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CategoryUpdateDto {

    @NotBlank(message = "Название категории не может быть пустым")
    @Size(min = 1, max = 50, message = "Категория должна быть от 1 до 50 символов")
    @NotOnlySpaces
    private String name;
}
